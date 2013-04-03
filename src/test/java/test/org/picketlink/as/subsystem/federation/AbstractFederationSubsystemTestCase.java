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

package test.org.picketlink.as.subsystem.federation;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.picketlink.as.subsystem.PicketLinkExtension;
import org.picketlink.as.subsystem.federation.service.FederationService;
import org.picketlink.as.subsystem.federation.service.IdentityProviderService;
import org.picketlink.as.subsystem.federation.service.ServiceProviderService;
import org.picketlink.as.subsystem.model.ModelElement;

import test.org.picketlink.as.subsystem.AbstractPicketLinkSubsystemTestCase;

/**
 * @author Pedro Silva
 *
 */
public class AbstractFederationSubsystemTestCase extends AbstractPicketLinkSubsystemTestCase {

    /**
     * <p>
     * Returns a {@link ModelNode} instance for the federation configuration being tested.
     * </p>
     * 
     * @param model
     * @return
     */
    protected ModelNode getFederationModel() {
        ModelNode federationNode = getResultingModelNode().get(ModelDescriptionConstants.SUBSYSTEM, PicketLinkExtension.SUBSYSTEM_NAME,
                ModelElement.FEDERATION.getName(), getFederationAliasToTest());
        
        return federationNode;
    }

    /**
     * <p>Subclasses can override this method to specify the federation alias to test. Check the picketlink-subsystem.xml.</p>
     * 
     * @return
     */
    protected String getFederationAliasToTest() {
        return "federation-with-signatures";
    }

    protected IdentityProviderService getIdentityProviderService() {
        ServiceName serviceName = IdentityProviderService.createServiceName(getIdentityProvider().asProperty().getName());

        return (IdentityProviderService) getInstalledService(serviceName).getValue();
    }

    protected FederationService getFederationService() {
        ServiceName serviceName = FederationService.createServiceName(getFederationModel().get(ModelElement.COMMON_ALIAS.getName()).asString());

        return (FederationService) getInstalledService(serviceName).getValue();
    }

    /**
     * <p>
     * Returns a {@link ModelNode} instance for the configured Identity Provider.
     * </p>
     * 
     * @return
     */
    protected ModelNode getIdentityProvider() {
        return getFederationModel().get(ModelElement.IDENTITY_PROVIDER.getName());
    }
    
    /**
     * <p>
     * Returns a {@link ServiceProviderService} instance for the given alias.
     * </p>
     * 
     * @throws Exception
     */
    protected ServiceProviderService getServiceProviderService(String alias) throws Exception {
        return (ServiceProviderService) getInstalledService(ServiceProviderService.createServiceName(alias)).getValue();
    }
}
