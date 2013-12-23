package org.picketlink.as.subsystem;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.picketlink.as.subsystem.core.deployment.PicketLinkCoreDeploymentProcessor;
import org.picketlink.as.subsystem.deployment.PicketLinkDependencyDeploymentProcessor;
import org.picketlink.as.subsystem.deployment.PicketLinkStructureDeploymentProcessor;
import org.picketlink.as.subsystem.federation.deployment.FederationDeploymentProcessor;
import org.picketlink.as.subsystem.idm.deployment.PicketLinkIDMDeploymentProcessor;

import java.util.List;

import static org.picketlink.as.subsystem.PicketLinkLogger.ROOT_LOGGER;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class PicketLinkSubsystemAdd extends AbstractBoottimeAddStepHandler {

    public static final PicketLinkSubsystemAdd INSTANCE = new PicketLinkSubsystemAdd();

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
    }

    @Override
    public void performBoottime(OperationContext context, ModelNode operation, ModelNode model,
                                       ServiceVerificationHandler verificationHandler, List<ServiceController<?>> controllers)
            throws OperationFailedException {

        PicketLinkLogger.ROOT_LOGGER.activatingSubsystem();

        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            public void execute(DeploymentProcessorTarget processorTarget) {
                ROOT_LOGGER.trace("Installing the PicketLink Marker deployment processor.");
                processorTarget.addDeploymentProcessor(PicketLinkExtension.SUBSYSTEM_NAME,
                                                              PicketLinkStructureDeploymentProcessor.PHASE, PicketLinkStructureDeploymentProcessor.PRIORITY,
                                                              new PicketLinkStructureDeploymentProcessor());
                ROOT_LOGGER.trace("Installing the PicketLink Dependency deployment processor.");
                processorTarget.addDeploymentProcessor(PicketLinkExtension.SUBSYSTEM_NAME,
                                                              PicketLinkDependencyDeploymentProcessor.PHASE, PicketLinkDependencyDeploymentProcessor.PRIORITY,
                                                              new PicketLinkDependencyDeploymentProcessor());
                ROOT_LOGGER.trace("Installing the PicketLink Identity Provider deployment processor.");
                processorTarget.addDeploymentProcessor(PicketLinkExtension.SUBSYSTEM_NAME, FederationDeploymentProcessor.PHASE,
                                                              FederationDeploymentProcessor.PRIORITY, new FederationDeploymentProcessor());
                ROOT_LOGGER.trace("Installing the PicketLink IDM deployment processor.");
                processorTarget.addDeploymentProcessor(PicketLinkExtension.SUBSYSTEM_NAME,
                                                              PicketLinkIDMDeploymentProcessor.PHASE, PicketLinkIDMDeploymentProcessor.PRIORITY,
                                                              new PicketLinkIDMDeploymentProcessor());
                ROOT_LOGGER.trace("Installing the PicketLink Core deployment processor.");
                processorTarget.addDeploymentProcessor(PicketLinkExtension.SUBSYSTEM_NAME,
                                                              PicketLinkCoreDeploymentProcessor.PHASE, PicketLinkCoreDeploymentProcessor.PRIORITY,
                                                              new PicketLinkCoreDeploymentProcessor());
            }
        }, OperationContext.Stage.RUNTIME);
    }
}
