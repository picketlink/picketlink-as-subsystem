/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.picketlink.as.subsystem.idm.service;

import static org.picketlink.as.subsystem.PicketLinkLogger.ROOT_LOGGER;

import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.metamodel.EntityType;

import org.hibernate.cfg.AvailableSettings;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.jpa.hibernate4.JBossAppServerJtaPlatform;
import org.jboss.as.jpa.transaction.JtaManagerImpl;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.ImmediateValue;
import org.picketlink.as.subsystem.model.ModelElement;
import org.picketlink.common.util.StringUtil;
import org.picketlink.idm.IdentityManagerFactory;
import org.picketlink.idm.config.BaseAbstractStoreConfiguration;
import org.picketlink.idm.config.FeatureSet;
import org.picketlink.idm.config.FeatureSet.FeatureGroup;
import org.picketlink.idm.config.FeatureSet.FeatureOperation;
import org.picketlink.idm.config.FileIdentityStoreConfiguration;
import org.picketlink.idm.config.IdentityConfiguration;
import org.picketlink.idm.config.IdentityStoreConfiguration;
import org.picketlink.idm.config.JPAIdentityStoreConfiguration;
import org.picketlink.idm.config.LDAPIdentityStoreConfiguration;
import org.picketlink.idm.jpa.annotations.IDMEntity;
import org.picketlink.idm.jpa.internal.JPAContextInitializer;
import org.picketlink.idm.model.Relationship;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class IdentityManagerFactoryService implements Service<IdentityManagerFactory> {

    private static String SERVICE_NAME_PREFIX = "IdentityManagementService";

    private boolean embeddedEmf;

    private EntityManagerFactory emf;

    private final String jndiName;
    private final Map<ModelElement, IdentityStoreConfiguration> storeConfigs = new HashMap<ModelElement, IdentityStoreConfiguration>();

    private final String alias;

    private IdentityManagerFactory identityManagerFactory;

    private String jpaStoreEntityManagerFactory;

    private String jpaStoreDataSource;

    private String jpaStoreEntityModule;

    private ModuleClassLoader entityClassLoader;

    public IdentityManagerFactoryService(ModelNode modelNode) {
        this.alias = modelNode.get(ModelElement.COMMON_ALIAS.getName()).asString();
        this.jndiName = modelNode.get(ModelElement.IDENTITY_MANAGEMENT_JNDI_NAME.getName()).asString();
    }

    @Override
    public IdentityManagerFactory getValue() throws IllegalStateException, IllegalArgumentException {
        return this.identityManagerFactory;
    }

    @Override
    public void start(StartContext context) throws StartException {
        ROOT_LOGGER.info("Starting Identity Service");

        startJPAIdentityStore();

        this.identityManagerFactory = createIdentityManagerFactory();

        publishIdentityManagerFactory(context);
    }

    private void createEntityManagerFactory() throws ModuleLoadException {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

        if (!StringUtil.isNullOrEmpty(jpaStoreEntityModule)) {
            ModuleLoader moduleLoader = Module.getContextModuleLoader();
            Module module = moduleLoader.loadModule(ModuleIdentifier.create(jpaStoreEntityModule));
            entityClassLoader = module.getClassLoader();
        }

        try {
            Map<Object, Object> properties = new HashMap<Object, Object>();
            properties.put("javax.persistence.jtaDataSource", jpaStoreDataSource);
            properties.put(AvailableSettings.JTA_PLATFORM, new JBossAppServerJtaPlatform(JtaManagerImpl.getInstance()));

            if (entityClassLoader != null) {
                Thread.currentThread().setContextClassLoader(entityClassLoader);
            }

            this.emf = Persistence.createEntityManagerFactory("identity", properties);
        } finally {
            if (entityClassLoader != null) {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
            }
        }
    }

    @Override
    public void stop(StopContext context) {
        ROOT_LOGGER.info("Stopping Identity Service");

        unpublishIdentityManagerFactory(context);

        if (embeddedEmf && emf != null) {
            ROOT_LOGGER.debug("Closing entity manager factory");
            emf.close();
        }

        emf = null;

        this.identityManagerFactory = null;
    }

    public void configureStore(ModelNode operation) {
        String storeType = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getLastElement()
                .getValue();

        if (storeType.equals(ModelElement.JPA_STORE.getName())) {
            configureJPAIdentityStore(operation);
        } else if (storeType.equals(ModelElement.FILE_STORE.getName())) {
            configureFileIdentityStore(operation);
        } else if (storeType.equals(ModelElement.LDAP_STORE.getName())) {
            configureLDAPIdentityStore(operation);
        }
        
        configureRealms(operation, storeType);
        configureTiers(operation, storeType);
    }

    private void configureRealms(ModelNode operation, String storeType) {
        ModelNode realmsNode = operation.get(ModelElement.REALMS.getName());

        if (realmsNode.isDefined()) {
            BaseAbstractStoreConfiguration<?> identityStoreConfiguration = (BaseAbstractStoreConfiguration<?>) this.storeConfigs.get(ModelElement.forName(storeType));
            
            String[] realms = realmsNode.asString().split(",");

            for (String realm : realms) {
                identityStoreConfiguration.addRealm(realm);
            }
        }
    }

    private void configureTiers(ModelNode operation, String storeType) {
        ModelNode tierNode = operation.get(ModelElement.TIERS.getName());

        if (tierNode.isDefined()) {
            BaseAbstractStoreConfiguration<?> identityStoreConfiguration = (BaseAbstractStoreConfiguration<?>) this.storeConfigs.get(ModelElement.forName(storeType));
            
            String[] tiers = tierNode.asString().split(",");

            for (String tier : tiers) {
                identityStoreConfiguration.addTier(tier);
            }
        }
    }

    public void configureFeatures(ModelNode operation) {
        String storeType = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getElement(2).getValue();
        String featureGroup = operation.get(ModelElement.FEATURE_GROUP.getName()).asString();
        String featureOperations = operation.get(ModelElement.FEATURE_OPERATION.getName()).asString();

        IdentityStoreConfiguration storeConfig = this.storeConfigs.get(ModelElement.forName(storeType));

        for (String featureOperation : featureOperations.split(",")) {
            storeConfig.getFeatureSet().addFeature(FeatureGroup.valueOf(featureGroup),
                    FeatureOperation.valueOf(featureOperation));
        }
    }

    @SuppressWarnings("unchecked")
    public void configureRelationships(ModelNode operation) {
        String storeType = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getElement(2).getValue();
        String relationshipClass = operation.get(ModelElement.COMMON_CLASS.getName()).asString();

        IdentityStoreConfiguration storeConfig = this.storeConfigs.get(ModelElement.forName(storeType));

        try {
            FeatureSet.addRelationshipSupport(storeConfig.getFeatureSet(),
                    (Class<? extends Relationship>) Class.forName(relationshipClass));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void configureAllFeatures(ModelNode operation) {
        String storeType = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getElement(2).getValue();

        BaseAbstractStoreConfiguration<?> storeConfig = (BaseAbstractStoreConfiguration<?>) this.storeConfigs.get(ModelElement
                .forName(storeType));

        storeConfig.supportAllFeatures();
    }

    public static ServiceName createServiceName(String alias) {
        return ServiceName.JBOSS.append(SERVICE_NAME_PREFIX, alias);
    }

    private IdentityManagerFactory createIdentityManagerFactory() {
        Collection<IdentityStoreConfiguration> storeConfigs = this.storeConfigs.values();

        IdentityConfiguration configuration = new IdentityConfiguration();

        for (IdentityStoreConfiguration identityStoreConfiguration : storeConfigs) {
            configuration.addConfig(identityStoreConfiguration);
        }

        return configuration.buildIdentityManagerFactory();
    }

    private void configureFileIdentityStore(ModelNode modelNode) {
        FileIdentityStoreConfiguration storeConfig = new FileIdentityStoreConfiguration();

        ModelNode workingDir = modelNode.get(ModelElement.FILE_STORE_WORKING_DIR.getName());
        ModelNode alwaysCreateFiles = modelNode.get(ModelElement.FILE_STORE_ALWAYS_CREATE_FILE.getName());
        ModelNode asyncWrite = modelNode.get(ModelElement.FILE_STORE_ASYNC_WRITE.getName());
        ModelNode asyncWriteThreadPool = modelNode.get(ModelElement.FILE_STORE_ASYNC_THREAD_POOL.getName());

        if (workingDir.isDefined()) {
            storeConfig.setWorkingDir(workingDir.asString());
        }

        if (alwaysCreateFiles.isDefined()) {
            storeConfig.setAlwaysCreateFiles(alwaysCreateFiles.asBoolean());
        }

        if (asyncWrite.isDefined()) {
            storeConfig.setAsyncWrite(asyncWrite.asBoolean());
        }

        if (asyncWriteThreadPool.isDefined()) {
            storeConfig.setAsyncThreadPool(asyncWriteThreadPool.asInt());
        }

        this.storeConfigs.put(ModelElement.FILE_STORE, storeConfig);
    }

    private void configureLDAPIdentityStore(ModelNode modelNode) {
        LDAPIdentityStoreConfiguration storeConfig = new LDAPIdentityStoreConfiguration();

        ModelNode url = modelNode.get(ModelElement.LDAP_STORE_URL.getName());
        ModelNode bindDn = modelNode.get(ModelElement.LDAP_STORE_BIND_DN.getName());
        ModelNode bindCredential = modelNode.get(ModelElement.LDAP_STORE_BIND_CREDENTIAL.getName());
        ModelNode baseDn = modelNode.get(ModelElement.LDAP_STORE_BASE_DN_SUFFIX.getName());
        ModelNode userDn = modelNode.get(ModelElement.LDAP_STORE_USER_DN_SUFFIX.getName());
        ModelNode agentDn = modelNode.get(ModelElement.LDAP_STORE_AGENT_DN_SUFFIX.getName());
        ModelNode groupDn = modelNode.get(ModelElement.LDAP_STORE_GROUP_DN_SUFFIX.getName());
        ModelNode roleDn = modelNode.get(ModelElement.LDAP_STORE_ROLE_DN_SUFFIX.getName());

        if (url.isDefined()) {
            storeConfig.setLdapURL(url.asString());
        }

        if (bindDn.isDefined()) {
            storeConfig.setBindDN(bindDn.asString());
        }

        if (bindCredential.isDefined()) {
            storeConfig.setBindCredential(bindCredential.asString());
        }

        if (baseDn.isDefined()) {
            storeConfig.setBaseDN(baseDn.asString());
        }

        if (userDn.isDefined()) {
            storeConfig.setUserDNSuffix(userDn.asString());
        }

        if (agentDn.isDefined()) {
            storeConfig.setAgentDNSuffix(agentDn.asString());
        }

        if (roleDn.isDefined()) {
            storeConfig.setRoleDNSuffix(roleDn.asString());
        }

        if (groupDn.isDefined()) {
            storeConfig.setGroupDNSuffix(groupDn.asString());
        }

        this.storeConfigs.put(ModelElement.LDAP_STORE, storeConfig);
    }

    private void startJPAIdentityStore() {
        JPAIdentityStoreConfiguration jpaConfig = (JPAIdentityStoreConfiguration) this.storeConfigs.get(ModelElement.JPA_STORE);
        if (jpaConfig == null) {
            return;
        }

        if (!StringUtil.isNullOrEmpty(jpaStoreEntityManagerFactory)) {
            try {
                ROOT_LOGGER.debug("Looking up entity manager factory: " + jpaStoreEntityManagerFactory);

                this.emf = (EntityManagerFactory) new InitialContext().lookup(jpaStoreEntityManagerFactory);
                embeddedEmf = false;
            } catch (NamingException e) {
                throw new RuntimeException(e);
            }
        } else if (!StringUtil.isNullOrEmpty(jpaStoreDataSource)) {
            try {
                ROOT_LOGGER.debug("Creating entity manager factory for module: " + "myschema");

                createEntityManagerFactory();
                embeddedEmf = true;
            } catch (ModuleLoadException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("No entity manager factory configured");
        }

        Set<EntityType<?>> entities = emf.getMetamodel().getEntities();
        for (EntityType<?> entity : entities) {
            Class<?> javaType = entity.getJavaType();
            IDMEntity idmEntity = javaType.getAnnotation(IDMEntity.class);
            if (idmEntity != null) {
                switch (idmEntity.value()) {
                    case CREDENTIAL_ATTRIBUTE:
                        jpaConfig.setCredentialAttributeClass(javaType);
                        break;
                    case IDENTITY_ATTRIBUTE:
                        jpaConfig.setAttributeClass(javaType);
                        break;
                    case IDENTITY_CREDENTIAL:
                        jpaConfig.setCredentialClass(javaType);
                        break;
                    case IDENTITY_TYPE:
                        jpaConfig.setIdentityClass(javaType);
                        break;
                    case PARTITION:
                        jpaConfig.setPartitionClass(javaType);
                        break;
                    case RELATIONSHIP:
                        jpaConfig.setRelationshipClass(javaType);
                        break;
                    case RELATIONSHIP_ATTRIBUTE:
                        jpaConfig.setRelationshipAttributeClass(javaType);
                        break;
                    case RELATIONSHIP_IDENTITY:
                        jpaConfig.setRelationshipIdentityClass(javaType);
                        break;
                }
            }
        }

        jpaConfig.addContextInitializer(new JPAContextInitializer(emf) {
            @Override
            public EntityManager getEntityManager() {
                return (EntityManager) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                        new Class<?>[] { EntityManager.class }, new EntityManagerTx(emf.createEntityManager(),
                                entityClassLoader));
            }
        });

    }

    private void configureJPAIdentityStore(ModelNode modelNode) {
        ModelNode jpaDataSourceNode = modelNode.get(ModelElement.JPA_STORE_DATASOURCE.getName());
        ModelNode jpaEntityModule = modelNode.get(ModelElement.JPA_STORE_ENTITY_MODULE.getName());

        ModelNode jpaEntityManagerFactoryNode = modelNode.get(ModelElement.JPA_STORE_ENTITY_MANAGER_FACTORY.getName());

        if (jpaEntityManagerFactoryNode.isDefined()) {
            jpaStoreEntityManagerFactory = jpaEntityManagerFactoryNode.asString();
            embeddedEmf = false;
        } else if (jpaDataSourceNode.isDefined()) {
            jpaStoreDataSource = jpaDataSourceNode.asString();
            embeddedEmf = true;
        } else {
            throw new RuntimeException("No entity manager factory configured");
        }

        if (jpaEntityModule.isDefined()) {
            jpaStoreEntityModule = jpaEntityModule.asString();
        }

        JPAIdentityStoreConfiguration jpaConfig = new JPAIdentityStoreConfiguration();
        this.storeConfigs.put(ModelElement.JPA_STORE, jpaConfig);
    }

    private void publishIdentityManagerFactory(StartContext context) {
        final BinderService binderService = new BinderService("IdentityService-" + this.alias);
        final ServiceBuilder<ManagedReferenceFactory> builder = context.getController().getServiceContainer()
                .addService(ContextNames.buildServiceName(ContextNames.JAVA_CONTEXT_SERVICE_NAME, this.jndiName), binderService);

        builder.addDependency(ContextNames.JAVA_CONTEXT_SERVICE_NAME, ServiceBasedNamingStore.class,
                binderService.getNamingStoreInjector());

        builder.addDependency(createServiceName(this.alias), IdentityManagerFactory.class,
                new Injector<IdentityManagerFactory>() {
                    @Override
                    public void inject(final IdentityManagerFactory value) throws InjectionException {
                        binderService.getManagedObjectInjector().inject(
                                new ValueManagedReferenceFactory(new ImmediateValue<Object>(value)));
                    }

                    @Override
                    public void uninject() {
                        binderService.getManagedObjectInjector().uninject();
                    }
                });

        builder.setInitialMode(Mode.ACTIVE).install();
    }

    private void unpublishIdentityManagerFactory(StopContext context) {
        ServiceController<?> service = context.getController().getServiceContainer()
                .getService(ContextNames.buildServiceName(ContextNames.JAVA_CONTEXT_SERVICE_NAME, this.jndiName));

        service.setMode(Mode.REMOVE);
    }
    
    public String getJndiName() {
        return this.jndiName;
    }
}