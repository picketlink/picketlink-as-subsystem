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

import java.util.List;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.picketlink.as.subsystem.idm.service.IdentityManagerFactoryService;
import org.picketlink.as.subsystem.idm.service.IdentityManagerService;
import org.picketlink.as.subsystem.model.AbstractResourceAddStepHandler;
import org.picketlink.as.subsystem.model.ModelElement;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.IdentityManagerFactory;

/**
 * @author Pedro Silva
 *
 */
public abstract class AbstractIdentityStoreAddStepHandler extends AbstractResourceAddStepHandler {

    protected AbstractIdentityStoreAddStepHandler(ModelElement modelElement) {
        super(modelElement);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model,
            ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
            throws OperationFailedException {
        String identityManagementAlias = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS))
                .getElement(1).getValue();

        doPerformRuntime(identityManagementAlias, context, operation, model, verificationHandler, newControllers);
        
        installIdentityManagersForRealms(context, operation, verificationHandler, newControllers, identityManagementAlias);
    }

    private void installIdentityManagersForRealms(OperationContext context, ModelNode operation,
            ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers,
            String identityManagementAlias) {
        ModelNode realmsNode = operation.get(ModelElement.REALMS.getName());

        if (realmsNode.isDefined()) {
            String[] realms = realmsNode.asString().split(",");

            for (String realm : realms) {
                IdentityManagerService identityManagerService = new IdentityManagerService(identityManagementAlias, realm);
                
                ServiceBuilder<IdentityManager> identityManagerServiceBuilder = context.getServiceTarget().addService(
                        IdentityManagerService.createServiceName(identityManagementAlias, realm),
                        identityManagerService);

                identityManagerServiceBuilder.addDependency(IdentityManagerFactoryService
                        .createServiceName(identityManagementAlias), IdentityManagerFactory.class, identityManagerService.getIdentityManagerFactory());

                ServiceController<IdentityManager> identityManagerController = identityManagerServiceBuilder
                        .addListener(verificationHandler).setInitialMode(Mode.PASSIVE).install();

                newControllers.add(identityManagerController);
            }
        }
    }

    protected void doPerformRuntime(String identityManagementAlias, OperationContext context, ModelNode operation, ModelNode model,
            ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
            throws OperationFailedException {
        ServiceController<?> container = (ServiceController<IdentityManagerFactoryService>) context
                .getServiceRegistry(false).getService(IdentityManagerFactoryService.createServiceName(identityManagementAlias));

        IdentityManagerFactoryService identityManagerService = (IdentityManagerFactoryService) container.getService();
        
        identityManagerService.configureStore(operation);
    }

}
