package org.picketlink.as.subsystem.core.deployment;

import org.jboss.as.naming.deployment.ContextNames;
import org.picketlink.annotations.PicketLink;
import org.picketlink.as.subsystem.core.PicketLinkCoreSubsystemExtension;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.RelationshipManager;
import org.picketlink.idm.model.Partition;
import org.picketlink.internal.SecuredIdentityManager;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * @author pedroigor
 */
@ApplicationScoped
public class PicketLinkSubsystemIdentityManagerProducer {

    @Inject
    @PicketLink
    private Instance<Partition> defaultPartition;

    private PartitionManager partitionManager;

    @Inject
    private PicketLinkCoreSubsystemExtension extension;

    @Inject
    public void init() {
        String partitionManagerJNDIUrl = this.extension.getPartitionManagerJNDIUrl();

        try {
            String formattedJNDIName = partitionManagerJNDIUrl.replaceAll(ContextNames.JAVA_CONTEXT_SERVICE_NAME.getSimpleName() + ":", "");

            this.partitionManager = (PartitionManager) new InitialContext().lookup(formattedJNDIName);
        } catch (NamingException e) {
            throw new RuntimeException("Error looking up IdentityManager from [" + partitionManagerJNDIUrl + "]", e);
        }
    }

    @Produces
    public PartitionManager createPartitionManager() {
        return partitionManager;
    }

    @Produces
    @RequestScoped
    public IdentityManager createIdentityManager() {
        if (defaultPartition.isUnsatisfied() || defaultPartition.get() == null) {
            return new SecuredIdentityManager(this.partitionManager.createIdentityManager());
        } else {
            return new SecuredIdentityManager(this.partitionManager.createIdentityManager(defaultPartition.get()));
        }
    }

    @Produces
    @RequestScoped
    public RelationshipManager createRelationshipManager() {
        return this.partitionManager.createRelationshipManager();
    }

}
