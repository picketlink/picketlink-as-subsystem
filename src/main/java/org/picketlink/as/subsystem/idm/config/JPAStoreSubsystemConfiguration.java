package org.picketlink.as.subsystem.idm.config;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.transaction.jta.platform.internal.JBossAppServerJtaPlatform;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.value.InjectedValue;
import org.picketlink.idm.config.JPAIdentityStoreConfiguration;
import org.picketlink.idm.config.SecurityConfigurationException;
import org.picketlink.idm.credential.handler.CredentialHandler;
import org.picketlink.idm.jpa.internal.JPAIdentityStore;
import org.picketlink.idm.model.AttributedType;
import org.picketlink.idm.spi.ContextInitializer;
import org.picketlink.idm.spi.IdentityContext;
import org.picketlink.idm.spi.IdentityStore;

import javax.naming.InitialContext;
import javax.naming.NamingException;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.reflect.Modifier.isAbstract;
import static org.picketlink.common.util.StringUtil.isNullOrEmpty;

/**
 * @author Pedro Igor
 */
public class JPAStoreSubsystemConfiguration extends JPAIdentityStoreConfiguration {

    private static final String JPA_ANNOTATION_PACKAGE = "org.picketlink.idm.jpa.annotations";

    private EntityManagerFactory emf;

    private String entityModuleUnitName = "identity";
    private final Module entityModule;
    private final String dataSourceJndiUrl;
    private final String entityManagerFactoryJndiName;
    private InjectedValue<TransactionManager> transactionManager;

    protected JPAStoreSubsystemConfiguration(
            final String entityModuleName,
            final String entityModuleUnitName,
            final String dataSourceJndiUrl,
            final String entityManagerFactoryJndiName,
            final InjectedValue<TransactionManager> transactionManager,
            final Set<Class<?>> entityTypes,
            final Map<Class<? extends AttributedType>, Set<IdentityOperation>> supportedTypes,
            final Map<Class<? extends AttributedType>, Set<IdentityOperation>> unsupportedTypes,
            final List<ContextInitializer> contextInitializers,
            final Map<String, Object> credentialHandlerProperties,
            final Set<Class<? extends CredentialHandler>> credentialHandlers,
            final boolean supportsAttribute,
            final boolean supportsCredential) {
        super(entityTypes, supportedTypes, unsupportedTypes, contextInitializers, credentialHandlerProperties, credentialHandlers, supportsAttribute, supportsCredential);

        this.transactionManager = transactionManager;

        if (entityModuleName != null) {
            ModuleLoader moduleLoader = Module.getContextModuleLoader();

            try {
                this.entityModule = moduleLoader.loadModule(ModuleIdentifier.create(entityModuleName));
            } catch (ModuleLoadException e) {
                throw new SecurityConfigurationException("Entities module not found [" + entityModuleName + "].");
            }
        } else {
            this.entityModule = null;
        }

        this.entityModuleUnitName = entityModuleUnitName;
        this.dataSourceJndiUrl = dataSourceJndiUrl;
        this.entityManagerFactoryJndiName = entityManagerFactoryJndiName;

        try {
            configureEntityManagerFactory();
            configureEntities();
        } catch (Exception e) {
            throw new SecurityConfigurationException("Could not configure JPA store.", e);
        }
    }

    @Override
    public void initializeContext(final IdentityContext context, final IdentityStore<?> store) {
        if (store instanceof JPAIdentityStore) {
            if (!context.isParameterSet(JPAIdentityStore.INVOCATION_CTX_ENTITY_MANAGER)) {
                context.setParameter(JPAIdentityStore.INVOCATION_CTX_ENTITY_MANAGER, getEntityManager());
            }
        }
    }

    @Override
    public Class<? extends IdentityStore> getIdentityStoreType() {
        return JPAIdentityStore.class;
    }

    private EntityManagerFactory createEmbeddedEntityManagerFactory() {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            Map<Object, Object> properties = new HashMap<Object, Object>();

            if (!isNullOrEmpty(this.dataSourceJndiUrl)) {
                properties.put("javax.persistence.jtaDataSource", this.dataSourceJndiUrl);
            }

            properties.put(AvailableSettings.JTA_PLATFORM, new JBossAppServerJtaPlatform());

            if (this.entityModule != null) {
                Thread.currentThread().setContextClassLoader(this.entityModule.getClassLoader());
            }

            return Persistence.createEntityManagerFactory(this.entityModuleUnitName, properties);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    private void configureEntities() {
        Set<EntityType<?>> mappedEntities = this.emf.getMetamodel().getEntities();

        for (EntityType<?> entity : mappedEntities) {
            Class<?> javaType = entity.getJavaType();

            if (!isAbstract(javaType.getModifiers()) && isIdentityEntity(javaType)) {
                getEntityTypes().add(javaType);
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

    private void configureEntityManagerFactory() {
        if (this.entityManagerFactoryJndiName != null) {
            this.emf = lookupEntityManagerFactory();
        } else {
            this.emf = createEmbeddedEntityManagerFactory();
        }
    }

    private EntityManagerFactory lookupEntityManagerFactory() {
        try {
            return (EntityManagerFactory) new InitialContext().lookup(this.entityManagerFactoryJndiName);
        } catch (NamingException e) {
            throw new SecurityConfigurationException("Could not lookup EntityManagerFactory [" + this.entityManagerFactoryJndiName + "].");
        }
    }

    private EntityManager getEntityManager() {
        return (EntityManager) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class<?>[]{EntityManager.class}, new EntityManagerInvocationHandler(emf.createEntityManager(),
                entityModule));
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
            Transaction tx = null;

            if (isTxRequired(method, args)) {
                if (transactionManager.getValue().getStatus() == Status.STATUS_NO_TRANSACTION) {
                    transactionManager.getValue().begin();
                    tx = transactionManager.getValue().getTransaction();
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
                Thread.currentThread().setContextClassLoader(originalClassLoader);

                if (tx != null) {
                    tx.commit();
                    transactionManager.getValue().suspend();
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
}