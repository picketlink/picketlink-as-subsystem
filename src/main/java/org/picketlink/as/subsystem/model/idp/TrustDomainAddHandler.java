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

package org.picketlink.as.subsystem.model.idp;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.picketlink.as.subsystem.model.ModelElement;
import org.picketlink.as.subsystem.service.IDPConfigurationService;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class TrustDomainAddHandler extends AbstractAddStepHandler {

    public static final TrustDomainAddHandler INSTANCE = new TrustDomainAddHandler();

    private TrustDomainAddHandler() {
    }

    /* (non-Javadoc)
     * @see org.jboss.as.controller.AbstractAddStepHandler#populateModel(org.jboss.dmr.ModelNode, org.jboss.dmr.ModelNode)
     */
    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        TrustDomainResourceDefinition.NAME.validateAndSet(operation, model);
    }
    
    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model,
            ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
            throws OperationFailedException {
        String alias = operation.get(ModelDescriptionConstants.ADDRESS).asPropertyList().get(2).getValue().asString();
        String domain = operation.get(ModelElement.TRUST_DOMAIN_NAME.getName()).asString();

        IDPConfigurationService service = IDPConfigurationService.getService(context.getServiceRegistry(true), alias);
        
        service.getIdpConfiguration().addTrustDomain(domain);
    }
    
}
