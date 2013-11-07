package org.picketlink.as.subsystem.deployment;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.deployment.WeldPortableExtensions;
import org.jboss.weld.bootstrap.spi.Metadata;

import javax.enterprise.inject.spi.Extension;

import static org.jboss.as.ee.weld.WeldDeploymentMarker.isWeldDeployment;

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
            getExtensions(deployment).registerExtensionInstance(extension, deployment);
        }
    }

    protected WeldPortableExtensions getExtensions(DeploymentUnit deployment) {
        return WeldPortableExtensions.getPortableExtensions(deployment);
    }

    protected boolean hasExtension(DeploymentUnit deployment, Class<? extends Extension>... extensions) {
        for (Class<? extends Extension> extension : extensions) {
            WeldPortableExtensions deploymentExtensions = getExtensions(deployment);

            if (deploymentExtensions != null) {
                for (Metadata<Extension> e : deploymentExtensions.getExtensions()) {
                    if (extension.equals(e.getValue().getClass())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

}
