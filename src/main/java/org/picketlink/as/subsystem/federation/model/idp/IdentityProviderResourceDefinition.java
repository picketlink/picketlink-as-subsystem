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

package org.picketlink.as.subsystem.federation.model.idp;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.picketlink.as.subsystem.federation.model.handlers.HandlerResourceDefinition;
import org.picketlink.as.subsystem.model.AbstractResourceDefinition;
import org.picketlink.as.subsystem.model.ModelElement;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 16, 2012
 */
public class IdentityProviderResourceDefinition extends AbstractResourceDefinition {

    public static final SimpleAttributeDefinition URL = new SimpleAttributeDefinitionBuilder(ModelElement.COMMON_URL.getName(), ModelType.STRING, false).setAllowExpression(true).build();
    public static final SimpleAttributeDefinition SECURITY_DOMAIN = new SimpleAttributeDefinitionBuilder(ModelElement.COMMON_SECURITY_DOMAIN.getName(), ModelType.STRING, false).setAllowExpression(true).build();
    public static final SimpleAttributeDefinition ALIAS = new SimpleAttributeDefinitionBuilder(ModelElement.COMMON_ALIAS.getName(), ModelType.STRING, false).setAllowExpression(true).build();
    public static final SimpleAttributeDefinition ENCRYPT = new SimpleAttributeDefinitionBuilder(ModelElement.IDENTITY_PROVIDER_ENCRYPT.getName(), ModelType.BOOLEAN, true).setDefaultValue(new ModelNode().set(false)).setAllowExpression(true).build();
    public static final SimpleAttributeDefinition SUPPORTS_SIGNATURES = new SimpleAttributeDefinitionBuilder(ModelElement.COMMON_SUPPORTS_SIGNATURES.getName(), ModelType.BOOLEAN, true).setDefaultValue(new ModelNode().set(false)).setAllowExpression(true).build();
    public static final SimpleAttributeDefinition STRICT_POST_BINDING = new SimpleAttributeDefinitionBuilder(ModelElement.COMMON_STRICT_POST_BINDING.getName(), ModelType.BOOLEAN, true).setDefaultValue(new ModelNode().set(true)).setAllowExpression(true).build();
    public static final SimpleAttributeDefinition ATTRIBUTE_MANAGER = new SimpleAttributeDefinitionBuilder(ModelElement.IDENTITY_PROVIDER_ATTRIBUTE_MANAGER.getName(), ModelType.STRING, true).setAllowExpression(true).build();
    public static final SimpleAttributeDefinition ROLE_GENERATOR = new SimpleAttributeDefinitionBuilder(ModelElement.IDENTITY_PROVIDER_ROLE_GENERATOR.getName(), ModelType.STRING, true).setAllowExpression(true).build();
    private static final SimpleAttributeDefinition EXTERNAL = new SimpleAttributeDefinitionBuilder(ModelElement.IDENTITY_PROVIDER_EXTERNAL.getName(), ModelType.BOOLEAN, true).setDefaultValue(new ModelNode().set(false)).setAllowExpression(true).build();
    public static final IdentityProviderResourceDefinition INSTANCE = new IdentityProviderResourceDefinition();

    private IdentityProviderResourceDefinition() {
        super(ModelElement.IDENTITY_PROVIDER, IdentityProviderAddHandler.INSTANCE, IdentityProviderRemoveHandler.INSTANCE, URL,
                 ALIAS, SECURITY_DOMAIN, EXTERNAL, ENCRYPT, SUPPORTS_SIGNATURES, STRICT_POST_BINDING, ATTRIBUTE_MANAGER,
                 ROLE_GENERATOR);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        addChildResourceDefinition(TrustDomainResourceDefinition.INSTANCE, resourceRegistration);
        addChildResourceDefinition(HandlerResourceDefinition.INSTANCE, resourceRegistration);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        for (final SimpleAttributeDefinition def : IdentityProviderMetricsOperationHandler.ATTRIBUTES) {
            resourceRegistration.registerMetric(def, IdentityProviderMetricsOperationHandler.INSTANCE);
        }
    }
}
