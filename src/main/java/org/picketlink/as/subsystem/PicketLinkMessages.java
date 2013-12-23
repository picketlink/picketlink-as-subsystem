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

package org.picketlink.as.subsystem;

import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;
import org.picketlink.idm.config.SecurityConfigurationException;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface PicketLinkMessages {

    PicketLinkMessages MESSAGES = Messages.getBundle(PicketLinkMessages.class);

    @Message(id = 12600, value = "No writer provided for element %s. Check if a writer is registered in PicketLinkSubsystemWriter.")
    IllegalStateException noModelElementWriterProvided(String modelElement);

    @Message(id = 12601, value = "No IdentityConfiguration provided. Maybe you forgot to provide a @Producer method for the IdentityConfiguration.")
    IllegalStateException idmNoConfigurationProvided();

    @Message(id = 12602, value = "No Identity Provider configuration found for federation [%s]. ")
    IllegalStateException federationIdentityProviderNotConfigured(String federationAlias);

    @Message(id = 12603, value = "Failed to configure deployment [%s].")
    DeploymentUnitProcessingException deploymentConfigurationFailed(String name, @Cause Throwable t);

    @Message(id = 12604, value = "Could not load module [%s].")
    RuntimeException moduleCouldNotLoad(String s, @Cause Throwable t);

    @Message (id = 12605, value = "PartitionManager JNDI url not defined. Check your web.xml.")
    SecurityConfigurationException coreNullPartitionManagerJNDIUrl();

    @Message (id = 12606, value = "Error looking up PartitionManager from [%s].")
    SecurityConfigurationException coreCouldNotLookupPartitionManager(String jndiUrl, @Cause Throwable e);
}
