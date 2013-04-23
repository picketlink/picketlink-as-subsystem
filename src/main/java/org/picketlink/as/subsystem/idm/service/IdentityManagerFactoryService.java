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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.metamodel.EntityType;

import org.hibernate.cfg.AvailableSettings;
import org.jboss.as.jpa.hibernate4.JBossAppServerJtaPlatform;
import org.jboss.as.jpa.transaction.JtaManagerImpl;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.picketlink.common.util.StringUtil;
import org.picketlink.idm.IdentityManagerFactory;
import org.picketlink.idm.config.IdentityConfiguration;
import org.picketlink.idm.config.IdentityStoreConfiguration;
import org.picketlink.idm.config.JPAIdentityStoreConfiguration;
import org.picketlink.idm.jpa.annotations.IDMEntity;
import org.picketlink.idm.jpa.internal.JPAContextInitializer;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class IdentityManagerFactoryService implements Service<IdentityManagerFactory> {

    private static String SERVICE_NAME_PREFIX = "IdentityManagerFactoryService";

    private boolean embeddedEmf;
    private EntityManagerFactory emf;
    private String jndiName;
    private final String alias;
    private IdentityManagerFactory identityManagerFactory;
    private String jpaStoreEntityManagerFactory;
    private String jpaStoreDataSource;
    private String jpaStoreEntityModule;
    private String jpaStoreEntityModuleUnitName = "identity";
    private ModuleClassLoader entityClassLoader;
    private IdentityConfiguration identityConfiguration;

    public IdentityManagerFactoryService(String alias, String jndiName, IdentityConfiguration configuration) {
        this.alias = alias;
        this.jndiName = jndiName;
        this.identityConfiguration = configuration;
    }

    public IdentityManagerFactoryService(String alias, String jndiName, String dataSourceJndiName, String entityModuleName,
            String entityModuleUnitName, String emfJndiName, IdentityConfiguration configuration) {
        this(alias, jndiName, configuration);
        this.jpaStoreDataSource = dataSourceJndiName;
        this.jpaStoreEntityManagerFactory = emfJndiName;
        this.jpaStoreEntityModule = entityModuleName;
        this.jpaStoreEntityModuleUnitName = entityModuleUnitName;
    }

    @Override
    public IdentityManagerFactory getValue() throws IllegalStateException, IllegalArgumentException {
        return this.identityManagerFactory;
    }

    @Override
    public void start(StartContext context) throws StartException {
        configureJPAIdentityStore();
        this.identityManagerFactory = this.identityConfiguration.buildIdentityManagerFactory();
    }

    @Override
    public void stop(StopContext context) {
        ROOT_LOGGER.info("Stopping Identity Service");

        if (embeddedEmf && emf != null) {
            ROOT_LOGGER.debug("Closing entity manager factory");
            emf.close();
        }

        emf = null;

        this.identityManagerFactory = null;
    }
    
    public String getJndiName() {
        return this.jndiName;
    }

    public IdentityConfiguration getIdentityConfiguration() {
        return this.identityConfiguration;
    }

    public String getAlias() {
        return this.alias;
    }
    
    public Set<String> getConfiguredRealms() {
        HashSet<String> hashSet = new HashSet<String>();
        
        List<IdentityStoreConfiguration> configuredStores = this.identityConfiguration.getConfiguredStores();
        
        for (IdentityStoreConfiguration identityStoreConfiguration : configuredStores) {
            hashSet.addAll(identityStoreConfiguration.getRealms());
        }
        
        return hashSet;
    }

    public static ServiceName createServiceName(String alias) {
        return ServiceName.JBOSS.append(SERVICE_NAME_PREFIX, alias);
    }

    private void configureJPAIdentityStore() {
        JPAIdentityStoreConfiguration jpaConfig = null;
        
        for (IdentityStoreConfiguration identityStoreConfiguration : this.identityConfiguration.getConfiguredStores()) {
            if (JPAIdentityStoreConfiguration.class.isInstance(identityStoreConfiguration)) {
                jpaConfig = (JPAIdentityStoreConfiguration) identityStoreConfiguration;
                break;
            }
        }
        
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

            this.emf = Persistence.createEntityManagerFactory(this.jpaStoreEntityModuleUnitName, properties);
        } finally {
            if (entityClassLoader != null) {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
            }
        }
    }
    
}