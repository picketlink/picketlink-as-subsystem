package org.picketlink.as.subsystem.deployment;

import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.metadata.MetadataImpl;

import javax.enterprise.inject.spi.Extension;

import static org.jboss.as.weld.WeldDeploymentMarker.isWeldDeployment;
import static org.jboss.as.weld.deployment.WeldAttachments.PORTABLE_EXTENSIONS;
import static org.picketlink.as.subsystem.PicketLinkMessages.MESSAGES;

/**
 * @author Pedro Igor
 */
public abstract class AbstractWeldDeploymentUnitProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deployment = phaseContext.getDeploymentUnit();

        if (isWeldDeployment(deployment)) {
            if (deployment.getParent() != null) {
                deployment = deployment.getParent();
            }

            try {
                doDeploy(deployment);
            } catch (Exception e) {
                throw MESSAGES.deploymentConfigurationFailed(deployment.getName(), e);
            }
        }
    }

    protected abstract void doDeploy(DeploymentUnit deploymentUnit) throws Exception;

    @SuppressWarnings("unchecked")
    protected void addExtension(DeploymentUnit deployment, Extension extension) {
        if (!hasExtension(deployment, extension.getClass())) {
            Metadata<Extension> metadata = new MetadataImpl<Extension>(extension, deployment.getName());
            deployment.addToAttachmentList(PORTABLE_EXTENSIONS, metadata);
        }
    }

    private boolean hasExtension(DeploymentUnit deployment, Class<? extends Extension>... extensions) {
        for (Class<? extends Extension> extension : extensions) {
            AttachmentList<Metadata<Extension>> deploymentExtensions = deployment.getAttachment(PORTABLE_EXTENSIONS);

            if (deploymentExtensions != null) {
                for (Metadata<Extension> deployedExtension : deploymentExtensions) {
                    if (extension.equals(deployedExtension.getValue().getClass())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
