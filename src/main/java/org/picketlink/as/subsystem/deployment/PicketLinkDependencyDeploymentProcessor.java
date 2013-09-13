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

import static org.picketlink.as.subsystem.PicketLinkLogger.ROOT_LOGGER;
import static org.picketlink.as.subsystem.deployment.PicketLinkStructureDeploymentProcessor.isCoreDeployment;
import static org.picketlink.as.subsystem.deployment.PicketLinkStructureDeploymentProcessor.isFederationDeployment;
import static org.picketlink.as.subsystem.deployment.PicketLinkStructureDeploymentProcessor.isIDMDeployment;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.filter.PathFilters;

/**
 * <p>
 * Specific {@link DeploymentUnitProcessor} to automatically configure the PicketLink module dependencies.
 * </p>
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 9, 2012
 */
public class PicketLinkDependencyDeploymentProcessor implements DeploymentUnitProcessor {

    public static final Phase PHASE = Phase.DEPENDENCIES;

    public static final int PRIORITY = 1;

    /*
     * (non-Javadoc)
     *
     * @see org.jboss.as.server.deployment.DeploymentUnitProcessor#deploy(org.jboss.as.server.deployment.DeploymentPhaseContext)
     */
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if (isFederationDeployment(deploymentUnit)) {
            configureFederationDependencies(deploymentUnit);
        }

        if (isCoreDeployment(deploymentUnit)) {
            configureCoreDependencies(deploymentUnit);
        }

        if (isIDMDeployment(deploymentUnit)) {
            configureIDMDependencies(deploymentUnit);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.jboss.as.server.deployment.DeploymentUnitProcessor#undeploy(org.jboss.as.server.deployment.DeploymentUnit)
     */
    @Override
    public void undeploy(DeploymentUnit context) {

    }

    /**
     * <p>
     * Add the PicketLink Federation dependencies to the {@link DeploymentUnit}.
     * </p>
     *
     * @param deploymentUnit
     */
    private void configureFederationDependencies(DeploymentUnit deploymentUnit) {
        addDependency(deploymentUnit, ModuleIdentifier.create("org.picketlink"));
        ROOT_LOGGER.configuringDeployment("PicketLink Federation Dependencies", deploymentUnit.getName());
    }

    /**
     * <p>
     * Add the PicketLink IDM dependencies to the {@link DeploymentUnit}.
     * </p>
     *
     * @param deploymentUnit
     */
    private void configureIDMDependencies(DeploymentUnit deploymentUnit) {
        addDependency(deploymentUnit, ModuleIdentifier.create("org.picketlink.idm.api"));
        addDependency(deploymentUnit, ModuleIdentifier.create("org.picketlink.idm"));
        ROOT_LOGGER.configuringDeployment("PicketLink IDM Dependencies", deploymentUnit.getName());
    }

    /**
     * <p>
     * Add the PicketLink Core dependencies to the {@link DeploymentUnit}.
     * </p>
     *
     * @param deploymentUnit
     */
    private void configureCoreDependencies(DeploymentUnit deploymentUnit) {
        addDependency(deploymentUnit, ModuleIdentifier.create("org.picketlink.core.api"));
        addDependency(deploymentUnit, ModuleIdentifier.create("org.picketlink.core"));
        ROOT_LOGGER.configuringDeployment("PicketLink Core Dependencies", deploymentUnit.getName());
    }

    private void addDependency(DeploymentUnit deploymentUnit, ModuleIdentifier moduleIdentifier) {
        final ModuleSpecification moduleSpec = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = deploymentUnit.getAttachment(Attachments.SERVICE_MODULE_LOADER);

        ModuleDependency dependency = new ModuleDependency(moduleLoader, moduleIdentifier, false, true, true, false);

        dependency.addImportFilter(PathFilters.getMetaInfFilter(), true);

        moduleSpec.addSystemDependency(dependency);
    }
}