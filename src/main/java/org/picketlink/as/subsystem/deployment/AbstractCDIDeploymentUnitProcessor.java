package org.picketlink.as.subsystem.deployment;

import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.metadata.MetadataImpl;

import javax.enterprise.inject.spi.Extension;

import static org.jboss.as.weld.WeldDeploymentMarker.*;
import static org.jboss.as.weld.deployment.WeldAttachments.*;

/**
 * @author Pedro Igor
 */
public abstract class AbstractCDIDeploymentUnitProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deployment = phaseContext.getDeploymentUnit();

        if (isWeldDeployment(deployment)) {
            if (!isAlreadyConfigured(deployment)) {
                doDeploy(phaseContext);
            }
        }
    }

    protected abstract void doDeploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException;

    protected abstract boolean isAlreadyConfigured(DeploymentUnit deployment);

    protected void addExtension(DeploymentUnit deployment, Extension extension) {
        if (!hasExtension(deployment, extension.getClass())) {
            Metadata<Extension> metadata = new MetadataImpl<Extension>(extension, deployment.getName());
            deployment.addToAttachmentList(PORTABLE_EXTENSIONS, metadata);
        }
    }

    protected AttachmentList<Metadata<Extension>> getExtensions(DeploymentUnit deployment) {
        return deployment.getAttachment(PORTABLE_EXTENSIONS);
    }

    protected boolean hasExtension(DeploymentUnit deployment, Class<? extends Extension>... extensions) {
        for (Class<? extends Extension> extension : extensions) {
            AttachmentList<Metadata<Extension>> deploymentExtensions = getExtensions(deployment);

            if (deploymentExtensions != null) {
                for (Metadata<Extension> e : deploymentExtensions) {
                    if (extension.equals(e.getValue().getClass())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

}
