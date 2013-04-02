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
package org.picketlink.as.subsystem.idm.cdi;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;

import org.jboss.logging.Logger;
import org.picketlink.IdentityConfigurationEvent;
import org.picketlink.authentication.internal.DefaultAuthenticatorSelector;
import org.picketlink.authentication.internal.IdmAuthenticator;
import org.picketlink.authentication.web.AuthenticationFilter;
import org.picketlink.credential.DefaultLoginCredentials;
import org.picketlink.deltaspike.core.util.ProjectStageProducer;
import org.picketlink.deltaspike.security.api.authorization.AccessDecisionVoter;
import org.picketlink.deltaspike.security.impl.authorization.DefaultAccessDecisionVoterContext;
import org.picketlink.deltaspike.security.impl.extension.DefaultSecurityStrategy;
import org.picketlink.deltaspike.security.impl.extension.SecurityInterceptor;
import org.picketlink.idm.config.FeatureSet;
import org.picketlink.idm.jpa.internal.JPAIdentityStoreConfiguration;
import org.picketlink.idm.jpa.schema.CredentialObject;
import org.picketlink.idm.jpa.schema.CredentialObjectAttribute;
import org.picketlink.idm.jpa.schema.IdentityObject;
import org.picketlink.idm.jpa.schema.IdentityObjectAttribute;
import org.picketlink.idm.jpa.schema.PartitionObject;
import org.picketlink.idm.jpa.schema.RelationshipIdentityObject;
import org.picketlink.idm.jpa.schema.RelationshipObject;
import org.picketlink.idm.jpa.schema.RelationshipObjectAttribute;
import org.picketlink.internal.CDIEventBridge;
import org.picketlink.internal.DefaultIdentity;
import org.picketlink.internal.EEIdentityStoreInvocationContextFactory;
import org.picketlink.internal.JPAIdentityStoreAutoConfig;
import org.picketlink.permission.internal.DefaultPermissionManager;
import org.picketlink.permission.internal.JPAPermissionStore;
import org.picketlink.permission.internal.JPAPermissionStoreConfig;
import org.picketlink.permission.internal.PermissionHandlerPolicy;
import org.picketlink.producer.IdentityManagerProducer;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class PicketLinkCdiExtension implements Extension {

    private static final Logger log = Logger.getLogger("org.eventjuggler.services.identity");

    public void register(@Observes BeforeBeanDiscovery bbd, BeanManager bm) {

        Util u = new Util(bbd, bm);

        // Identity
        u.add(IdentityEntityManagerProducer.class);

        // IDM Schema
        u.add(CredentialObjectAttribute.class, CredentialObject.class, IdentityObjectAttribute.class, IdentityObject.class,
                PartitionObject.class, RelationshipIdentityObject.class, RelationshipObjectAttribute.class,
                RelationshipObject.class);

        // Core API
        u.add(AuthenticationFilter.class);
        u.add(DefaultLoginCredentials.class);
        u.add(DefaultSecurityStrategy.class);

        // Core Impl
        u.add(DefaultAuthenticatorSelector.class);
        u.add(ProjectStageProducer.class);
        u.add(AccessDecisionVoter.class);
        u.add(DefaultAccessDecisionVoterContext.class);
        u.add(SecurityInterceptor.class);
        u.add(CDIEventBridge.class);
        u.add(DefaultIdentity.class);
        u.add(EEIdentityStoreInvocationContextFactory.class);
        u.add(JPAIdentityStoreAutoConfig.class);
        u.add(DefaultPermissionManager.class);
        u.add(JPAPermissionStore.class);
        u.add(JPAPermissionStoreConfig.class);
        u.add(PermissionHandlerPolicy.class);
        u.add(IdentityManagerProducer.class);
        u.add(IdmAuthenticator.class);
    }

    class Util {
        BeforeBeanDiscovery bbd;
        BeanManager bm;

        public Util(BeforeBeanDiscovery bbd, BeanManager bm) {
            this.bbd = bbd;
            this.bm = bm;
        }

        public void add(Class<?>... classes) {
            for (Class<?> c : classes) {
                bbd.addAnnotatedType(bm.createAnnotatedType(c));
            }
        }
    }

    public void configJPAIdentityStoreAutoConfig(@Observes IdentityConfigurationEvent event) {
        JPAIdentityStoreConfiguration config = new JPAIdentityStoreConfiguration();
        config.setIdentityClass(IdentityObject.class);
        config.setCredentialClass(CredentialObject.class);
        config.setCredentialAttributeClass(CredentialObjectAttribute.class);
        config.setAttributeClass(IdentityObjectAttribute.class);
        config.setRelationshipClass(RelationshipObject.class);
        config.setRelationshipIdentityClass(RelationshipIdentityObject.class);
        config.setRelationshipAttributeClass(RelationshipObjectAttribute.class);
        config.setPartitionClass(PartitionObject.class);

        FeatureSet.addFeatureSupport(config.getFeatureSet());
        FeatureSet.addRelationshipSupport(config.getFeatureSet());
        config.getFeatureSet().setSupportsCustomRelationships(true);
        config.getFeatureSet().setSupportsMultiRealm(true);
        event.getConfig().addStoreConfiguration(config);

        log.info("Config PicketLink JPA identity store config");
    }

    public <X> void vetoJPAIdentityStoreAutoConfig(@Observes ProcessAnnotatedType<X> event) {
        if (event.getAnnotatedType().getJavaClass().equals(JPAIdentityStoreAutoConfig.class)) {
            log.info("Veto bundled PicketLink JPA identity store config");
            event.veto();
        }
    }

}
