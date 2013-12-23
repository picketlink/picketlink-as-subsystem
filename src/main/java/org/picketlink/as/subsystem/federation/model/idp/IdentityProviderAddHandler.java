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

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.picketlink.as.subsystem.federation.service.FederationService;
import org.picketlink.as.subsystem.federation.service.IdentityProviderService;
import org.picketlink.identity.federation.core.config.IDPConfiguration;

import java.util.List;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class IdentityProviderAddHandler extends AbstractAddStepHandler {

    static final IdentityProviderAddHandler INSTANCE = new IdentityProviderAddHandler();

    private IdentityProviderAddHandler() {

    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition attribute : IdentityProviderResourceDefinition.INSTANCE.getAttributes()) {
            attribute.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        PathAddress pathAddress = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS));
        String alias = pathAddress.getLastElement().getValue();
        IdentityProviderService service = new IdentityProviderService(toIDPConfig(context, model));
        ServiceBuilder<IdentityProviderService> serviceBuilder = context.getServiceTarget().addService(IdentityProviderService.createServiceName(alias), service);
        String federationAlias = pathAddress.subAddress(0, pathAddress.size() - 1).getLastElement().getValue();

        serviceBuilder.addDependency(FederationService.createServiceName(federationAlias), FederationService.class,
                                            service.getFederationService());

        ServiceController<IdentityProviderService> controller = serviceBuilder
                                                                    .addListener(verificationHandler)
                                                                    .setInitialMode(ServiceController.Mode.PASSIVE)
                                                                    .install();

        if (newControllers != null) {
            newControllers.add(controller);
        }
    }

    private IDPConfiguration toIDPConfig(OperationContext context, ModelNode fromModel) throws OperationFailedException {
        IDPConfiguration idpType = new IDPConfiguration();

        String alias = IdentityProviderResourceDefinition.ALIAS.resolveModelAttribute(context, fromModel).asString();

        idpType.setAlias(alias);

        String url = IdentityProviderResourceDefinition.URL.resolveModelAttribute(context, fromModel).asString();

        idpType.setIdentityURL(url);

        boolean supportsSignatures = IdentityProviderResourceDefinition.SUPPORTS_SIGNATURES.resolveModelAttribute(context, fromModel).asBoolean();

        idpType.setSupportsSignature(supportsSignatures);

        boolean encrypt = IdentityProviderResourceDefinition.ENCRYPT.resolveModelAttribute(context, fromModel).asBoolean();

        idpType.setEncrypt(encrypt);

        boolean strictPostBinding = IdentityProviderResourceDefinition.STRICT_POST_BINDING.resolveModelAttribute(context, fromModel).asBoolean();

        idpType.setStrictPostBinding(strictPostBinding);

        String securityDomain = IdentityProviderResourceDefinition.SECURITY_DOMAIN.resolveModelAttribute(context, fromModel).asString();

        idpType.setSecurityDomain(securityDomain);

        ModelNode attributeManager = IdentityProviderResourceDefinition.ATTRIBUTE_MANAGER.resolveModelAttribute(context, fromModel);

        if (attributeManager.isDefined()) {
            idpType.setAttributeManager(attributeManager.asString());
        }

        ModelNode roleGenerator = IdentityProviderResourceDefinition.ROLE_GENERATOR.resolveModelAttribute(context, fromModel);

        if (roleGenerator.isDefined()) {
            idpType.setRoleGenerator(roleGenerator.asString());
        }

        return idpType;
    }
}