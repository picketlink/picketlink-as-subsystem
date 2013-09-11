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

import org.picketlink.as.subsystem.core.deployment.PicketLinkSubsystemIdentityManagerProducer;
import org.picketlink.idm.IdentityManager;
import org.picketlink.producer.IdentityManagerProducer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;

/**
 * <p>
 * {@link Extension} implementation to enable PicketLink Core when deploying the application using the subsystem.
 * </p>
 * 
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@ApplicationScoped
public class PicketLinkCoreSubsystemExtension implements Extension {

    private String partitionManagerJNDIUrl;

    public PicketLinkCoreSubsystemExtension() {

    }

    public PicketLinkCoreSubsystemExtension(String partitionManagerJNDIUrl) {
        this.partitionManagerJNDIUrl = partitionManagerJNDIUrl;
    }

    /**
     * <p>
     * We should veto the {@link IdentityManagerProducer} when the {@link IdentityManager} must be used obtained from the JNDI,
     * instead of internally produced. In this case, the {@link IdentityManager} will be properly produced by the
     * {@link PartitionManagerBeanDefinition}.
     * </p>
     * 
     * @param event
     */
    public void vetoIdentityManagerProducer(@Observes ProcessAnnotatedType<IdentityManagerProducer> event) {
        if (this.partitionManagerJNDIUrl != null) {
            event.veto();
        }
    }

    public void test(@Observes BeforeBeanDiscovery event, BeanManager beanManager) {
        if (this.partitionManagerJNDIUrl != null) {
            event.addAnnotatedType(beanManager.createAnnotatedType(PicketLinkSubsystemIdentityManagerProducer.class));
        }
    }

    public String getPartitionManagerJNDIUrl() {
        return partitionManagerJNDIUrl;
    }
}
