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
import static org.picketlink.as.subsystem.deployment.PicketLinkStructureDeploymentProcessor.isCoreDeployment;

import javax.enterprise.inject.spi.Extension;

import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.weld.deployment.WeldAttachments;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.metadata.MetadataImpl;
import org.picketlink.as.subsystem.idm.cdi.PicketLinkCdiExtension;
import org.picketlink.deltaspike.core.api.provider.BeanManagerProvider;
import org.picketlink.deltaspike.security.impl.extension.SecurityExtension;
import org.picketlink.permission.internal.JPAPermissionStoreConfig;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class IdentityCdiExtensionInstallerProcessor implements DeploymentUnitProcessor {

    public static final Phase PHASE = Phase.PARSE;

    public static final int PRIORITY = Phase.PARSE_WEB_COMPONENTS - 1;

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if (isCoreDeployment(deploymentUnit)) {
            if (deploymentUnit.getParent() != null) {
                deploymentUnit = deploymentUnit.getParent();
            }

            AttachmentList<Metadata<Extension>> extensions = deploymentUnit.getAttachment(WeldAttachments.PORTABLE_EXTENSIONS);
            
            if (extensions != null) {
                for (Metadata<Extension> e : extensions) {
                    if (e.getValue() instanceof PicketLinkCdiExtension) {
                        return;
                    }
                }
            }

            ROOT_LOGGER.infov("Enabling identity extension for {0}", deploymentUnit.getName());

            addExtensions(deploymentUnit, new PicketLinkCdiExtension(), new JPAPermissionStoreConfig(),
                    new SecurityExtension(), new BeanManagerProvider());

            // Don't install JPAIdentityStoreAutoConfig as it doesn't work from a module
        }
    }

    private void addExtensions(DeploymentUnit du, Extension... extensions) {
        for (Extension e : extensions) {
            Metadata<Extension> metadata = new MetadataImpl(e, du.getName());
            du.addToAttachmentList(WeldAttachments.PORTABLE_EXTENSIONS, metadata);
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

}
