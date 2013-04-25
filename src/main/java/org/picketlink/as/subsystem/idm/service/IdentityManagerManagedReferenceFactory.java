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

import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.IdentityManagerFactory;
import org.picketlink.idm.model.Realm;

/**
 * @author Pedro Igor
 *
 */
public class IdentityManagerManagedReferenceFactory implements ManagedReferenceFactory {

    private IdentityManagerFactory identityManagerFactory;
    private String realmName;

    public IdentityManagerManagedReferenceFactory(String realmName, IdentityManagerFactory identityManagerFactory) {
        this.realmName = realmName;
        this.identityManagerFactory = identityManagerFactory;
    }
    
    @Override
    public ManagedReference getReference() {
        return new ManagedReference() {
            
            private IdentityManager identityManager = getIdentityManagerFactory().createIdentityManager(new Realm(realmName));
            
            @Override
            public void release() {
                this.identityManager = null;
            }

            @Override
            public Object getInstance() {
                return this.identityManager;
            }
        };
    }

    public IdentityManagerFactory getIdentityManagerFactory() {
        return this.identityManagerFactory;
    }

}
