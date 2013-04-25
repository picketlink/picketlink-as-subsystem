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

import java.util.List;
import java.util.Set;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.registry.Resource.ResourceEntry;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.picketlink.as.subsystem.idm.service.IdentityManagerFactoryService;
import org.picketlink.as.subsystem.idm.service.JPABasedIdentityManagerFactoryService;
import org.picketlink.as.subsystem.model.AbstractResourceAddStepHandler;
import org.picketlink.as.subsystem.model.ModelElement;
import org.picketlink.idm.IdentityManagerFactory;
import org.picketlink.idm.config.BaseAbstractStoreConfiguration;
import org.picketlink.idm.config.IdentityConfiguration;

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

        Resource identityManagementResource = context.readResource(PathAddress.EMPTY_ADDRESS);

        IdentityConfiguration configuration = fromResource(identityManagementResource);

        IdentityManagerFactoryService identityManagerFactoryService = null;

        Set<ResourceEntry> jpaStoreResources = identityManagementResource.getChildren(ModelElement.JPA_STORE.getName());

        if (!jpaStoreResources.isEmpty()) {
            identityManagerFactoryService = installJPABasedIdentityManagerFactory(context, verificationHandler, newControllers,
                    alias, jndiName, configuration, jpaStoreResources);
        } else {
            identityManagerFactoryService = new IdentityManagerFactoryService(alias, jndiName, configuration);

            ServiceBuilder<IdentityManagerFactory> serviceBuilder = context.getServiceTarget().addService(
                    IdentityManagerFactoryService.createServiceName(alias), identityManagerFactoryService);

            ServiceController<IdentityManagerFactory> controller = serviceBuilder.addListener(verificationHandler)
                    .setInitialMode(Mode.PASSIVE).install();

            newControllers.add(controller);
        }
    }

    private IdentityManagerFactoryService installJPABasedIdentityManagerFactory(OperationContext context,
            final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers,
            final String alias, String jndiName, IdentityConfiguration configuration, Set<ResourceEntry> jpaStoreResources) {
        Resource jpaStoreResource;
        jpaStoreResource = jpaStoreResources.iterator().next();

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
                entityModuleName, entityModuleUnitName, configuration);

        ServiceBuilder<IdentityManagerFactory> serviceBuilder = context.getServiceTarget().addService(
                JPABasedIdentityManagerFactoryService.createServiceName(alias), jpaBasedIdentityManagerFactory);

        if (dataSourceJndiName != null) {
            serviceBuilder.addDependency(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(dataSourceJndiName.split("/")));
        }

        if (emfJndiName != null) {
            serviceBuilder.addDependency(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(emfJndiName.split("/")), ValueManagedReferenceFactory.class, jpaBasedIdentityManagerFactory.getProvidedEntityManagerFactory());
        }

        ServiceController<IdentityManagerFactory> controller = serviceBuilder.addListener(verificationHandler)
                .setInitialMode(Mode.PASSIVE).install();

        newControllers.add(controller);

        return jpaBasedIdentityManagerFactory;
    }

    private IdentityConfiguration fromResource(Resource resource) {
        IdentityConfiguration configuration = new IdentityConfiguration();

        for (ModelElement supportedStoreTypes : getSupportedStoreTypes()) {
            String elementName = supportedStoreTypes.getName();

            Resource storeResource = resource.getChild(PathElement.pathElement(elementName, elementName));

            if (storeResource != null) {
                BaseAbstractStoreConfiguration<?> storeConfig = IdentityManagementConfiguration.configureStore(
                        supportedStoreTypes, storeResource);

                configuration.addConfig(storeConfig);
            }
        }
        return configuration;
    }

    private static final ModelElement[] getSupportedStoreTypes() {
        return new ModelElement[] { ModelElement.JPA_STORE, ModelElement.FILE_STORE, ModelElement.LDAP_STORE };
    }

}