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

package org.picketlink.as.subsystem.idm.model;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.registry.Resource.ResourceEntry;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.value.InjectedValue;
import org.picketlink.as.subsystem.idm.config.JPAStoreSubsystemConfigurationBuilder;
import org.picketlink.as.subsystem.idm.service.PartitionManagerService;
import org.picketlink.as.subsystem.model.AbstractResourceAddStepHandler;
import org.picketlink.as.subsystem.model.ModelElement;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.config.IdentityConfigurationBuilder;
import org.picketlink.idm.config.IdentityStoreConfigurationBuilder;
import org.picketlink.idm.config.NamedIdentityConfigurationBuilder;

import javax.transaction.TransactionManager;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.picketlink.as.subsystem.idm.model.IdentityManagementConfiguration.*;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class IdentityManagementAddHandler extends AbstractResourceAddStepHandler {

    public static final IdentityManagementAddHandler INSTANCE = new IdentityManagementAddHandler();

    private IdentityManagementAddHandler() {
        super(ModelElement.IDENTITY_MANAGEMENT);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model,
                                  final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers)
            throws OperationFailedException {
        final String alias = operation.get(ModelElement.COMMON_ALIAS.getName()).asString();

        ModelNode jndiNameNode = operation.get(ModelElement.IDENTITY_MANAGEMENT_JNDI_NAME.getName());

        final String jndiName;

        if (jndiNameNode.isDefined()) {
            jndiName = toJndiName(jndiNameNode.asString());
        } else {
            jndiName = null;
        }

        IdentityConfigurationBuilder builder = new IdentityConfigurationBuilder();

        PartitionManagerService partitionManagerService = new PartitionManagerService(alias, jndiName, builder);

        ServiceBuilder<PartitionManager> serviceBuilder = context.getServiceTarget().addService(
                PartitionManagerService.createServiceName(alias), partitionManagerService);

        configureDependencies(partitionManagerService, serviceBuilder);

        Iterator<ResourceEntry> identityConfigurationResourceIterator = context.readResource(PathAddress.EMPTY_ADDRESS).getChildren(ModelElement.IDENTITY_CONFIGURATION.getName()).iterator();

        while (identityConfigurationResourceIterator.hasNext()) {
            ResourceEntry identityConfigurationResource = identityConfigurationResourceIterator.next();

            NamedIdentityConfigurationBuilder namedIdentityConfigurationBuilder = builder.named(identityConfigurationResource.getName());

            Set<String> identityStoreTypes = identityConfigurationResource.getChildTypes();

            for (String storeType : identityStoreTypes) {
                for (ResourceEntry identityStoreResource : identityConfigurationResource.getChildren(storeType)) {
                    IdentityStoreConfigurationBuilder storeConfig = configureStore(storeType, identityStoreResource, namedIdentityConfigurationBuilder, partitionManagerService);

                    if (isJPAIdentityStoreConfiguration(storeConfig)) {
                        configureJPAStoreDependencies(serviceBuilder, identityStoreResource);
                    }
                }
            }
        }

        ServiceController<PartitionManager> controller = serviceBuilder.addListener(verificationHandler)
                .setInitialMode(Mode.PASSIVE).install();

        newControllers.add(controller);
    }

    private boolean isJPAIdentityStoreConfiguration(final IdentityStoreConfigurationBuilder storeConfig) {
        return JPAStoreSubsystemConfigurationBuilder.class.isInstance(storeConfig);
    }

    private void configureDependencies(final PartitionManagerService partitionManagerService, final ServiceBuilder<PartitionManager> serviceBuilder) {
        serviceBuilder.addDependency(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER, TransactionManager.class, partitionManagerService.getTransactionManager());
    }

    private void configureJPAStoreDependencies(final ServiceBuilder<PartitionManager> serviceBuilder, final ResourceEntry identityStoreResource) {
        ModelNode jpaDataSourceNode = identityStoreResource.getModel().get(ModelElement.JPA_STORE_DATASOURCE.getName());
        ModelNode jpaEntityManagerFactoryNode = identityStoreResource.getModel().get(ModelElement.JPA_STORE_ENTITY_MANAGER_FACTORY.getName());

        String dataSourceJndiName = null;

        if (jpaDataSourceNode.isDefined()) {
            dataSourceJndiName = jpaDataSourceNode.asString();
        }

        dataSourceJndiName = toJndiName(dataSourceJndiName);

        if (dataSourceJndiName != null) {
            serviceBuilder.addDependency(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(dataSourceJndiName.split("/")));
        }

        String emfJndiName = null;

        if (jpaEntityManagerFactoryNode.isDefined()) {
            emfJndiName = jpaEntityManagerFactoryNode.asString();
        }

        if (emfJndiName != null) {
            serviceBuilder.addDependency(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(emfJndiName.split("/")), ValueManagedReferenceFactory.class, new InjectedValue<ValueManagedReferenceFactory>());
        }
    }

}