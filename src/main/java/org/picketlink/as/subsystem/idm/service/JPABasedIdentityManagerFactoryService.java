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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;
import javax.persistence.metamodel.EntityType;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.hibernate.cfg.AvailableSettings;
import org.jboss.as.jpa.hibernate4.JBossAppServerJtaPlatform;
import org.jboss.as.jpa.transaction.JtaManagerImpl;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.picketlink.common.util.StringUtil;
import org.picketlink.idm.config.IdentityConfiguration;
import org.picketlink.idm.config.IdentityStoreConfiguration;
import org.picketlink.idm.config.JPAIdentityStoreConfiguration;
import org.picketlink.idm.jpa.annotations.IDMEntity;
import org.picketlink.idm.jpa.internal.JPAContextInitializer;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JPABasedIdentityManagerFactoryService extends IdentityManagerFactoryService {

    private final InjectedValue<ValueManagedReferenceFactory> providedEntityManagerFactory = new InjectedValue<ValueManagedReferenceFactory>();
    private EntityManagerFactory emf;
    private final String jpaStoreDataSource;
    private String jpaStoreEntityModuleUnitName = "identity";
    private Module entitiesModule;

    public JPABasedIdentityManagerFactoryService(String alias, String jndiName, String dataSourceJndiName,
            String entityModuleName, String entityModuleUnitName, IdentityConfiguration configuration) {
        super(alias, jndiName, configuration);
        this.jpaStoreDataSource = dataSourceJndiName;
        this.jpaStoreEntityModuleUnitName = entityModuleUnitName;

        if (entityModuleName == null) {
            entityModuleName = "org.picketlink.as.extension";
        }

        ModuleLoader moduleLoader = Module.getContextModuleLoader();
        try {
            this.entitiesModule = moduleLoader.loadModule(ModuleIdentifier.create(entityModuleName));
        } catch (ModuleLoadException e) {
            throw new IllegalStateException("Entities module not found [" + entityModuleName
                    + "]. Unable to start IDM service for [" + alias + "].");
        }
    }

    @Override
    public void start(StartContext context) throws StartException {
        configureJPAIdentityStore();
        super.start(context);
    }

    @Override
    public void stop(StopContext context) {
        closeEmbeddedEntityManagerFactory();
        super.stop(context);
    }

    private void closeEmbeddedEntityManagerFactory() {
        if (this.emf != null && this.providedEntityManagerFactory.getOptionalValue() == null) {
            ROOT_LOGGER.debugf("Closing embedded entity manager factory for %s", getAlias());
            this.emf.close();
        }

        this.emf = null;
    }

    private void configureJPAIdentityStore() {
        JPAIdentityStoreConfiguration jpaConfig = getJPAIdentityStoreConfiguration();

        if (!hasEmbeddedEntityManagerFactory()) {
            this.emf = (EntityManagerFactory) this.providedEntityManagerFactory.getValue().getReference().getInstance();
        } else if (!StringUtil.isNullOrEmpty(jpaStoreDataSource)) {
            try {
                ROOT_LOGGER.debug("Creating entity manager factory for module: " + "myschema");
                createEmbeddedEntityManagerFactory();
            } catch (ModuleLoadException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("No entity manager factory configured");
        }

        configureIDMEntities(jpaConfig);

        jpaConfig.addContextInitializer(new JPAContextInitializer(emf) {
            @Override
            public EntityManager getEntityManager() {
                return (EntityManager) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                        new Class<?>[] { EntityManager.class }, new EntityManagerTx(emf.createEntityManager(), entitiesModule));
            }
        });
    }

    private JPAIdentityStoreConfiguration getJPAIdentityStoreConfiguration() {
        JPAIdentityStoreConfiguration jpaConfig = null;

        for (IdentityStoreConfiguration identityStoreConfiguration : getIdentityConfiguration().getConfiguredStores()) {
            if (JPAIdentityStoreConfiguration.class.isInstance(identityStoreConfiguration)) {
                jpaConfig = (JPAIdentityStoreConfiguration) identityStoreConfiguration;
                break;
            }
        }
        return jpaConfig;
    }

    private void configureIDMEntities(JPAIdentityStoreConfiguration jpaConfig) {
        Set<EntityType<?>> entities = this.emf.getMetamodel().getEntities();

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
    }

    private boolean hasEmbeddedEntityManagerFactory() {
        return this.providedEntityManagerFactory.getOptionalValue() == null;
    }

    private void createEmbeddedEntityManagerFactory() throws ModuleLoadException {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            Map<Object, Object> properties = new HashMap<Object, Object>();
            properties.put("javax.persistence.jtaDataSource", this.jpaStoreDataSource);
            properties.put(AvailableSettings.JTA_PLATFORM, new JBossAppServerJtaPlatform(JtaManagerImpl.getInstance()));

            if (this.entitiesModule != null) {
                Thread.currentThread().setContextClassLoader(this.entitiesModule.getClassLoader());
            }

            this.emf = Persistence.createEntityManagerFactory(this.jpaStoreEntityModuleUnitName, properties);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    public InjectedValue<ValueManagedReferenceFactory> getProvidedEntityManagerFactory() {
        return this.providedEntityManagerFactory;
    }

    private class EntityManagerTx implements InvocationHandler {

        private final EntityManager em;
        private final Module entityModule;

        public EntityManagerTx(EntityManager em, Module entitiesModule) {
            this.em = em;
            this.entityModule = entitiesModule;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            UserTransaction tx = null;

            if (isTxRequired(method, args)) {
                tx = getUserTransaction();

                if (tx.getStatus() == Status.STATUS_NO_TRANSACTION) {
                    tx.begin();
                } else {
                    tx = null;
                }

                this.em.joinTransaction();
            }

            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

            try {
                if (this.entityModule != null) {
                    Thread.currentThread().setContextClassLoader(this.entityModule.getClassLoader());
                }

                return method.invoke(this.em, args);
            } finally {
                if (this.entityModule != null) {
                    Thread.currentThread().setContextClassLoader(originalClassLoader);
                }

                if (tx != null) {
                    tx.commit();
                }
            }
        }

        private boolean isTxRequired(Method method, Object[] args) {
            String n = method.getName();
            if (n.equals("flush")) {
                return true;
            } else if (n.equals("getLockMode")) {
                return true;
            } else if (n.equals("lock")) {
                return true;
            } else if (n.equals("merge")) {
                return true;
            } else if (n.equals("persist")) {
                return true;
            } else if (n.equals("refresh")) {
                return true;
            } else if (n.equals("remove")) {
                return true;
            } else {
                return false;
            }
        }

        private UserTransaction getUserTransaction() {
            try {
                return (UserTransaction) new InitialContext().lookup("java:jboss/UserTransaction");
            } catch (NamingException e) {
                throw new PersistenceException(e);
            }
        }
    }

}