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
package org.picketlink.as.subsystem.idm.service;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.cfg.AvailableSettings;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.jpa.hibernate4.JBossAppServerJtaPlatform;
import org.jboss.as.jpa.transaction.JtaManagerImpl;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class IdentityService implements Service<IdentityService> {

    private static final Logger log = Logger.getLogger("org.eventjuggler.services.identity");

    public static ServiceName SERVICE_NAME = ServiceName.JBOSS.append("identity");

    public static ServiceName JNDI_SERVICE_NAME = ContextNames.JBOSS_CONTEXT_SERVICE_NAME.append("IdentityService");

    private EntityManagerFactory emf;

    public static ServiceController<IdentityService> addService(final ServiceTarget target,
            final ServiceVerificationHandler verificationHandler) {
        IdentityService service = new IdentityService();
        ServiceBuilder<IdentityService> serviceBuilder = target.addService(SERVICE_NAME, service);

        serviceBuilder.addListener(verificationHandler);
        return serviceBuilder.install();
    }

    @Override
    public IdentityService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public EntityManagerFactory getEmf() {
        return emf;
    }

    @Override
    public void start(StartContext context) throws StartException {
        log.info("Starting Identity Service");

        log.info("Creating entity manager factory");

        Map<Object, Object> properties = new HashMap();
        properties.put(AvailableSettings.JTA_PLATFORM, new JBossAppServerJtaPlatform(JtaManagerImpl.getInstance()));

        emf = Persistence.createEntityManagerFactory("identity", properties);
    }

    @Override
    public void stop(StopContext context) {
        log.info("Stopping Identity Service");

        log.info("Closing entity manager factory");
        emf.close();
    }

}
