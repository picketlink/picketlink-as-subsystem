package org.picketlink.as.subsystem.idm;

import org.picketlink.annotations.PicketLink;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.config.IdentityConfiguration;
import org.picketlink.idm.internal.DefaultPartitionManager;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static org.picketlink.as.subsystem.PicketLinkMessages.MESSAGES;

/**
 * @author Pedro Igor
 */
@ApplicationScoped
public class PartitionManagerProducer {

    @Inject
    private Instance<IdentityConfiguration> configurationInstance;
    @Inject
    @PicketLink
    private Instance<PartitionManager> partitionManagerInstance;
    private PartitionManager partitionManager;

    @Inject
    public void init() {
        if (!this.partitionManagerInstance.isUnsatisfied()) {
            this.partitionManager = this.partitionManagerInstance.get();
        } else {
            if (this.configurationInstance.isUnsatisfied()) {
                throw MESSAGES.idmNoConfigurationProvided();
            }

            List<IdentityConfiguration> configurations = new ArrayList<IdentityConfiguration>();

            for (IdentityConfiguration aConfigurationInstance : this.configurationInstance) {
                configurations.add(aConfigurationInstance);
            }

            this.partitionManager = new DefaultPartitionManager(configurations);
        }
    }

    @Produces
    public PartitionManager createPartitionManager() {
        return this.partitionManager;
    }
}
