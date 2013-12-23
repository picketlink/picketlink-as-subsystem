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

package org.picketlink.as.subsystem.federation.service;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.service.ServiceName;
import org.picketlink.identity.federation.core.config.IDPConfiguration;
import org.picketlink.identity.federation.core.config.SPConfiguration;

import static org.picketlink.as.subsystem.PicketLinkMessages.MESSAGES;

/**
 * <p> Service implementation to enable a deployed applications as a Service Provider. </p>
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class ServiceProviderService extends EntityProviderService<ServiceProviderService, SPConfiguration> {

    private static final String SERVICE_NAME = "SPConfigurationService";

    public ServiceProviderService(SPConfiguration configuration) {
        super(configuration);
    }

    public static ServiceName createServiceName(String alias) {
        return ServiceName.JBOSS.append(SERVICE_NAME, alias);
    }

    @Override
    public void doConfigureDeployment(DeploymentUnit deploymentUnit) {
        configureBindingType();
        configureIdentityProvider();
    }

    private void configureIdentityProvider() {
        IDPConfiguration idpConfiguration = getFederationService().getValue().getIdpConfiguration();

        if (idpConfiguration == null) {
            throw MESSAGES.federationIdentityProviderNotConfigured(getFederationService().getValue().getAlias());
        }

        getConfiguration().setIdentityURL(idpConfiguration.getIdentityURL());
    }

    private void configureBindingType() {
        if (getConfiguration().isPostBinding()) {
            getConfiguration().setBindingType("POST");
        } else {
            getConfiguration().setBindingType("REDIRECT");
        }
    }
}
