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
import org.picketlink.as.subsystem.model.ModelElement;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 16, 2012
 */
public class JPAStoreResourceDefinition extends AbstractIdentityStoreResourceDefinition {

    public static final SimpleAttributeDefinition DATA_SOURCE = new SimpleAttributeDefinitionBuilder(ModelElement.JPA_STORE_DATASOURCE.getName(), ModelType.STRING, true).setAllowExpression(true).build();
    public static final SimpleAttributeDefinition ENTITY_MODULE = new SimpleAttributeDefinitionBuilder(ModelElement.JPA_STORE_ENTITY_MODULE.getName(), ModelType.STRING, true).setAllowExpression(true).build();
    public static final SimpleAttributeDefinition ENTITY_MODULE_UNIT_NAME = new SimpleAttributeDefinitionBuilder(ModelElement.JPA_STORE_ENTITY_MODULE_UNIT_NAME.getName(), ModelType.STRING, true).setAllowExpression(true).build();
    public static final SimpleAttributeDefinition ENTITY_MANAGER_FACTORY = new SimpleAttributeDefinitionBuilder(ModelElement.JPA_STORE_ENTITY_MANAGER_FACTORY.getName(), ModelType.STRING, true).setAllowExpression(true).build();
    public static final JPAStoreResourceDefinition INSTANCE = new JPAStoreResourceDefinition(DATA_SOURCE, ENTITY_MODULE, ENTITY_MODULE_UNIT_NAME, ENTITY_MANAGER_FACTORY, MODULE, SUPPORT_ATTRIBUTE, SUPPORT_CREDENTIAL);

    private JPAStoreResourceDefinition(SimpleAttributeDefinition... attributes) {
        super(ModelElement.JPA_STORE, new IDMConfigAddStepHandler(attributes), attributes);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        addChildResourceDefinition(SupportedTypesResourceDefinition.INSTANCE, resourceRegistration);
        addChildResourceDefinition(CredentialHandlerResourceDefinition.INSTANCE, resourceRegistration);
    }
}
