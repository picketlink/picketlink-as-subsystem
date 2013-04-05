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
package org.picketlink.as.subsystem.core.deployment;

import static org.picketlink.as.subsystem.PicketLinkLogger.ROOT_LOGGER;
import static org.picketlink.as.subsystem.deployment.PicketLinkStructureDeploymentProcessor.isCoreDeployment;

import java.util.Iterator;

import javax.enterprise.inject.spi.Extension;

import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.web.deployment.WarMetaData;
import org.jboss.as.weld.WeldDeploymentMarker;
import org.jboss.as.weld.deployment.WeldAttachments;
import org.jboss.metadata.javaee.spec.ResourceReferenceMetaData;
import org.jboss.metadata.javaee.spec.ResourceReferencesMetaData;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.metadata.MetadataImpl;
import org.picketlink.as.subsystem.core.PicketLinkCoreSubsystemExtension;
import org.picketlink.deltaspike.core.api.provider.BeanManagerProvider;
import org.picketlink.deltaspike.security.impl.extension.SecurityExtension;
import org.picketlink.idm.IdentityManager;
import org.picketlink.internal.IdentityStoreAutoConfiguration;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class PicketLinkCoreDeploymentProcessor implements DeploymentUnitProcessor {

    public static final Phase PHASE = Phase.POST_MODULE;

    public static final int PRIORITY = Phase.POST_MODULE_WELD_BEAN_ARCHIVE;

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if (isCoreDeployment(deploymentUnit) && WeldDeploymentMarker.isWeldDeployment(deploymentUnit)) {
            if (deploymentUnit.getParent() != null) {
                deploymentUnit = deploymentUnit.getParent();
            }

            AttachmentList<Metadata<Extension>> extensions = deploymentUnit.getAttachment(WeldAttachments.PORTABLE_EXTENSIONS);

            if (extensions != null) {
                for (Metadata<Extension> e : extensions) {
                    if ((e.getValue() instanceof SecurityExtension) || (e.getValue() instanceof IdentityStoreAutoConfiguration)
                            || (e.getValue() instanceof BeanManagerProvider)) {
                        return;
                    }
                }
            }
            
            WarMetaData warMetadata = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);

            PicketLinkCoreSubsystemExtension coreExtension = null;

            if (warMetadata.getWebMetaData() != null && warMetadata.getWebMetaData().getResourceReferences() != null) {
                ResourceReferencesMetaData resourceReferences = warMetadata.getWebMetaData().getResourceReferences();

                Iterator<ResourceReferenceMetaData> iterator = resourceReferences.iterator();

                while (iterator.hasNext()) {
                    ResourceReferenceMetaData resourceReferenceMetaData = (ResourceReferenceMetaData) iterator.next();

                    if (resourceReferenceMetaData.getType().equals(IdentityManager.class.getName())) {
                        coreExtension = new PicketLinkCoreSubsystemExtension(resourceReferenceMetaData.getName());
                    }
                }
            }

            if (coreExtension == null) {
                coreExtension = new PicketLinkCoreSubsystemExtension();
            }

            ROOT_LOGGER.infov("Enabling PicketLink Core extension for {0}", deploymentUnit.getName());

            addExtensions(deploymentUnit, coreExtension, new SecurityExtension(), new IdentityStoreAutoConfiguration(),
                    new BeanManagerProvider());
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
