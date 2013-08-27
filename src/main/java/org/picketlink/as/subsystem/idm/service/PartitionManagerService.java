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
import org.jboss.as.naming.deployment.ContextNames.BindInfo;
import org.jboss.as.naming.deployment.JndiName;
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
import org.picketlink.as.subsystem.PicketLinkExtension;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.config.IdentityConfigurationBuilder;
import org.picketlink.idm.internal.DefaultPartitionManager;
import org.picketlink.idm.model.Partition;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.picketlink.as.subsystem.PicketLinkLogger.*;

/**
 * <p>
 * This {@link Service} starts the {@link PartitionManager} using the configurationBuilder loaded from the domain model and
 * publishes it in JNDI.
 * </p>
 *
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Pedro Igor
 */
public class PartitionManagerService implements Service<PartitionManager> {

    private static String SERVICE_NAME_PREFIX = "PartitionManagerService";

    private final String jndiName;
    private final String alias;
    private PartitionManager partitionManager;
    private final IdentityConfigurationBuilder configurationBuilder;

    public PartitionManagerService(String alias, String jndiName, IdentityConfigurationBuilder configurationBuilder) {
        this.alias = alias;

        // if not jndi name was provide defaults to bellow
        if (jndiName == null) {
            jndiName = JndiName.of(PicketLinkExtension.SUBSYSTEM_NAME).append(this.alias).getAbsoluteName();
        }

        this.jndiName = jndiName;
        this.configurationBuilder = configurationBuilder;
    }

    @Override
    public PartitionManager getValue() throws IllegalStateException, IllegalArgumentException {
        return this.partitionManager;
    }

    @Override
    public void start(StartContext context) throws StartException {
        ROOT_LOGGER.debugf("Starting PartitionManagerService for [%s]", this.alias);
        this.partitionManager = new DefaultPartitionManager(this.configurationBuilder.buildAll());

        publishIdentityManagerFactory(context);
        publishIdentityManagers(context);
    }

    @Override
    public void stop(StopContext context) {
        ROOT_LOGGER.debugf("Stopping PartitionManagerService for [%s]", this.alias);
        unpublishIdentityManagerFactory(context);
        unpublishIdentityManagers(context);
        this.partitionManager = null;
    }

    public IdentityConfigurationBuilder getConfigurationBuilder() {
        return this.configurationBuilder;
    }

    public static ServiceName createServiceName(String alias) {
        return ServiceName.JBOSS.append(SERVICE_NAME_PREFIX, alias);
    }

    protected String getAlias() {
        return this.alias;
    }

    private Set<String> getConfiguredRealms() {
        HashSet<String> realms = new HashSet<String>();

        List<Partition> partitions = this.partitionManager.getPartitions(Partition.class);

        for (Partition partition: partitions) {
            realms.add(partition.getName());
        }

        return realms;
    }

    private void publishIdentityManagerFactory(StartContext context) {
        BindInfo bindInfo = createIdentityManagerFactoryBindInfo();
        ServiceName serviceName = bindInfo.getBinderServiceName();
        final BinderService binderService = new BinderService(serviceName.getCanonicalName());
        final ServiceBuilder<ManagedReferenceFactory> builder = context.getController().getServiceContainer()
                .addService(serviceName, binderService).addAliases(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(jndiName));

        builder.addDependency(ContextNames.JAVA_CONTEXT_SERVICE_NAME, ServiceBasedNamingStore.class,
                binderService.getNamingStoreInjector());

        builder.addDependency(createServiceName(getAlias()), PartitionManager.class,
                new Injector<PartitionManager>() {
                    @Override
                    public void inject(final PartitionManager value) throws InjectionException {
                        binderService.getManagedObjectInjector().inject(
                                new ValueManagedReferenceFactory(new ImmediateValue<Object>(value)));
                    }

                    @Override
                    public void uninject() {
                        binderService.getManagedObjectInjector().uninject();
                    }
                });

        builder.setInitialMode(Mode.PASSIVE).install();

        ROOT_LOGGER.boundToJndi("PartitionManager " + this.alias, bindInfo.getAbsoluteJndiName());
    }

    private void publishIdentityManagers(StartContext context) {
        Set<String> configuredRealms = getConfiguredRealms();

        for (final String realmName : configuredRealms) {
            BindInfo bindInfo = createJndiIdentityManagerBindInfo(realmName);
            ServiceName serviceName = bindInfo.getBinderServiceName();

            ROOT_LOGGER.boundToJndi("IdentityManager", bindInfo.getAbsoluteJndiName());

            final BinderService binderService = new BinderService(serviceName.getCanonicalName());

            final ServiceBuilder<ManagedReferenceFactory> builder = context
                    .getController()
                    .getServiceContainer()
                    .addService(serviceName, binderService);

            builder.addDependency(ContextNames.JAVA_CONTEXT_SERVICE_NAME, ServiceBasedNamingStore.class,
                    binderService.getNamingStoreInjector());
            builder.addDependency(createIdentityManagerFactoryBindInfo().getBinderServiceName(), ManagedReferenceFactory.class,
                    new Injector<ManagedReferenceFactory>() {
                        @Override
                        public void inject(final ManagedReferenceFactory value) throws InjectionException {
                            binderService.getManagedObjectInjector().inject(new IdentityManagerManagedReferenceFactory(realmName, getValue()));
                        }

                        @Override
                        public void uninject() {
                            binderService.getManagedObjectInjector().uninject();
                        }
                    });

            builder.setInitialMode(Mode.PASSIVE).install();
        }
    }

    private void unpublishIdentityManagers(StopContext context) {
        for (String realmName : getConfiguredRealms()) {
            ServiceController<?> service = context.getController().getServiceContainer()
                    .getService(createJndiIdentityManagerBindInfo(realmName).getBinderServiceName());
            if (service != null) {
                service.setMode(Mode.REMOVE);
            }
        }
    }

    private void unpublishIdentityManagerFactory(StopContext context) {
        ServiceController<?> service = context.getController().getServiceContainer()
                .getService(createIdentityManagerFactoryBindInfo().getBinderServiceName());
        if (service != null) {
            service.setMode(Mode.REMOVE);
        }
    }

    private BindInfo createIdentityManagerFactoryBindInfo() {
        return ContextNames.bindInfoFor(this.jndiName);
    }

    private BindInfo createJndiIdentityManagerBindInfo(String realmName) {
        return ContextNames.bindInfoFor(this.jndiName + "/" + realmName);
    }

}