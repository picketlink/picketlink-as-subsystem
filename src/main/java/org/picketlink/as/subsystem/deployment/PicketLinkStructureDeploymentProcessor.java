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
package org.picketlink.as.subsystem.deployment;

import static org.picketlink.as.subsystem.deployment.PicketLinkAttachments.CORE_ATTACHMENT_KEY;
import static org.picketlink.as.subsystem.deployment.PicketLinkAttachments.FEDERATION_ATTACHMENT_KEY;
import static org.picketlink.as.subsystem.deployment.PicketLinkAttachments.IDM_ATTACHMENT_KEY;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;
import org.picketlink.as.subsystem.federation.service.IdentityProviderService;
import org.picketlink.as.subsystem.federation.service.PicketLinkFederationService;
import org.picketlink.as.subsystem.federation.service.ServiceProviderService;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class PicketLinkStructureDeploymentProcessor implements DeploymentUnitProcessor {

    public static final ModuleIdentifier IDM_MODULE_IDENTIFIER = ModuleIdentifier.create("org.picketlink.idm");
    public static final ModuleIdentifier CORE_MODULE_IDENTIFIER = ModuleIdentifier.create("org.picketlink.core");

    public static final Phase PHASE = Phase.STRUCTURE;

    public static final int PRIORITY = 0x3000;

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        markFederationDeployment(phaseContext);
        markCoreDeployment(deploymentUnit);
        markIDMDeployment(deploymentUnit);
    }

    private void markFederationDeployment(DeploymentPhaseContext phaseContext) {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        ServiceRegistry serviceRegistry = phaseContext.getServiceRegistry();

        ServiceController<?> federationService = serviceRegistry.getService(IdentityProviderService
                .createServiceName(deploymentUnit.getName()));

        if (federationService == null) {
            federationService = serviceRegistry.getService(ServiceProviderService.createServiceName(deploymentUnit.getName()));
        }
        
        if (federationService != null) {
            deploymentUnit.putAttachment(FEDERATION_ATTACHMENT_KEY, (PicketLinkFederationService<?>) federationService.getValue());
        }
    }

    private void markIDMDeployment(DeploymentUnit deploymentUnit) {
        markDeploymentWithModuleDependency(IDM_MODULE_IDENTIFIER, IDM_ATTACHMENT_KEY, deploymentUnit);
    }
    
    private void markCoreDeployment(DeploymentUnit deploymentUnit) {
        markDeploymentWithModuleDependency(CORE_MODULE_IDENTIFIER, CORE_ATTACHMENT_KEY, deploymentUnit);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void markDeploymentWithModuleDependency(ModuleIdentifier moduleDependencyIdentifier, AttachmentKey attachment, DeploymentUnit deploymentUnit) {
        ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);

        for (ModuleDependency d : moduleSpecification.getUserDependencies()) {
            if (d.getIdentifier().equals(moduleDependencyIdentifier)) {
                deploymentUnit.putAttachment(attachment, Boolean.TRUE);
                return;
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
    
    public static boolean isFederationDeployment(DeploymentUnit deploymentUnit) {
        return deploymentUnit.getAttachment(FEDERATION_ATTACHMENT_KEY) != null;
    }
    
    public static boolean isIDMDeployment(DeploymentUnit deploymentUnit) {
        return deploymentUnit.getAttachment(IDM_ATTACHMENT_KEY) != null;
    }

    public static boolean isCoreDeployment(DeploymentUnit deploymentUnit) {
        return deploymentUnit.getAttachment(CORE_ATTACHMENT_KEY) != null;
    }

}