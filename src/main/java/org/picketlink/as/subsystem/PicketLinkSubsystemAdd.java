package org.picketlink.as.subsystem;

import static org.picketlink.as.subsystem.PicketLinkLogger.ROOT_LOGGER;

import java.util.List;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.ImmediateValue;
import org.picketlink.as.subsystem.deployment.IdentityMarkerProcessor;
import org.picketlink.as.subsystem.federation.deployment.IdentityProviderDeploymentProcessor;
import org.picketlink.as.subsystem.federation.deployment.PicketLinkDependencyDeploymentProcessor;
import org.picketlink.as.subsystem.federation.deployment.ServiceProviderDeploymentProcessor;
import org.picketlink.as.subsystem.idm.deployment.IdentityCdiExtensionInstallerProcessor;
import org.picketlink.as.subsystem.idm.deployment.IdentityDependenciesProcessor;
import org.picketlink.as.subsystem.idm.service.IdentityManagerService;
import org.picketlink.idm.IdentityManager;

/**
 * <p>
 * Handler responsible for adding the subsystem resource to the model and install the PicketLink deployment unit processors.
 * </p>
 * 
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 16, 2012
 */
public class PicketLinkSubsystemAdd extends AbstractBoottimeAddStepHandler {

    public static final PicketLinkSubsystemAdd INSTANCE = new PicketLinkSubsystemAdd();

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.as.controller.AbstractAddStepHandler#populateModel(org.jboss.dmr.ModelNode, org.jboss.dmr.ModelNode)
     */
    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
    }

    /* (non-Javadoc)
     * @see org.jboss.as.controller.AbstractBoottimeAddStepHandler#performBoottime(org.jboss.as.controller.OperationContext, org.jboss.dmr.ModelNode, org.jboss.dmr.ModelNode, org.jboss.as.controller.ServiceVerificationHandler, java.util.List)
     */
    @Override
    public void performBoottime(OperationContext context, ModelNode operation, ModelNode model,
            ServiceVerificationHandler verificationHandler, List<ServiceController<?>> controllers)
            throws OperationFailedException {

        PicketLinkLogger.ROOT_LOGGER.activatingSubsystem();
        
        final ServiceTarget target = context.getServiceTarget();
        
        final ServiceController<IdentityManager> controller = IdentityManagerService.addService(target, verificationHandler);
        
        controllers.add(controller);
        
        context.addStep(new AbstractDeploymentChainStep() {
            public void execute(DeploymentProcessorTarget processorTarget) {
                ROOT_LOGGER.trace("Installing the PicketLink Dependency deployment processor.");
                processorTarget.addDeploymentProcessor(PicketLinkExtension.SUBSYSTEM_NAME, PicketLinkDependencyDeploymentProcessor.PHASE, PicketLinkDependencyDeploymentProcessor.PRIORITY,
                        new PicketLinkDependencyDeploymentProcessor());
                ROOT_LOGGER.trace("Installing the PicketLink Identity Provider deployment processor.");
                processorTarget.addDeploymentProcessor(PicketLinkExtension.SUBSYSTEM_NAME, IdentityProviderDeploymentProcessor.PHASE,
                        IdentityProviderDeploymentProcessor.PRIORITY, new IdentityProviderDeploymentProcessor());
                ROOT_LOGGER.trace("Installing the PicketLink Service Provider deployment processor.");
                processorTarget.addDeploymentProcessor(PicketLinkExtension.SUBSYSTEM_NAME, ServiceProviderDeploymentProcessor.PHASE,
                        ServiceProviderDeploymentProcessor.PRIORITY, new ServiceProviderDeploymentProcessor());
                
                processorTarget.addDeploymentProcessor(PicketLinkExtension.SUBSYSTEM_NAME,
                        IdentityMarkerProcessor.PHASE, IdentityMarkerProcessor.PRIORITY, new IdentityMarkerProcessor());
                processorTarget.addDeploymentProcessor(PicketLinkExtension.SUBSYSTEM_NAME,
                        IdentityDependenciesProcessor.PHASE, IdentityDependenciesProcessor.PRIORITY,
                        new IdentityDependenciesProcessor());
                processorTarget.addDeploymentProcessor(PicketLinkExtension.SUBSYSTEM_NAME,
                        IdentityCdiExtensionInstallerProcessor.PHASE, IdentityCdiExtensionInstallerProcessor.PRIORITY,
                        new IdentityCdiExtensionInstallerProcessor());
            }
        }, OperationContext.Stage.RUNTIME);

        final BinderService binderService = new BinderService("IdentityService");
        final ServiceBuilder<ManagedReferenceFactory> builder = context.getServiceTarget().addService(
                IdentityManagerService.JNDI_SERVICE_NAME, binderService);
        builder.addDependency(ContextNames.JBOSS_CONTEXT_SERVICE_NAME, ServiceBasedNamingStore.class,
                binderService.getNamingStoreInjector());
        builder.addDependency(IdentityManagerService.SERVICE_NAME, IdentityManager.class, new Injector<IdentityManager>() {
            @Override
            public void inject(final IdentityManager value) throws InjectionException {
                binderService.getManagedObjectInjector().inject(
                        new ValueManagedReferenceFactory(new ImmediateValue<Object>(value)));
            }

            @Override
            public void uninject() {
                binderService.getManagedObjectInjector().uninject();
            }
        });

        builder.addListener(verificationHandler);

        controllers.add(builder.install());
    }

}