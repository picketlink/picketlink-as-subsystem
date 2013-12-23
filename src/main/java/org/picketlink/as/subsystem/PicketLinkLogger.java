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

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import static org.jboss.logging.Logger.Level.INFO;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface PicketLinkLogger extends BasicLogger {

    PicketLinkLogger ROOT_LOGGER = Logger.getMessageLogger(PicketLinkLogger.class, PicketLinkLogger.class.getPackage().getName());

    @LogMessage(level = INFO)
    @Message(id = 12500, value = "Activating PicketLink Subsystem")
    void activatingSubsystem();

    @LogMessage(level = INFO)
    @Message(id = 12501, value = "[%s] - Configuring deployment %s")
    void configuringDeployment(String serviceName, String deploymentName);

    @LogMessage(level = INFO)
    @Message(id = 12502, value = "Bound [%s] to [%s]")
    void boundToJndi(String alias, String jndiName);
}
