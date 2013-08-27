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

package org.picketlink.as.subsystem.idm.model;

import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.registry.Resource.ResourceEntry;
import org.jboss.dmr.ModelNode;
import org.picketlink.as.subsystem.PicketLinkMessages;
import org.picketlink.as.subsystem.model.ModelElement;
import org.picketlink.idm.config.FileStoreConfigurationBuilder;
import org.picketlink.idm.config.IdentityStoreConfigurationBuilder;
import org.picketlink.idm.config.JPAIdentityStoreConfiguration;
import org.picketlink.idm.config.LDAPIdentityStoreConfiguration;
import org.picketlink.idm.config.NamedIdentityConfigurationBuilder;
import org.picketlink.idm.model.Relationship;

import java.util.Set;


/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 16, 2012
 */
public class IdentityManagementConfiguration {

    public static void configureStore(ModelElement storeType, Resource resource, final NamedIdentityConfigurationBuilder builder) {
        IdentityStoreConfigurationBuilder storeConfig = null;

        if (storeType.equals(ModelElement.JPA_STORE)) {
        } else if (storeType.equals(ModelElement.FILE_STORE)) {
            storeConfig = configureFileIdentityStore(resource.getModel(), builder);
        } else if (storeType.equals(ModelElement.LDAP_STORE)) {
        } else {
            throw PicketLinkMessages.MESSAGES.idmNoConfigurationProvided();
        }

        Set<ResourceEntry> featuresSetEntries = resource.getChildren(ModelElement.FEATURES.getName());
        
        if (featuresSetEntries != null && !featuresSetEntries.isEmpty()) {
            ResourceEntry featuresSet = featuresSetEntries.iterator().next();
            
            configureAllFeatures(featuresSet.getModel(), storeConfig);
            
            Set<ResourceEntry> featuresList = featuresSet.getChildren(ModelElement.FEATURE.getName());
            
            for (ResourceEntry feature : featuresList) {
            }
        }

        Set<ResourceEntry> relationshipsSet = resource.getChildren(ModelElement.RELATIONSHIP.getName());
        
        if (relationshipsSet != null && !relationshipsSet.isEmpty()) {
            for (ResourceEntry relationship : relationshipsSet) {
                configureRelationships(relationship.getModel(), storeConfig);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static  void configureRelationships(ModelNode operation, IdentityStoreConfigurationBuilder storeConfig) {
        String relationshipClass = operation.get(ModelElement.COMMON_CLASS.getName()).asString();

        try {
            storeConfig.supportType((Class<? extends Relationship>) Class.forName(relationshipClass));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static  void configureAllFeatures(ModelNode operation, IdentityStoreConfigurationBuilder storeConfig) {
        ModelNode supportsAll = operation.get(ModelElement.COMMON_SUPPORTS_ALL.getName());
        
        if (supportsAll.isDefined() && supportsAll.asBoolean()) {
            storeConfig.supportAllFeatures();            
        }
    }
 
    private static IdentityStoreConfigurationBuilder configureFileIdentityStore(ModelNode modelNode, final NamedIdentityConfigurationBuilder builder) {

        FileStoreConfigurationBuilder fileStoreBuilder = builder.stores().file();

        ModelNode workingDir = modelNode.get(ModelElement.FILE_STORE_WORKING_DIR.getName());
        ModelNode alwaysCreateFiles = modelNode.get(ModelElement.FILE_STORE_ALWAYS_CREATE_FILE.getName());
        ModelNode asyncWrite = modelNode.get(ModelElement.FILE_STORE_ASYNC_WRITE.getName());
        ModelNode asyncWriteThreadPool = modelNode.get(ModelElement.FILE_STORE_ASYNC_THREAD_POOL.getName());

        if (workingDir.isDefined()) {
            fileStoreBuilder.workingDirectory(workingDir.asString());
        }

        if (alwaysCreateFiles.isDefined()) {
            fileStoreBuilder.preserveState(!alwaysCreateFiles.asBoolean());
        }

        if (asyncWrite.isDefined()) {
            fileStoreBuilder.asyncWrite(asyncWrite.asBoolean());
        }

        if (asyncWriteThreadPool.isDefined()) {
            fileStoreBuilder.asyncWriteThreadPool(asyncWriteThreadPool.asInt());
        }

        return fileStoreBuilder;
    }

    private static  LDAPIdentityStoreConfiguration configureLDAPIdentityStore(ModelNode modelNode) {
//        LDAPIdentityStoreConfiguration storeConfig = new LDAPIdentityStoreConfiguration();
//
//        ModelNode url = modelNode.get(ModelElement.LDAP_STORE_URL.getName());
//        ModelNode bindDn = modelNode.get(ModelElement.LDAP_STORE_BIND_DN.getName());
//        ModelNode bindCredential = modelNode.get(ModelElement.LDAP_STORE_BIND_CREDENTIAL.getName());
//        ModelNode baseDn = modelNode.get(ModelElement.LDAP_STORE_BASE_DN_SUFFIX.getName());
//        ModelNode userDn = modelNode.get(ModelElement.LDAP_STORE_USER_DN_SUFFIX.getName());
//        ModelNode agentDn = modelNode.get(ModelElement.LDAP_STORE_AGENT_DN_SUFFIX.getName());
//        ModelNode groupDn = modelNode.get(ModelElement.LDAP_STORE_GROUP_DN_SUFFIX.getName());
//        ModelNode roleDn = modelNode.get(ModelElement.LDAP_STORE_ROLE_DN_SUFFIX.getName());
//
//        if (url.isDefined()) {
//            storeConfig.setLdapURL(url.asString());
//        }
//
//        if (bindDn.isDefined()) {
//            storeConfig.setBindDN(bindDn.asString());
//        }
//
//        if (bindCredential.isDefined()) {
//            storeConfig.setBindCredential(bindCredential.asString());
//        }
//
//        if (baseDn.isDefined()) {
//            storeConfig.setBaseDN(baseDn.asString());
//        }
//
//        if (userDn.isDefined()) {
//            storeConfig.setUserDNSuffix(userDn.asString());
//        }
//
//        if (agentDn.isDefined()) {
//            storeConfig.setAgentDNSuffix(agentDn.asString());
//        }
//
//        if (roleDn.isDefined()) {
//            storeConfig.setRoleDNSuffix(roleDn.asString());
//        }
//
//        if (groupDn.isDefined()) {
//            storeConfig.setGroupDNSuffix(groupDn.asString());
//        }
//
//        return storeConfig;
        return null;
    }
    
    private static  JPAIdentityStoreConfiguration configureJPAIdentityStore(ModelNode modelNode) {
        return null;
    }
    
}