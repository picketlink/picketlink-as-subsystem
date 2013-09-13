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
import org.picketlink.idm.config.IdentityConfigurationBuilder;
import org.picketlink.idm.config.JPAStoreConfigurationBuilder;
import org.picketlink.idm.config.NamedIdentityConfigurationBuilder;
import org.picketlink.idm.jpa.internal.JPAIdentityStore;
import org.picketlink.idm.spi.ContextInitializer;
import org.picketlink.idm.spi.IdentityContext;
import org.picketlink.idm.spi.IdentityStore;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.metamodel.EntityType;
import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.reflect.Modifier.*;
import static org.picketlink.as.subsystem.PicketLinkLogger.*;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JPABasedIdentityManagerFactoryService extends PartitionManagerService {

    private static final String JPA_ANNOTATION_PACKAGE = "org.picketlink.idm.jpa.annotations";

    private final InjectedValue<ValueManagedReferenceFactory> providedEntityManagerFactory = new InjectedValue<ValueManagedReferenceFactory>();
    private final InjectedValue<TransactionManager> transactionManager = new InjectedValue<TransactionManager>();
    private final NamedIdentityConfigurationBuilder namedBuilder;
    private EntityManagerFactory emf;
    private final String jpaStoreDataSource;
    private String jpaStoreEntityModuleUnitName = "identity";
    private Module entitiesModule;

    public JPABasedIdentityManagerFactoryService(String alias, String jndiName, String dataSourceJndiName,
            String entityModuleName, String entityModuleUnitName, NamedIdentityConfigurationBuilder namedBuilder, IdentityConfigurationBuilder builder) {
        super(alias, jndiName, builder);
        this.namedBuilder = namedBuilder;
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
        JPAStoreConfigurationBuilder jpaConfig = getJPAIdentityStoreConfiguration();

        if (!hasEmbeddedEntityManagerFactory()) {
            this.emf = (EntityManagerFactory) this.providedEntityManagerFactory.getValue().getReference().getInstance();
        } else {
            try {
                ROOT_LOGGER.debug("Creating entity manager factory for module: " + "myschema");
                createEmbeddedEntityManagerFactory();
            } catch (ModuleLoadException e) {
                throw new RuntimeException(e);
            }
        }

        configureIDMEntities(jpaConfig);

        jpaConfig.addContextInitializer(new ContextInitializer() {
            @Override
            public void initContextForStore(IdentityContext context, IdentityStore<?> store) {
                if (store instanceof JPAIdentityStore) {
                    if (!context.isParameterSet(JPAIdentityStore.INVOCATION_CTX_ENTITY_MANAGER)) {
                        context.setParameter(JPAIdentityStore.INVOCATION_CTX_ENTITY_MANAGER, getEntityManager());
                    }
                }
            }
            public EntityManager getEntityManager() {
                return (EntityManager) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                        new Class<?>[] { EntityManager.class }, new EntityManagerInvocationHandler(emf.createEntityManager(),
                                entitiesModule));
            }
        });
    }

    private JPAStoreConfigurationBuilder getJPAIdentityStoreConfiguration() {
        return namedBuilder.stores().jpa();
    }

    private void configureIDMEntities(JPAStoreConfigurationBuilder jpaConfig) {
        Set<EntityType<?>> mappedEntities = this.emf.getMetamodel().getEntities();
        List<Class<?>> entities = new ArrayList<Class<?>>();

        for (EntityType<?> entity : mappedEntities) {
            Class<?> javaType = entity.getJavaType();

            if (!isAbstract(javaType.getModifiers()) && isIdentityEntity(javaType)) {
                entities.add(javaType);
            }
        }

        jpaConfig.mappedEntity(entities.toArray(new Class<?>[entities.size()]));
    }

    private boolean hasEmbeddedEntityManagerFactory() {
        return this.providedEntityManagerFactory.getOptionalValue() == null;
    }

    private void createEmbeddedEntityManagerFactory() throws ModuleLoadException {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            Map<Object, Object> properties = new HashMap<Object, Object>();

            if (!StringUtil.isNullOrEmpty(this.jpaStoreDataSource)) {
                properties.put("javax.persistence.jtaDataSource", this.jpaStoreDataSource);
            }

            properties.put(AvailableSettings.JTA_PLATFORM, new JBossAppServerJtaPlatform(JtaManagerImpl.getInstance()));

            if (this.entitiesModule != null) {
                Thread.currentThread().setContextClassLoader(this.entitiesModule.getClassLoader());
            }

            this.emf = Persistence.createEntityManagerFactory(this.jpaStoreEntityModuleUnitName, properties);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    public InjectedValue<TransactionManager> getTransactionManager() {
        return transactionManager;
    }

    public InjectedValue<ValueManagedReferenceFactory> getProvidedEntityManagerFactory() {
        return this.providedEntityManagerFactory;
    }

    private class EntityManagerInvocationHandler implements InvocationHandler {

        private final EntityManager em;
        private final Module entityModule;

        public EntityManagerInvocationHandler(EntityManager em, Module entitiesModule) {
            this.em = em;
            this.entityModule = entitiesModule;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            TransactionManager tm = transactionManager.getValue();
            Transaction tx = null;

            if (isTxRequired(method, args)) {
                if (tm.getStatus() == Status.STATUS_NO_TRANSACTION) {
                    tm.begin();
                    tx = tm.getTransaction();
                }

                this.em.joinTransaction();
            }

            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

            try {
                Thread.currentThread().setContextClassLoader(this.entityModule.getClassLoader());

                return method.invoke(this.em, args);
            } finally {
                Thread.currentThread().setContextClassLoader(originalClassLoader);

                if (tx != null) {
                    tx.commit();
                    tm.suspend();
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

    }

    private boolean isIdentityEntity(Class<?> cls) {
        while (!cls.equals(Object.class)) {
            for (Annotation a : cls.getAnnotations()) {
                if (a.annotationType().getName().startsWith(JPA_ANNOTATION_PACKAGE)) {
                    return true;
                }
            }

            // No class annotation was found, check the fields
            for (Field f : cls.getDeclaredFields()) {
                for (Annotation a : f.getAnnotations()) {
                    if (a.annotationType().getName().startsWith(JPA_ANNOTATION_PACKAGE)) {
                        return true;
                    }
                }
            }

            // Check the superclass
            cls = cls.getSuperclass();
        }

        return false;
    }
}