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
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.cfg.AvailableSettings;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.jpa.hibernate4.JBossAppServerJtaPlatform;
import org.jboss.as.jpa.transaction.JtaManagerImpl;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.config.FeatureSet;
import org.picketlink.idm.config.IdentityConfiguration;
import org.picketlink.idm.internal.DefaultIdentityManager;
import org.picketlink.idm.internal.DefaultIdentityStoreInvocationContextFactory;
import org.picketlink.idm.jpa.internal.JPAIdentityStoreConfiguration;
import org.picketlink.idm.jpa.schema.CredentialObject;
import org.picketlink.idm.jpa.schema.CredentialObjectAttribute;
import org.picketlink.idm.jpa.schema.IdentityObject;
import org.picketlink.idm.jpa.schema.IdentityObjectAttribute;
import org.picketlink.idm.jpa.schema.PartitionObject;
import org.picketlink.idm.jpa.schema.RelationshipIdentityObject;
import org.picketlink.idm.jpa.schema.RelationshipObject;
import org.picketlink.idm.jpa.schema.RelationshipObjectAttribute;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class IdentityManagerService implements Service<IdentityManager> {

    public static ServiceName SERVICE_NAME = ServiceName.JBOSS.append("identity");

    public static ServiceName JNDI_SERVICE_NAME = ContextNames.JBOSS_CONTEXT_SERVICE_NAME.append("IdentityManager");

    private EntityManagerFactory emf;

    private IdentityConfiguration identityConfiguration;

    public static ServiceController<IdentityManager> addService(final ServiceTarget target,
            final ServiceVerificationHandler verificationHandler) {
        IdentityManagerService service = new IdentityManagerService();
        ServiceBuilder<IdentityManager> serviceBuilder = target.addService(SERVICE_NAME, service);

        serviceBuilder.addDependency(ContextNames.JBOSS_CONTEXT_SERVICE_NAME.append("datasources", "ExampleDS"));

        serviceBuilder.addListener(verificationHandler);
        return serviceBuilder.install();
    }

    @Override
    public IdentityManager getValue() throws IllegalStateException, IllegalArgumentException {
        return createIdentityManager();
    }

    @Override
    public void start(StartContext context) throws StartException {
        ROOT_LOGGER.info("Starting Identity Service");

        ROOT_LOGGER.info("Creating entity manager factory");

        Map<Object, Object> properties = new HashMap<Object, Object>();
        properties.put(AvailableSettings.JTA_PLATFORM, new JBossAppServerJtaPlatform(JtaManagerImpl.getInstance()));

        emf = Persistence.createEntityManagerFactory("identity", properties);
        identityConfiguration = createConfig();
    }

    private IdentityManager createIdentityManager() {
        IdentityManager identityManager = new DefaultIdentityManager();

        DefaultIdentityStoreInvocationContextFactory icf = new DefaultIdentityStoreInvocationContextFactory();

        EntityManager em = (EntityManager) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { EntityManager.class }, new EntityManagerTx(emf.createEntityManager()));

        icf.setEntityManager(em);

        identityManager.bootstrap(identityConfiguration, icf);

        return identityManager;
    }

    private IdentityConfiguration createConfig() {
        IdentityConfiguration config = new IdentityConfiguration();

        JPAIdentityStoreConfiguration jpaConfig = new JPAIdentityStoreConfiguration();
        jpaConfig.setIdentityClass(IdentityObject.class);
        jpaConfig.setCredentialClass(CredentialObject.class);
        jpaConfig.setCredentialAttributeClass(CredentialObjectAttribute.class);
        jpaConfig.setAttributeClass(IdentityObjectAttribute.class);
        jpaConfig.setRelationshipClass(RelationshipObject.class);
        jpaConfig.setRelationshipIdentityClass(RelationshipIdentityObject.class);
        jpaConfig.setRelationshipAttributeClass(RelationshipObjectAttribute.class);
        jpaConfig.setPartitionClass(PartitionObject.class);

        FeatureSet.addFeatureSupport(jpaConfig.getFeatureSet());
        FeatureSet.addRelationshipSupport(jpaConfig.getFeatureSet());
        jpaConfig.getFeatureSet().setSupportsCustomRelationships(true);
        jpaConfig.getFeatureSet().setSupportsMultiRealm(true);

        config.addStoreConfiguration(jpaConfig);

        return config;
    }

    @Override
    public void stop(StopContext context) {
        ROOT_LOGGER.info("Stopping Identity Service");

        ROOT_LOGGER.info("Closing entity manager factory");
        emf.close();
    }

}
