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

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.as.controller.AbstractAddStepHandler#performRuntime(org.jboss.as.controller.OperationContext,
     * org.jboss.dmr.ModelNode, org.jboss.dmr.ModelNode, org.jboss.as.controller.ServiceVerificationHandler, java.util.List)
     */
    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model,
            ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
            throws OperationFailedException {
        String alias = operation.get(ModelElement.COMMON_ALIAS.getName()).asString();

        ServiceBuilder<IdentityManagerFactory> serviceBuilder = context.getServiceTarget().addService(
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

        ServiceController<IdentityManagerFactory> controller = serviceBuilder.addListener(verificationHandler)
                .setInitialMode(Mode.PASSIVE).install();

        newControllers.add(controller);
    }

}
