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
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.picketlink.as.subsystem.idm.service.IdentityManagerFactoryService;
import org.picketlink.as.subsystem.model.AbstractResourceAddStepHandler;
import org.picketlink.as.subsystem.model.ModelElement;
import org.picketlink.idm.IdentityManagerFactory;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class IdentityManagementAddHandler extends AbstractResourceAddStepHandler {

    public static final IdentityManagementAddHandler INSTANCE = new IdentityManagementAddHandler();

    private IdentityManagementAddHandler() {
        super(ModelElement.IDENTITY_MANAGEMENT);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model,
            final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers)
            throws OperationFailedException {
        final String alias = operation.get(ModelElement.COMMON_ALIAS.getName()).asString();

        final ServiceBuilder<IdentityManagerFactory> serviceBuilder = context.getServiceTarget().addService(
                IdentityManagerFactoryService.createServiceName(alias), new IdentityManagerFactoryService(operation));
        Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);

        Resource jpaStoreResource = resource.getChild(PathElement.pathElement(ModelElement.JPA_STORE.getName(),
                ModelElement.JPA_STORE.getName()));

        if (jpaStoreResource != null) {
            ModelNode dataSourceNode = jpaStoreResource.getModel().get(ModelElement.JPA_STORE_DATASOURCE.getName());
            ModelNode emfSourceNode = jpaStoreResource.getModel().get(ModelElement.JPA_STORE_ENTITY_MANAGER_FACTORY.getName());

            if (dataSourceNode.isDefined()) {
                serviceBuilder.addDependency(ContextNames.JAVA_CONTEXT_SERVICE_NAME
                        .append(dataSourceNode.asString().split("/")));
            }

            if (emfSourceNode.isDefined()) {
                serviceBuilder
                        .addDependency(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(emfSourceNode.asString().split("/")));
            }
        }

        final ServiceController<IdentityManagerFactory> controller = serviceBuilder.addListener(verificationHandler)
                .setInitialMode(Mode.NEVER).install();

        newControllers.add(controller);
        
        // This needs to run after all child resources so that they can detect a fresh state
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                controller.setMode(Mode.PASSIVE);
                context.stepCompleted();
            }
        }, OperationContext.Stage.RUNTIME);
    }

}