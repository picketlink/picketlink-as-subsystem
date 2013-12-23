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

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.Phase;
import org.picketlink.as.subsystem.deployment.AbstractWeldDeploymentUnitProcessor;
import org.picketlink.as.subsystem.idm.PicketLinkIDMSubsystemExtension;

import static org.picketlink.as.subsystem.PicketLinkLogger.ROOT_LOGGER;
import static org.picketlink.as.subsystem.deployment.PicketLinkStructureDeploymentProcessor.isIDMDeployment;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class PicketLinkIDMDeploymentProcessor extends AbstractWeldDeploymentUnitProcessor {

    public static final Phase PHASE = Phase.POST_MODULE;
    public static final int PRIORITY = Phase.POST_MODULE_WELD_BEAN_ARCHIVE;

    @Override
    public void doDeploy(DeploymentUnit deploymentUnit) throws DeploymentUnitProcessingException {
        if (isIDMDeployment(deploymentUnit)) {
            ROOT_LOGGER.configuringDeployment("PicketLink IDM CDI Extension", deploymentUnit.getName());
            addExtension(deploymentUnit, new PicketLinkIDMSubsystemExtension());
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}
