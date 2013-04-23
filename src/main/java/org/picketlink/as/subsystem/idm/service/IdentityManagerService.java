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

import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
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
import org.jboss.msc.value.InjectedValue;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.IdentityManagerFactory;
import org.picketlink.idm.model.Realm;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class IdentityManagerService implements Service<ManagedReferenceFactory>, ManagedReferenceFactory {

    private static String SERVICE_NAME_PREFIX = "IdentityManagerService";

    private final InjectedValue<IdentityManagerFactory> identityManagerFactory = new InjectedValue<IdentityManagerFactory>();

    private String realm;

    private String identityManagementAlias;

    private String jndiName;

    public IdentityManagerService(String identityManagementAlias, String realm) {
        this.identityManagementAlias = identityManagementAlias;
        this.realm = realm;
    }

    @Override
    public ManagedReferenceFactory getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public void start(StartContext context) throws StartException {
        ServiceController<?> container = context.getController().getServiceContainer()
                .getService(IdentityManagerFactoryService.createServiceName(this.identityManagementAlias));

        IdentityManagerFactoryService identityManagerFactoryService = (IdentityManagerFactoryService) container.getService();

        this.jndiName = identityManagerFactoryService.getJndiName() + "/" + this.realm;
        
        publishIdentityManager(context);
    }

    @Override
    public void stop(StopContext context) {
        unpublishIdentityManager(context);
    }

    public static ServiceName createServiceName(String identityManagementAlias, String realm) {
        return ServiceName.JBOSS.append(SERVICE_NAME_PREFIX, identityManagementAlias + realm);
    }

    public InjectedValue<IdentityManagerFactory> getIdentityManagerFactory() {
        return identityManagerFactory;
    }

    private void publishIdentityManager(StartContext context) {
        final BinderService binderService = new BinderService("IdentityManagerService-" + this.identityManagementAlias + "-"
                + this.realm);
        
        final ServiceBuilder<ManagedReferenceFactory> builder = context
                .getController()
                .getServiceContainer()
                .addService(ContextNames.buildServiceName(ContextNames.JAVA_CONTEXT_SERVICE_NAME, this.jndiName), binderService);

        builder.addDependency(ContextNames.JAVA_CONTEXT_SERVICE_NAME, ServiceBasedNamingStore.class,
                binderService.getNamingStoreInjector());

        final ManagedReferenceFactory as = this;
        
        builder.addDependency(createServiceName(this.identityManagementAlias, this.realm), ManagedReferenceFactory.class,
                new Injector<ManagedReferenceFactory>() {
                    @Override
                    public void inject(final ManagedReferenceFactory value) throws InjectionException {
                        binderService.getManagedObjectInjector().inject(as);
                    }

                    @Override
                    public void uninject() {
                        binderService.getManagedObjectInjector().uninject();
                    }
                });

        builder.setInitialMode(Mode.ACTIVE).install();
    }

    private void unpublishIdentityManager(StopContext context) {
        ServiceController<?> service = context.getController().getServiceContainer()
                .getService(ContextNames.buildServiceName(ContextNames.JAVA_CONTEXT_SERVICE_NAME, this.jndiName));

        service.setMode(Mode.REMOVE);
    }

    @Override
    public ManagedReference getReference() {
        return new ManagedReference() {
            
            private IdentityManager identityManager = getIdentityManagerFactory().getValue().createIdentityManager(new Realm(realm));
            
            @Override
            public void release() {
                this.identityManager = null;
            }
            
            @Override
            public Object getInstance() {
                return this.identityManager;
            }
        };
    }
}