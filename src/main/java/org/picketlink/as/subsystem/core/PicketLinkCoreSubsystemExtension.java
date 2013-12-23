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
package org.picketlink.as.subsystem.core;

import org.jboss.as.naming.deployment.ContextNames;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

/**
 * <p> {@link Extension} implementation to enable PicketLink Core when deploying the application using the subsystem. </p>
 *
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Pedro Igor
 */
public class PicketLinkCoreSubsystemExtension implements Extension {

    private final String partitionManagerJNDIUrl;

    public PicketLinkCoreSubsystemExtension() {
        // should not be used. only necessary to meet CDI spec requirements.
        this(null);
    }

    public PicketLinkCoreSubsystemExtension(String partitionManagerJNDIUrl) {
        if (partitionManagerJNDIUrl == null) {
            throw new IllegalArgumentException("PartitionManager JNDI url.");
        }

        this.partitionManagerJNDIUrl = partitionManagerJNDIUrl.replaceAll(ContextNames.JAVA_CONTEXT_SERVICE_NAME.getSimpleName() + ":", "");
    }

    public void installJNDIPartitionManagerProducer(@Observes BeforeBeanDiscovery event, BeanManager beanManager) {
        event.addAnnotatedType(beanManager.createAnnotatedType(JNDIPartitionManagerProducer.class));
    }

    String getPartitionManagerJNDIUrl() {
        return this.partitionManagerJNDIUrl;
    }
}
