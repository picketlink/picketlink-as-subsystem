package org.picketlink.as.subsystem.core;

import org.picketlink.annotations.PicketLink;
import org.picketlink.idm.PartitionManager;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import static org.picketlink.as.subsystem.PicketLinkMessages.MESSAGES;

/**
 * <p>This producer is responsible to produce a @{PartitionManager} by obtaining it from the JNDI url defined inside the web deployment
 * descriptor.</p>
 *
 * @author Pedro Igor
 * @see org.picketlink.as.subsystem.core.PicketLinkCoreSubsystemExtension
 */
@ApplicationScoped
public class JNDIPartitionManagerProducer {

    @Inject
    private PicketLinkCoreSubsystemExtension extension;

    @Produces
    @PicketLink
    public PartitionManager lookupPartitionManager() {
        String partitionManagerJNDIUrl = this.extension.getPartitionManagerJNDIUrl();

        if (partitionManagerJNDIUrl == null) {
            throw MESSAGES.coreNullPartitionManagerJNDIUrl();
        }

        try {
            return (PartitionManager) new InitialContext().lookup(partitionManagerJNDIUrl);
        } catch (NamingException e) {
            throw MESSAGES.coreCouldNotLookupPartitionManager(partitionManagerJNDIUrl, e);
        }
    }
}
