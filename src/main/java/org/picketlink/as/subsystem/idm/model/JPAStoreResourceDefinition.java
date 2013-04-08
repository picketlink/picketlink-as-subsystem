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

import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.picketlink.as.subsystem.model.AbstractResourceDefinition;
import org.picketlink.as.subsystem.model.ModelElement;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 16, 2012
 */
public class JPAStoreResourceDefinition extends AbstractResourceDefinition {

    public static final JPAStoreResourceDefinition INSTANCE = new JPAStoreResourceDefinition();

    public static final SimpleAttributeDefinition DATA_SOURCE = new SimpleAttributeDefinitionBuilder(
            ModelElement.JPA_STORE_DATASOURCE.getName(), ModelType.STRING, true)
            .setAllowExpression(false).build();

    public static final SimpleAttributeDefinition ENTITY_MANAGER_FACTORY = new SimpleAttributeDefinitionBuilder(
            ModelElement.JPA_STORE_ENTITY_MANAGER_FACTORY.getName(), ModelType.STRING, true)
            .setAllowExpression(false).build();

    public static final SimpleAttributeDefinition REALMS = new SimpleAttributeDefinitionBuilder(
            ModelElement.REALMS.getName(), ModelType.STRING, true).setDefaultValue(new ModelNode().set("default"))
            .setAllowExpression(false).build();

    public static final SimpleAttributeDefinition TIERS = new SimpleAttributeDefinitionBuilder(
            ModelElement.TIERS.getName(), ModelType.STRING, true).setDefaultValue(new ModelNode().set(""))
            .setAllowExpression(false).build();

    static {
        INSTANCE.addAttribute(DATA_SOURCE);
        INSTANCE.addAttribute(ENTITY_MANAGER_FACTORY);
        INSTANCE.addAttribute(REALMS);
        INSTANCE.addAttribute(TIERS);
    }
    
    private JPAStoreResourceDefinition() {
        super(ModelElement.JPA_STORE, JPAStoreAddHandler.INSTANCE, JPAStoreRemoveHandler.INSTANCE);
    }
    
    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        addChildResourceDefinition(FeatureSetResourceDefinition.INSTANCE, resourceRegistration);
        addChildResourceDefinition(RelationshipResourceDefinition.INSTANCE, resourceRegistration);
    }
    
    @Override
    protected OperationStepHandler doGetAttributeWriterHandler() {
        return JPAStoreWriteAttributeHandler.INSTANCE;
    }
}