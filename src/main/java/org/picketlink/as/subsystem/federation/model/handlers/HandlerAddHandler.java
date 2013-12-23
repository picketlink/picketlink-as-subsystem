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

package org.picketlink.as.subsystem.federation.model.handlers;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.picketlink.as.subsystem.federation.service.EntityProviderService;
import org.picketlink.as.subsystem.federation.service.IdentityProviderService;
import org.picketlink.as.subsystem.federation.service.SAMLHandlerService;
import org.picketlink.as.subsystem.federation.service.ServiceProviderService;
import org.picketlink.config.federation.KeyValueType;
import org.picketlink.config.federation.handler.Handler;

import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.picketlink.as.subsystem.federation.service.SAMLHandlerService.createServiceName;
import static org.picketlink.as.subsystem.model.ModelElement.COMMON_HANDLER_PARAMETER;
import static org.picketlink.as.subsystem.model.ModelElement.IDENTITY_PROVIDER;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class HandlerAddHandler extends AbstractAddStepHandler {

    static final HandlerAddHandler INSTANCE = new HandlerAddHandler();

    private HandlerAddHandler() {

    }

    void launchServices(final OperationContext context, final PathAddress pathAddress, final ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        Handler newHandler = new Handler();

        newHandler.setClazz(HandlerResourceDefinition.CLASS.resolveModelAttribute(context, model).asString());

        ModelNode handler = Resource.Tools.readModel(context.readResourceFromRoot(pathAddress));

        if (handler.hasDefined(COMMON_HANDLER_PARAMETER.getName())) {
            for (ModelNode handlerParameter : handler.get(COMMON_HANDLER_PARAMETER.getName()).asList()) {
                Property property = handlerParameter.asProperty();
                String paramName = property.getName();
                String paramValue = HandlerParameterResourceDefinition.VALUE
                                        .resolveModelAttribute(context, property.getValue()).asString();

                KeyValueType kv = new KeyValueType();

                kv.setKey(paramName);
                kv.setValue(paramValue);

                newHandler.add(kv);
            }
        }

        SAMLHandlerService service = new SAMLHandlerService(newHandler);
        PathElement providerAlias = pathAddress.subAddress(0, pathAddress.size() - 1).getLastElement();

        ServiceTarget serviceTarget = context.getServiceTarget();
        ServiceBuilder<SAMLHandlerService> serviceBuilder = serviceTarget.addService(createServiceName(providerAlias.getValue(), newHandler.getClazz()), service);
        ServiceName serviceName;

        if (providerAlias.getKey().equals(IDENTITY_PROVIDER.getName())) {
            serviceName = IdentityProviderService.createServiceName(providerAlias.getValue());
        } else {
            serviceName = ServiceProviderService.createServiceName(providerAlias.getValue());
        }

        serviceBuilder.addDependency(serviceName, EntityProviderService.class, service.getEntityProviderService());

        ServiceController<SAMLHandlerService> controller = serviceBuilder.addListener(verificationHandler).setInitialMode(ServiceController.Mode.PASSIVE).install();

        if (newControllers != null) {
            newControllers.add(controller);
        }
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (SimpleAttributeDefinition attribute : HandlerResourceDefinition.INSTANCE.getAttributes()) {
            attribute.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler,
                                         List<ServiceController<?>> newControllers) throws OperationFailedException {
        PathAddress pathAddress = PathAddress.pathAddress(operation.get(ADDRESS));
        launchServices(context, pathAddress, model, verificationHandler, newControllers);
    }
}