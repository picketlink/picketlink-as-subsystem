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

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

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
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.ImmediateValue;
import org.picketlink.as.subsystem.model.ModelElement;
import org.picketlink.common.util.StringUtil;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.IdentityManagerFactory;
import org.picketlink.idm.config.FeatureSet;
import org.picketlink.idm.config.FeatureSet.FeatureGroup;
import org.picketlink.idm.config.FeatureSet.FeatureOperation;
import org.picketlink.idm.config.IdentityConfiguration;
import org.picketlink.idm.config.IdentityStoreConfiguration;
import org.picketlink.idm.config.JPAIdentityStoreConfiguration;
import org.picketlink.idm.jpa.internal.JPAContextInitializer;
import org.picketlink.idm.jpa.schema.CredentialObject;
import org.picketlink.idm.jpa.schema.CredentialObjectAttribute;
import org.picketlink.idm.jpa.schema.IdentityObject;
import org.picketlink.idm.jpa.schema.IdentityObjectAttribute;
import org.picketlink.idm.jpa.schema.PartitionObject;
import org.picketlink.idm.jpa.schema.RelationshipIdentityObject;
import org.picketlink.idm.jpa.schema.RelationshipObject;
import org.picketlink.idm.jpa.schema.RelationshipObjectAttribute;
import org.picketlink.idm.model.Relationship;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class IdentityManagerService implements Service<IdentityManager> {

    private static String SERVICE_NAME_PREFIX = "IdentityManagementService";

    private EntityManagerFactory emf;
    private IdentityConfiguration identityConfiguration = new IdentityConfiguration();
    private String jndiUrl;
    private Map<ModelElement, IdentityStoreConfiguration> storeConfigs = new HashMap<ModelElement, IdentityStoreConfiguration>();

    private String jpaStoreDataSource;

    private String jpaStoreEntityManagerFactory;

    private String alias;

    public IdentityManagerService(ModelNode modelNode) {
        this.alias = modelNode.get(ModelElement.COMMON_ALIAS.getName()).asString();
        this.jndiUrl = modelNode.get(ModelElement.IDENTITY_MANAGEMENT_JNDI_URL.getName()).asString();
    }

    @Override
    public IdentityManager getValue() throws IllegalStateException, IllegalArgumentException {
        return createIdentityManager();
    }

    @Override
    public void start(StartContext context) throws StartException {
        ROOT_LOGGER.info("Starting Identity Service");
        ROOT_LOGGER.info("Creating entity manager factory");

        if (!StringUtil.isNullOrEmpty(this.jpaStoreEntityManagerFactory)) {
            try {
                this.emf = (EntityManagerFactory) new InitialContext().lookup(this.jpaStoreEntityManagerFactory);
            } catch (NamingException e) {
                throw new RuntimeException(e);
            }
        } else {
            Map<Object, Object> properties = new HashMap<Object, Object>();

            properties.put("javax.persistence.jtaDataSource", this.jpaStoreDataSource);

            properties.put(AvailableSettings.JTA_PLATFORM, new JBossAppServerJtaPlatform(JtaManagerImpl.getInstance()));

            this.emf = Persistence.createEntityManagerFactory("identity", properties);
        }

        final BinderService binderService = new BinderService("IdentityService-" + this.alias);
        final ServiceBuilder<ManagedReferenceFactory> builder = context.getController().getServiceContainer()
                .addService(ContextNames.buildServiceName(ContextNames.JAVA_CONTEXT_SERVICE_NAME, this.jndiUrl), binderService);

        builder.addDependency(ContextNames.JAVA_CONTEXT_SERVICE_NAME, ServiceBasedNamingStore.class,
                binderService.getNamingStoreInjector());

        builder.addDependency(createServiceName(this.alias), IdentityManager.class, new Injector<IdentityManager>() {
            @Override
            public void inject(final IdentityManager value) throws InjectionException {
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

    private IdentityManager createIdentityManager() {
        Collection<IdentityStoreConfiguration> storeConfigs = this.storeConfigs.values();
        
        for (IdentityStoreConfiguration identityStoreConfiguration : storeConfigs) {
            this.identityConfiguration.addConfig(identityStoreConfiguration);
        }
        
        IdentityManagerFactory entityManager = this.identityConfiguration.buildIdentityManagerFactory();
        
        return entityManager.createIdentityManager();
    }

    private void configureJPAIdentityStore(ModelNode modelNode) {
        JPAIdentityStoreConfiguration jpaConfig = new JPAIdentityStoreConfiguration();

        ModelNode jpaDataSourceNode = modelNode.get(ModelElement.JPA_STORE_DATASOURCE.getName());

        if (jpaDataSourceNode.isDefined()) {
            this.jpaStoreDataSource = jpaDataSourceNode.asString();
        }

        ModelNode jpaEntityManagerFactoryNode = modelNode.get(ModelElement.JPA_STORE_ENTITY_MANAGER_FACTORY.getName());

        if (jpaEntityManagerFactoryNode.isDefined()) {
            this.jpaStoreEntityManagerFactory = jpaEntityManagerFactoryNode.asString();
        }

        jpaConfig.setIdentityClass(IdentityObject.class);
        jpaConfig.setCredentialClass(CredentialObject.class);
        jpaConfig.setCredentialAttributeClass(CredentialObjectAttribute.class);
        jpaConfig.setAttributeClass(IdentityObjectAttribute.class);
        jpaConfig.setRelationshipClass(RelationshipObject.class);
        jpaConfig.setRelationshipIdentityClass(RelationshipIdentityObject.class);
        jpaConfig.setRelationshipAttributeClass(RelationshipObjectAttribute.class);
        jpaConfig.setPartitionClass(PartitionObject.class);

        jpaConfig.addContextInitializer(new JPAContextInitializer(this.emf) {
            @Override
            public EntityManager getEntityManager() {
                EntityManager em = (EntityManager) Proxy.newProxyInstance(getClass().getClassLoader(),
                        new Class<?>[] { EntityManager.class }, new EntityManagerTx(emf.createEntityManager()));

                return em;
            }
        });
        
        this.storeConfigs.put(ModelElement.JPA_STORE, jpaConfig);
    }

    @Override
    public void stop(StopContext context) {
        ROOT_LOGGER.info("Stopping Identity Service");

        ROOT_LOGGER.info("Closing entity manager factory");
        emf.close();
    }

    public IdentityConfiguration getIdentityConfiguration() {
        return this.identityConfiguration;
    }

    public void configureStore(ModelNode operation) {
        String storeType = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getLastElement()
                .getValue();

        if (storeType.equals(ModelElement.JPA_STORE.getName())) {
            configureJPAIdentityStore(operation);
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

    public static ServiceName createServiceName(String alias) {
        return ServiceName.JBOSS.append(SERVICE_NAME_PREFIX, alias);
    }
}