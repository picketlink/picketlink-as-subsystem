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
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.model.Partition;

/**
 * @author Pedro Igor
 *
 */
public class IdentityManagerManagedReferenceFactory implements ManagedReferenceFactory {

    private PartitionManager partitionManager;
    private String realmName;

    public IdentityManagerManagedReferenceFactory(String realmName, PartitionManager partitionManager) {
        this.realmName = realmName;
        this.partitionManager = partitionManager;
    }
    
    @Override
    public ManagedReference getReference() {
        return new ManagedReference() {

            private IdentityManager identityManager;

            @Override
            public void release() {
                this.identityManager = null;
            }

            @Override
            public Object getInstance() {
                if (this.identityManager == null) {
                    Partition partition = getPartitionManager().getPartition(Partition.class, realmName);
                    this.identityManager = getPartitionManager().createIdentityManager(partition);
                }

                return this.identityManager;
            }
        };
    }

    public PartitionManager getPartitionManager() {
        return this.partitionManager;
    }

}
