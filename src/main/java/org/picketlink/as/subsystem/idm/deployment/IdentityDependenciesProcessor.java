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
package org.picketlink.as.subsystem.idm.deployment;

import static org.picketlink.as.subsystem.PicketLinkLogger.ROOT_LOGGER;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.picketlink.as.subsystem.deployment.IdentityMarkerProcessor;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class IdentityDependenciesProcessor implements DeploymentUnitProcessor {

    public static final Phase PHASE = Phase.DEPENDENCIES;

    public static final int PRIORITY = 0x4000;

    public static final ModuleIdentifier PICKETLINK = ModuleIdentifier.create("org.picketlink");

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if (deploymentUnit.getAttachment(IdentityMarkerProcessor.ENABLE_IDENTITY_KEY) == null) {
            return;
        }

        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = deploymentUnit.getAttachment(Attachments.SERVICE_MODULE_LOADER);

        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, PICKETLINK, false, true, false, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, ModuleIdentifier.create("org.picketlink.idm"), false, true, false, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, ModuleIdentifier.create("org.picketlink.core"), false, true, false, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, ModuleIdentifier.create("org.picketlink.federation"), false, false, true, false));

        ROOT_LOGGER.infov("Added picketlink dependencies to {0}", deploymentUnit.getName());
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

}
