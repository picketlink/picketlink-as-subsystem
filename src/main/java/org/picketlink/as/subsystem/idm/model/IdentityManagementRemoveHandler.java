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
import java.util.Set;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.picketlink.as.subsystem.idm.service.IdentityManagerFactoryService;
import org.picketlink.as.subsystem.idm.service.IdentityManagerService;
import org.picketlink.as.subsystem.model.ModelElement;
import org.picketlink.idm.config.IdentityStoreConfiguration;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class IdentityManagementRemoveHandler extends AbstractRemoveStepHandler {

    public static final IdentityManagementRemoveHandler INSTANCE = new IdentityManagementRemoveHandler();

    private IdentityManagementRemoveHandler() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.as.controller.AbstractRemoveStepHandler#performRuntime(org.jboss.as.controller.OperationContext,
     * org.jboss.dmr.ModelNode, org.jboss.dmr.ModelNode)
     */
    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model)
            throws OperationFailedException {
        final String alias = operation.get(ModelElement.COMMON_ALIAS.getName()).asString();

        ModelNode jndiNameNode = operation.get(ModelElement.IDENTITY_MANAGEMENT_JNDI_NAME.getName());
        String jndiName = null;

        if (jndiNameNode.isDefined()) {
            jndiName = jndiNameNode.asString();
        }

        context.removeService(ContextNames.buildServiceName(ContextNames.JAVA_CONTEXT_SERVICE_NAME, jndiName));
        ServiceController<?> service = (ServiceController<IdentityManagerFactoryService>) context.removeService(IdentityManagerFactoryService.createServiceName(alias));
        
        IdentityManagerFactoryService identityManagerService = (IdentityManagerFactoryService) service.getService();
        
        List<IdentityStoreConfiguration> stores = identityManagerService.getIdentityConfiguration().getConfiguredStores();
        
        for (IdentityStoreConfiguration identityStoreConfiguration : stores) {
            Set<String> realms = identityStoreConfiguration.getRealms();
            
            for (String realm : realms) {
                context.removeService(IdentityManagerService.createServiceName(alias, realm));
            }
        }
    }

}
