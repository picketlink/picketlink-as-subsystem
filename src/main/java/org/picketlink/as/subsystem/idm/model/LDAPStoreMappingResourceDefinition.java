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

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;
import org.picketlink.as.subsystem.model.AbstractResourceDefinition;
import org.picketlink.as.subsystem.model.ModelElement;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 16, 2012
 */
public class LDAPStoreMappingResourceDefinition extends AbstractResourceDefinition {

    public static final SimpleAttributeDefinition CLASS = new SimpleAttributeDefinitionBuilder(
                                                                                                      ModelElement.LDAP_STORE_MAPPING_CLASS.getName(), ModelType.STRING, false).setAllowExpression(true).build();

    public static final SimpleAttributeDefinition BASE_DN = new SimpleAttributeDefinitionBuilder(
                                                                                                        ModelElement.LDAP_STORE_MAPPING_BASE_DN.getName(), ModelType.STRING, true).setAllowExpression(true).build();

    public static final SimpleAttributeDefinition OBJECT_CLASSES = new SimpleAttributeDefinitionBuilder(
                                                                                                               ModelElement.LDAP_STORE_MAPPING_OBJECT_CLASSES.getName(), ModelType.STRING, true).setAllowExpression(true).build();

    public static final SimpleAttributeDefinition PARENT_ATTRIBUTE = new SimpleAttributeDefinitionBuilder(
                                                                                                                 ModelElement.LDAP_STORE_MAPPING_PARENT_ATTRIBUTE_NAME.getName(), ModelType.STRING, true).setAllowExpression(true)
                                                                             .build();

    public static final SimpleAttributeDefinition RELATES_TO = new SimpleAttributeDefinitionBuilder(
                                                                                                           ModelElement.LDAP_STORE_MAPPING_RELATES_TO.getName(), ModelType.STRING, true).setAllowExpression(true).build();

    public static final LDAPStoreMappingResourceDefinition INSTANCE = new LDAPStoreMappingResourceDefinition(CLASS, BASE_DN,
                                                                                                                    OBJECT_CLASSES, PARENT_ATTRIBUTE, RELATES_TO);

    private LDAPStoreMappingResourceDefinition(SimpleAttributeDefinition... attributes) {
        super(ModelElement.LDAP_STORE_MAPPING, new IDMConfigAddStepHandler(attributes), attributes);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        addChildResourceDefinition(LDAPStoreAttributeResourceDefinition.INSTANCE, resourceRegistration);
    }
}
