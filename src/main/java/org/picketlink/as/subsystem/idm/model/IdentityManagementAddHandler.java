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
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.registry.Resource.ResourceEntry;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.picketlink.as.subsystem.idm.service.JPABasedIdentityManagerFactoryService;
import org.picketlink.as.subsystem.idm.service.PartitionManagerService;
import org.picketlink.as.subsystem.model.AbstractResourceAddStepHandler;
import org.picketlink.as.subsystem.model.ModelElement;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.config.IdentityConfigurationBuilder;
import org.picketlink.idm.config.NamedIdentityConfigurationBuilder;

import javax.transaction.TransactionManager;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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

        String jndiName = null;

        if (jndiNameNode.isDefined()) {
            jndiName = jndiNameNode.asString();
        }

        jndiName = toJndiName(jndiName);

        Resource identityManagementResource = context.readResource(PathAddress.EMPTY_ADDRESS);
        IdentityConfigurationBuilder builder = new IdentityConfigurationBuilder();

        fromResource(identityManagementResource, builder);

        PartitionManagerService partitionManagerService = null;

        Iterator<ResourceEntry> iterator = identityManagementResource.getChildren(ModelElement.IDENTITY_CONFIGURATION.getName()).iterator();

        while (iterator.hasNext()) {
            ResourceEntry configuration = iterator.next();

            NamedIdentityConfigurationBuilder namedIdentityConfigurationBuilder = builder.named(configuration.getName());

            Set<ResourceEntry> children = configuration.getChildren(ModelElement.JPA_STORE.getName());

            if (!children.isEmpty()) {
                partitionManagerService = installJPABasedIdentityManagerFactory(context, verificationHandler, newControllers,
                        alias, jndiName, namedIdentityConfigurationBuilder, builder, children);
            }
        }

        if (partitionManagerService == null) {
            partitionManagerService = new PartitionManagerService(alias, jndiName, builder);

            ServiceBuilder<PartitionManager> serviceBuilder = context.getServiceTarget().addService(
                    PartitionManagerService.createServiceName(alias), partitionManagerService);

            ServiceController<PartitionManager> controller = serviceBuilder.addListener(verificationHandler)
                    .setInitialMode(Mode.PASSIVE).install();

            newControllers.add(controller);
        }
    }

    private PartitionManagerService installJPABasedIdentityManagerFactory(OperationContext context,
                                                                          final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers,
                                                                          final String alias, String jndiName, NamedIdentityConfigurationBuilder namedBuilder, IdentityConfigurationBuilder builder, Set<ResourceEntry> jpaStoreResources) {
        ResourceEntry jpaStoreResource = jpaStoreResources.iterator().next();

        ModelNode jpaDataSourceNode = jpaStoreResource.getModel().get(ModelElement.JPA_STORE_DATASOURCE.getName());
        ModelNode jpaEntityModule = jpaStoreResource.getModel().get(ModelElement.JPA_STORE_ENTITY_MODULE.getName());
        ModelNode jpaEntityModuleUnitName = jpaStoreResource.getModel().get(
                ModelElement.JPA_STORE_ENTITY_MODULE_UNIT_NAME.getName());
        ModelNode jpaEntityManagerFactoryNode = jpaStoreResource.getModel().get(
                ModelElement.JPA_STORE_ENTITY_MANAGER_FACTORY.getName());

        String dataSourceJndiName = null;

        if (jpaDataSourceNode.isDefined()) {
            dataSourceJndiName = jpaDataSourceNode.asString();
        }

        dataSourceJndiName = toJndiName(dataSourceJndiName);

        String entityModuleName = null;

        if (jpaEntityModule.isDefined()) {
            entityModuleName = jpaEntityModule.asString();
        }

        String emfJndiName = null;

        if (jpaEntityManagerFactoryNode.isDefined()) {
            emfJndiName = jpaEntityManagerFactoryNode.asString();
        }

        String entityModuleUnitName = null;

        if (jpaEntityModuleUnitName.isDefined()) {
            entityModuleUnitName = jpaEntityModuleUnitName.asString();
        }

        JPABasedIdentityManagerFactoryService jpaBasedIdentityManagerFactory = new JPABasedIdentityManagerFactoryService(alias, jndiName, dataSourceJndiName,
                entityModuleName, entityModuleUnitName, namedBuilder, builder);

        ServiceBuilder<PartitionManager> serviceBuilder = context.getServiceTarget().addService(
                JPABasedIdentityManagerFactoryService.createServiceName(alias), jpaBasedIdentityManagerFactory);

        serviceBuilder.addDependency(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER, TransactionManager.class, jpaBasedIdentityManagerFactory.getTransactionManager());

        if (dataSourceJndiName != null) {
            serviceBuilder.addDependency(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(dataSourceJndiName.split("/")));
        }

        if (emfJndiName != null) {
            serviceBuilder.addDependency(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(emfJndiName.split("/")), ValueManagedReferenceFactory.class, jpaBasedIdentityManagerFactory.getProvidedEntityManagerFactory());
        }

        ServiceController<PartitionManager> controller = serviceBuilder.addListener(verificationHandler)
                .setInitialMode(Mode.PASSIVE).install();

        newControllers.add(controller);

        return jpaBasedIdentityManagerFactory;
    }

    private void fromResource(Resource resource, IdentityConfigurationBuilder builder) {
        Iterator<ResourceEntry> iterator = resource.getChildren(ModelElement.IDENTITY_CONFIGURATION.getName()).iterator();

        while (iterator.hasNext()) {
            ResourceEntry configuration = iterator.next();

            NamedIdentityConfigurationBuilder namedIdentityConfigurationBuilder = builder.named(configuration.getName());

            Set<String> storeTypes = configuration.getChildTypes();

            for (String storeType : storeTypes) {
                for (ResourceEntry storeResource : configuration.getChildren(storeType)) {
                    IdentityManagementConfiguration.configureStore(storeType, storeResource, namedIdentityConfigurationBuilder);
                }
            }
        }
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