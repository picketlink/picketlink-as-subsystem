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

import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.InjectedValue;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.IdentityManagerFactory;
import org.picketlink.idm.model.Realm;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class IdentityManagerService implements Service<IdentityManager> {

    private static String SERVICE_NAME_PREFIX = "IdentityManager";

    private final InjectedValue<IdentityManagerFactory> identityManagerFactory = new InjectedValue<IdentityManagerFactory>();

    private String realm;

    private IdentityManager identityManager;

    private String identityManagementAlias;

    private String jndiName;

    public IdentityManagerService(String identityManagementAlias, String realm) {
        this.identityManagementAlias = identityManagementAlias;
        this.realm = realm;
    }

    @Override
    public IdentityManager getValue() throws IllegalStateException, IllegalArgumentException {
        return this.identityManager;
    }

    @Override
    public void start(StartContext context) throws StartException {
        this.identityManager = this.identityManagerFactory.getValue().createIdentityManager(new Realm(this.realm));
        
        ServiceController<?> container = context.getController().getServiceContainer().getService(IdentityManagerFactoryService.createServiceName(this.identityManagementAlias));

        IdentityManagerFactoryService identityManagerFactoryService = (IdentityManagerFactoryService) container.getService();
        
        this.jndiName = identityManagerFactoryService.getJndiName() + "/" + this.realm;
        
        publishIdentityManagerFactory(context);
    }

    @Override
    public void stop(StopContext context) {
        this.identityManager = null;
        unpublishIdentityManagerFactory(context);
    }

    public static ServiceName createServiceName(String identityManagementAlias, String realm) {
        return ServiceName.JBOSS.append(SERVICE_NAME_PREFIX, identityManagementAlias + realm);
    }
    
    public InjectedValue<IdentityManagerFactory> getIdentityManagerFactory() {
        return identityManagerFactory;
    }
    
    private void publishIdentityManagerFactory(StartContext context) {
        final BinderService binderService = new BinderService("IdentityManagerService-" + this.identityManagementAlias + "-" + this.realm);
        final ServiceBuilder<ManagedReferenceFactory> builder = context.getController().getServiceContainer()
                .addService(ContextNames.buildServiceName(ContextNames.JAVA_CONTEXT_SERVICE_NAME, this.jndiName), binderService);

        builder.addDependency(ContextNames.JAVA_CONTEXT_SERVICE_NAME, ServiceBasedNamingStore.class,
                binderService.getNamingStoreInjector());

        builder.addDependency(createServiceName(this.identityManagementAlias, this.realm), IdentityManager.class,
                new Injector<IdentityManager>() {
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

        builder.setInitialMode(Mode.ACTIVE).install();
    }

    private void unpublishIdentityManagerFactory(StopContext context) {
        ServiceController<?> service = context.getController().getServiceContainer()
                .getService(ContextNames.buildServiceName(ContextNames.JAVA_CONTEXT_SERVICE_NAME, this.jndiName));

        service.setMode(Mode.REMOVE);
    }
}