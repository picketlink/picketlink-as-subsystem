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

import static org.picketlink.as.subsystem.deployment.PicketLinkAttachments.CORE_ATTACHMENT_KEY;
import static org.picketlink.as.subsystem.deployment.PicketLinkAttachments.IDM_ATTACHMENT_KEY;
import static org.picketlink.as.subsystem.deployment.PicketLinkModuleIdentifiers.ORG_PICKETLINK_CORE_MODULE;
import static org.picketlink.as.subsystem.deployment.PicketLinkModuleIdentifiers.ORG_PICKETLINK_IDM_MODULE;

/**
 * <p>{@link DeploymentUnitProcessor} that marks PicketLink deployments according with structure of the deployment.</p>
 *
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class PicketLinkStructureDeploymentProcessor implements DeploymentUnitProcessor {

    public static final Phase PHASE = Phase.STRUCTURE;
    public static final int PRIORITY = 0x3000;

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        markCoreDeployment(deploymentUnit);
        markIDMDeployment(deploymentUnit);
    }

    private void markIDMDeployment(DeploymentUnit deployment) {
        markDeploymentWithModuleDependency(ORG_PICKETLINK_IDM_MODULE, IDM_ATTACHMENT_KEY, deployment);
    }
    
    private void markCoreDeployment(DeploymentUnit deployment) {
        markDeploymentWithModuleDependency(ORG_PICKETLINK_CORE_MODULE, CORE_ATTACHMENT_KEY, deployment);
    }
    

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void markDeploymentWithModuleDependency(ModuleIdentifier moduleIdentifier, AttachmentKey attachment, DeploymentUnit deployment) {
        ModuleSpecification moduleSpecification = deployment.getAttachment(Attachments.MODULE_SPECIFICATION);

        for (ModuleDependency deploymentDependency : moduleSpecification.getUserDependencies()) {
            if (deploymentDependency.getIdentifier().equals(moduleIdentifier)) {
                deployment.putAttachment(attachment, Boolean.TRUE);
                return;
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
    
    public static boolean isIDMDeployment(DeploymentUnit deployment) {
        return deployment.getAttachment(IDM_ATTACHMENT_KEY) != null;
    }

    public static boolean isCoreDeployment(DeploymentUnit deployment) {
        return deployment.getAttachment(CORE_ATTACHMENT_KEY) != null;
    }

}