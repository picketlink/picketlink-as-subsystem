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

package org.picketlink.as.subsystem.federation.model;

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
import org.picketlink.as.subsystem.federation.service.KeyProviderService;
import org.picketlink.config.federation.AuthPropertyType;
import org.picketlink.config.federation.KeyProviderType;
import org.picketlink.identity.federation.core.impl.KeyStoreKeyManager;

import java.util.List;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class KeyProviderAddHandler extends AbstractAddStepHandler {

    public static final KeyProviderAddHandler INSTANCE = new KeyProviderAddHandler();

    private KeyProviderAddHandler() {
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition attribute : KeyProviderResourceDefinition.INSTANCE.getAttributes()) {
            attribute.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model,
                                     final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers)
        throws OperationFailedException {
        PathAddress pathAddress = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS));
        String federationAlias = pathAddress.subAddress(0, pathAddress.size() - 1).getLastElement().getValue();

        KeyProviderService service = new KeyProviderService(toKeyProviderType(context, model));

        ServiceBuilder<KeyProviderService> serviceBuilder = context.getServiceTarget().addService(KeyProviderService.createServiceName(federationAlias), service);

        serviceBuilder.addDependency(FederationService.createServiceName(federationAlias), FederationService.class,
                                        service.getFederationService());

        ServiceController<KeyProviderService> controller = serviceBuilder.addListener(verificationHandler)
                                                               .setInitialMode(ServiceController.Mode.PASSIVE).install();

        if (newControllers != null) {
            newControllers.add(controller);
        }
    }

    private KeyProviderType toKeyProviderType(OperationContext context, ModelNode model) throws OperationFailedException {
        KeyProviderType keyProviderType = new KeyProviderType();

        keyProviderType.setClassName(KeyStoreKeyManager.class.getName());

        keyProviderType.setSigningAlias(KeyProviderResourceDefinition.SIGN_KEY_ALIAS.resolveModelAttribute(context, model).asString());

        AuthPropertyType keyStoreURL = new AuthPropertyType();

        keyStoreURL.setKey("KeyStoreURL");
        keyStoreURL.setValue(KeyProviderResourceDefinition.URL.resolveModelAttribute(context, model).asString());

        keyProviderType.add(keyStoreURL);

        AuthPropertyType keyStorePass = new AuthPropertyType();

        keyStorePass.setKey("KeyStorePass");
        keyStorePass.setValue(KeyProviderResourceDefinition.PASSWD.resolveModelAttribute(context, model).asString());

        keyProviderType.add(keyStorePass);

        AuthPropertyType signingKeyPass = new AuthPropertyType();

        signingKeyPass.setKey("SigningKeyPass");
        signingKeyPass.setValue(KeyProviderResourceDefinition.SIGN_KEY_PASSWD.resolveModelAttribute(context, model).asString());

        keyProviderType.add(signingKeyPass);

        AuthPropertyType signingKeyAlias = new AuthPropertyType();

        signingKeyAlias.setKey("SigningKeyAlias");
        signingKeyAlias.setValue(KeyProviderResourceDefinition.SIGN_KEY_ALIAS.resolveModelAttribute(context, model).asString());

        keyProviderType.add(signingKeyAlias);

        return keyProviderType;
    }
}