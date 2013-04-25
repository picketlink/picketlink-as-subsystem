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

import static org.picketlink.as.subsystem.PicketLinkLogger.ROOT_LOGGER;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.JndiName;
import org.jboss.as.naming.service.BinderService;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.ImmediateValue;
import org.picketlink.as.subsystem.PicketLinkExtension;
import org.picketlink.idm.IdentityManagerFactory;
import org.picketlink.idm.config.IdentityConfiguration;
import org.picketlink.idm.config.IdentityStoreConfiguration;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class IdentityManagerFactoryService implements Service<IdentityManagerFactory> {

    private static String SERVICE_NAME_PREFIX = "IdentityManagerFactoryService";

    private String jndiName;
    private final String alias;
    private IdentityManagerFactory identityManagerFactory;
    private IdentityConfiguration identityConfiguration;

    public IdentityManagerFactoryService(String alias, String jndiName, IdentityConfiguration configuration) {
        this.alias = alias;

        if (jndiName == null) {
            jndiName = JndiName.of(PicketLinkExtension.SUBSYSTEM_NAME).append(this.alias).getAbsoluteName();
        }

        this.jndiName = toJndiName(jndiName);
        this.identityConfiguration = configuration;
    }

    @Override
    public IdentityManagerFactory getValue() throws IllegalStateException, IllegalArgumentException {
        return this.identityManagerFactory;
    }

    @Override
    public void start(StartContext context) throws StartException {
        ROOT_LOGGER.debugf("Starting IdentityManagerFactoryService for %s", this.alias);

        this.identityManagerFactory = this.identityConfiguration.buildIdentityManagerFactory();
        publishIdentityManagerFactory(context);
        publishIdentityManagers(context);
    }

    @Override
    public void stop(StopContext context) {
        ROOT_LOGGER.debugf("Stopping IdentityManagerFactoryService for %s", this.alias);

        unpublishIdentityManagerFactory(context);
        unpublishIdentityManagers(context);
        this.identityManagerFactory = null;
    }

    public String getJndiName() {
        return this.jndiName;
    }

    public IdentityConfiguration getIdentityConfiguration() {
        return this.identityConfiguration;
    }
    
    public static ServiceName createServiceName(String alias) {
        return ServiceName.JBOSS.append(SERVICE_NAME_PREFIX, alias);
    }

    protected String getAlias() {
        return this.alias;
    }

    private Set<String> getConfiguredRealms() {
        HashSet<String> realms = new HashSet<String>();

        List<IdentityStoreConfiguration> configuredStores = this.identityConfiguration.getConfiguredStores();

        for (IdentityStoreConfiguration identityStoreConfiguration : configuredStores) {
            realms.addAll(identityStoreConfiguration.getRealms());
        }

        return realms;
    }

    private void publishIdentityManagerFactory(StartContext context) {
        String jndiName = getJndiName();

        ServiceName serviceName = createJndiServiceName();
        final BinderService binderService = new BinderService(serviceName.getCanonicalName());
        final ServiceBuilder<ManagedReferenceFactory> builder = context.getController().getServiceContainer()
                .addService(serviceName, binderService).addAliases(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(jndiName));

        builder.addDependency(ContextNames.JAVA_CONTEXT_SERVICE_NAME, ServiceBasedNamingStore.class,
                binderService.getNamingStoreInjector());

        builder.addDependency(IdentityManagerFactoryService.createServiceName(getAlias()), IdentityManagerFactory.class,
                new Injector<IdentityManagerFactory>() {
                    @Override
                    public void inject(final IdentityManagerFactory value) throws InjectionException {
                        binderService.getManagedObjectInjector().inject(
                                new ValueManagedReferenceFactory(new ImmediateValue<Object>(value)));
                    }

                    @Override
                    public void uninject() {
                        binderService.getManagedObjectInjector().uninject();
                    }
                });

        builder.setInitialMode(Mode.PASSIVE).install();
    }

    private void publishIdentityManagers(StartContext context) {
        Set<String> configuredRealms = getConfiguredRealms();

        for (String realmName : configuredRealms) {
            IdentityManagerService identityManagerService = new IdentityManagerService(alias, realmName);

            ServiceBuilder<ManagedReferenceFactory> identityManagerServiceBuilder = context.getController()
                    .getServiceContainer()
                    .addService(IdentityManagerService.createServiceName(alias, realmName), identityManagerService);

            identityManagerServiceBuilder.addDependency(IdentityManagerFactoryService.createServiceName(alias),
                    IdentityManagerFactory.class, identityManagerService.getIdentityManagerFactory());

            identityManagerServiceBuilder.setInitialMode(Mode.PASSIVE).install();
        }
    }

    private void unpublishIdentityManagers(StopContext context) {
        for (String realm : getConfiguredRealms()) {
            context.getController().getServiceContainer()
                    .getService(IdentityManagerService.createServiceName(getAlias(), realm)).setMode(Mode.REMOVE);
        }
    }

    private void unpublishIdentityManagerFactory(StopContext context) {
        context.getController().getServiceContainer().getService(createJndiServiceName()).setMode(Mode.REMOVE);
    }

    private ServiceName createJndiServiceName() {
        return ContextNames.bindInfoFor(this.jndiName).getBinderServiceName();
    }

    private String toJndiName(String jndiName) {
        if (jndiName != null) {
            if (jndiName.startsWith("java:")) {
                jndiName = jndiName.substring(jndiName.indexOf(":") + 1);
            }
        }

        return jndiName;
    }
    
}