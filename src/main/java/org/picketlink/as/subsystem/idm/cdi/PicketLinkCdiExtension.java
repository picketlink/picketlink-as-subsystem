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

import org.picketlink.authentication.internal.DefaultAuthenticatorSelector;
import org.picketlink.authentication.internal.IdmAuthenticator;
import org.picketlink.authentication.web.AuthenticationFilter;
import org.picketlink.credential.DefaultLoginCredentials;
import org.picketlink.deltaspike.core.util.ProjectStageProducer;
import org.picketlink.deltaspike.security.api.authorization.AccessDecisionVoter;
import org.picketlink.deltaspike.security.impl.authorization.DefaultAccessDecisionVoterContext;
import org.picketlink.deltaspike.security.impl.extension.DefaultSecurityStrategy;
import org.picketlink.deltaspike.security.impl.extension.SecurityInterceptor;
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

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class PicketLinkCdiExtension implements Extension {

    public void register(@Observes BeforeBeanDiscovery bbd, BeanManager bm) {

        Util u = new Util(bbd, bm);

        // Identity
        u.add(org.picketlink.as.subsystem.idm.cdi.IdentityManagerProducer.class);

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

}
