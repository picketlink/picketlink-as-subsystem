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

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.value.InjectedValue;
import org.picketlink.as.subsystem.PicketLinkMessages;
import org.picketlink.as.subsystem.idm.config.JPAStoreSubsystemConfiguration;
import org.picketlink.as.subsystem.idm.config.JPAStoreSubsystemConfigurationBuilder;
import org.picketlink.as.subsystem.idm.service.PartitionManagerService;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.config.FileStoreConfigurationBuilder;
import org.picketlink.idm.config.IdentityConfigurationBuilder;
import org.picketlink.idm.config.IdentityStoreConfigurationBuilder;
import org.picketlink.idm.config.LDAPMappingConfigurationBuilder;
import org.picketlink.idm.config.LDAPStoreConfigurationBuilder;
import org.picketlink.idm.config.NamedIdentityConfigurationBuilder;
import org.picketlink.idm.model.AttributedType;
import org.picketlink.idm.model.Relationship;

import javax.transaction.TransactionManager;
import java.util.List;

import static org.jboss.as.controller.PathAddress.EMPTY_ADDRESS;
import static org.picketlink.as.subsystem.model.ModelElement.FILE_STORE;
import static org.picketlink.as.subsystem.model.ModelElement.IDENTITY_CONFIGURATION;
import static org.picketlink.as.subsystem.model.ModelElement.IDENTITY_STORE_CREDENTIAL_HANDLER;
import static org.picketlink.as.subsystem.model.ModelElement.JPA_STORE;
import static org.picketlink.as.subsystem.model.ModelElement.LDAP_STORE;
import static org.picketlink.as.subsystem.model.ModelElement.LDAP_STORE_ATTRIBUTE;
import static org.picketlink.as.subsystem.model.ModelElement.LDAP_STORE_MAPPING;
import static org.picketlink.as.subsystem.model.ModelElement.SUPPORTED_TYPE;
import static org.picketlink.as.subsystem.model.ModelElement.SUPPORTED_TYPES;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class IdentityManagementAddHandler extends AbstractAddStepHandler {

    static final IdentityManagementAddHandler INSTANCE = new IdentityManagementAddHandler();

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (SimpleAttributeDefinition attribute : IdentityManagementResourceDefinition.INSTANCE.getAttributes()) {
            attribute.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        createPartitionManagerService(context, model, verificationHandler, newControllers);
    }

    public void createPartitionManagerService(final OperationContext context, final ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        final String alias = IdentityManagementResourceDefinition.ALIAS.resolveModelAttribute(context, model).asString();
        final String jndiName = IdentityManagementResourceDefinition.IDENTITY_MANAGEMENT_JNDI_URL.resolveModelAttribute(context, model).asString();
        IdentityConfigurationBuilder builder = new IdentityConfigurationBuilder();
        PartitionManagerService partitionManagerService = new PartitionManagerService(alias, jndiName, builder);
        ServiceBuilder<PartitionManager> serviceBuilder = context.getServiceTarget().addService(PartitionManagerService.createServiceName(alias), partitionManagerService);
        ModelNode identityManagement = Resource.Tools.readModel(context.readResource(EMPTY_ADDRESS));

        for (ModelNode identityConfiguration : identityManagement.get(IDENTITY_CONFIGURATION.getName()).asList()) {
            NamedIdentityConfigurationBuilder namedIdentityConfigurationBuilder = builder.named(identityConfiguration.asProperty().getName());

            for (ModelNode store : identityConfiguration.asProperty().getValue().asList()) {
                String storeType = store.keys().iterator().next();
                ModelNode identityStore = store.asProperty().getValue().asProperty().getValue();
                IdentityStoreConfigurationBuilder storeConfig = configureIdentityStore(context, namedIdentityConfigurationBuilder, storeType, identityStore);

                if (JPAStoreSubsystemConfigurationBuilder.class.isInstance(storeConfig)) {
                    ((JPAStoreSubsystemConfigurationBuilder) storeConfig).transactionManager(partitionManagerService.getTransactionManager());
                    configureJPAStoreDependencies(context, serviceBuilder, identityStore);
                }
            }
        }

        serviceBuilder.addDependency(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER, TransactionManager.class, partitionManagerService.getTransactionManager());

        ServiceController<PartitionManager> controller = serviceBuilder.addListener(verificationHandler).setInitialMode(Mode.PASSIVE).install();

        if (newControllers != null) {
            newControllers.add(controller);
        }
    }

    private void configureJPAStoreDependencies(final OperationContext context, final ServiceBuilder<PartitionManager> serviceBuilder, final ModelNode identityStoreResource) throws OperationFailedException {
        ModelNode jpaDataSourceNode = JPAStoreResourceDefinition.DATA_SOURCE.resolveModelAttribute(context, identityStoreResource);
        ModelNode jpaEntityManagerFactoryNode = JPAStoreResourceDefinition.ENTITY_MANAGER_FACTORY.resolveModelAttribute(context, identityStoreResource);

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
            serviceBuilder.addDependency(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(emfJndiName.split("/")),
                                                ValueManagedReferenceFactory.class, new InjectedValue<ValueManagedReferenceFactory>());
        }
    }

    private String toJndiName(String jndiName) {
        if (jndiName != null) {
            if (jndiName.startsWith("java:")) {
                return jndiName.substring(jndiName.indexOf(":") + 1);
            }
        }

        return jndiName;
    }

    private IdentityStoreConfigurationBuilder configureIdentityStore(OperationContext context, NamedIdentityConfigurationBuilder namedIdentityConfigurationBuilder, String storeType, ModelNode identityStore) throws OperationFailedException {
        ModelNode alternativeModuleNode = JPAStoreResourceDefinition.MODULE.resolveModelAttribute(context, identityStore);
        Module alternativeModule;

        if (alternativeModuleNode.isDefined()) {
            ModuleLoader moduleLoader = Module.getContextModuleLoader();
            try {
                alternativeModule = moduleLoader.loadModule(ModuleIdentifier.create(alternativeModuleNode.asString()));
            } catch (ModuleLoadException e) {
                throw new IllegalStateException("Could not load module [" + alternativeModuleNode.asString() + "].");
            }
        } else {
            alternativeModule = Module.getCallerModule();
        }

        IdentityStoreConfigurationBuilder storeConfig;

        if (storeType.equals(JPA_STORE.getName())) {
            storeConfig = configureJPAIdentityStore(context, identityStore, namedIdentityConfigurationBuilder);
        } else if (storeType.equals(FILE_STORE.getName())) {
            storeConfig = configureFileIdentityStore(context, identityStore, namedIdentityConfigurationBuilder);
        } else if (storeType.equals(LDAP_STORE.getName())) {
            storeConfig = configureLDAPIdentityStore(context, alternativeModule, identityStore, namedIdentityConfigurationBuilder);
        } else {
            throw PicketLinkMessages.MESSAGES.idmNoConfigurationProvided();
        }

        ModelNode supportAttributeNode = JPAStoreResourceDefinition.SUPPORT_ATTRIBUTE.resolveModelAttribute(context, identityStore);

        storeConfig.supportAttributes(true);

        if (supportAttributeNode.isDefined()) {
            storeConfig.supportAttributes(supportAttributeNode.asBoolean());
        }

        ModelNode supportCredentialNode = JPAStoreResourceDefinition.SUPPORT_CREDENTIAL.resolveModelAttribute(context, identityStore);

        storeConfig.supportCredentials(true);

        if (supportCredentialNode.isDefined()) {
            storeConfig.supportCredentials(supportCredentialNode.asBoolean());
        }

        configureSupportedTypes(context, identityStore, alternativeModule, storeConfig);
        configureCredentialHandlers(context, identityStore, alternativeModule, storeConfig);

        return storeConfig;
    }

    private LDAPStoreConfigurationBuilder configureLDAPIdentityStore(OperationContext context, Module alternativeModule, ModelNode ldapIdentityStore, NamedIdentityConfigurationBuilder builder) throws OperationFailedException {
        LDAPStoreConfigurationBuilder storeConfig = builder.stores().ldap();
        ModelNode url = LDAPStoreResourceDefinition.URL.resolveModelAttribute(context, ldapIdentityStore);
        ModelNode bindDn = LDAPStoreResourceDefinition.BIND_DN.resolveModelAttribute(context, ldapIdentityStore);
        ModelNode bindCredential = LDAPStoreResourceDefinition.BIND_CREDENTIAL.resolveModelAttribute(context, ldapIdentityStore);
        ModelNode baseDn = LDAPStoreResourceDefinition.BASE_DN_SUFFIX.resolveModelAttribute(context, ldapIdentityStore);

        if (url.isDefined()) {
            storeConfig.url(url.asString());
        }

        if (bindDn.isDefined()) {
            storeConfig.bindDN(bindDn.asString());
        }

        if (bindCredential.isDefined()) {
            storeConfig.bindCredential(bindCredential.asString());
        }

        if (baseDn.isDefined()) {
            storeConfig.baseDN(baseDn.asString());
        }

        if (ldapIdentityStore.hasDefined(LDAP_STORE_MAPPING.getName())) {
            for (ModelNode mappingNode : ldapIdentityStore.get(LDAP_STORE_MAPPING.getName()).asList()) {
                ModelNode ldapMapping = mappingNode.asProperty().getValue();
                String mappingClass = LDAPStoreMappingResourceDefinition.CLASS.resolveModelAttribute(context, ldapMapping).asString();
                LDAPMappingConfigurationBuilder storeMapping;

                try {
                    storeMapping = storeConfig.mapping(this.<AttributedType>loadClass(alternativeModule, mappingClass));
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Could not load LDAP mapped class [" + mappingClass + "].", e);
                }

                ModelNode relatesTo = LDAPStoreMappingResourceDefinition.RELATES_TO.resolveModelAttribute(context, ldapMapping);

                if (relatesTo.isDefined()) {
                    try {
                        storeMapping.forMapping(this.<AttributedType>loadClass(alternativeModule, relatesTo.asString()));
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("Could not load LDAP mapped class [" + mappingClass + "].", e);
                    }
                } else {
                    String baseDN = LDAPStoreMappingResourceDefinition.BASE_DN.resolveModelAttribute(context, ldapMapping)
                                            .asString();

                    storeMapping.baseDN(baseDN);

                    String objectClasses = LDAPStoreMappingResourceDefinition.OBJECT_CLASSES.resolveModelAttribute(context, ldapMapping).asString();

                    for (String objClass : objectClasses.split(",")) {
                        if (!objClass.trim().isEmpty()) {
                            storeMapping.objectClasses(objClass);
                        }
                    }

                    ModelNode parentAttributeName = LDAPStoreMappingResourceDefinition.PARENT_ATTRIBUTE.resolveModelAttribute(context, ldapMapping);

                    if (parentAttributeName.isDefined()) {
                        storeMapping.parentMembershipAttributeName(parentAttributeName.asString());
                    }
                }

                if (ldapMapping.hasDefined(LDAP_STORE_ATTRIBUTE.getName())) {
                    for (ModelNode attributeNode : ldapMapping.get(LDAP_STORE_ATTRIBUTE.getName()).asList()) {
                        ModelNode attribute = attributeNode.asProperty().getValue();
                        String name = LDAPStoreAttributeResourceDefinition.NAME.resolveModelAttribute(context, attribute).asString();
                        String ldapName = LDAPStoreAttributeResourceDefinition.LDAP_NAME.resolveModelAttribute(context, attribute).asString();
                        boolean readOnly = LDAPStoreAttributeResourceDefinition.READ_ONLY.resolveModelAttribute(context, attribute).asBoolean();

                        if (readOnly) {
                            storeMapping.readOnlyAttribute(name, ldapName);
                        } else {
                            boolean isIdentifier = LDAPStoreAttributeResourceDefinition.IS_IDENTIFIER.resolveModelAttribute(context, attribute).asBoolean();
                            storeMapping.attribute(name, ldapName, isIdentifier);
                        }
                    }
                }
            }
        }

        return storeConfig;
    }

    @SuppressWarnings("unchecked")
    private <T> Class<T> loadClass(final Module module, final String typeName) throws ClassNotFoundException {
        if (module != null) {
            return (Class<T>) module.getClassLoader().loadClass(typeName);
        }

        return (Class<T>) Class.forName(typeName);
    }

    private IdentityStoreConfigurationBuilder configureFileIdentityStore(OperationContext context, ModelNode resource, final NamedIdentityConfigurationBuilder builder) throws OperationFailedException {
        FileStoreConfigurationBuilder fileStoreBuilder = builder.stores().file();

        ModelNode workingDir = FileStoreResourceDefinition.WORKING_DIR.resolveModelAttribute(context, resource);
        ModelNode alwaysCreateFiles = FileStoreResourceDefinition.ALWAYS_CREATE_FILE.resolveModelAttribute(context, resource);
        ModelNode asyncWrite = FileStoreResourceDefinition.ASYNC_WRITE.resolveModelAttribute(context, resource);
        ModelNode asyncWriteThreadPool = FileStoreResourceDefinition.ASYNC_WRITE_THREAD_POOL.resolveModelAttribute(context, resource);

        if (workingDir.isDefined()) {
            fileStoreBuilder.workingDirectory(workingDir.asString());
        }

        if (alwaysCreateFiles.isDefined()) {
            fileStoreBuilder.preserveState(!alwaysCreateFiles.asBoolean());
        }

        if (asyncWrite.isDefined()) {
            fileStoreBuilder.asyncWrite(asyncWrite.asBoolean());
        }

        if (asyncWriteThreadPool.isDefined()) {
            fileStoreBuilder.asyncWriteThreadPool(asyncWriteThreadPool.asInt());
        }

        return fileStoreBuilder;
    }

    private JPAStoreSubsystemConfigurationBuilder configureJPAIdentityStore(OperationContext context, final ModelNode resource, final NamedIdentityConfigurationBuilder builder) throws OperationFailedException {
        JPAStoreSubsystemConfigurationBuilder storeConfig = builder.stores().add(JPAStoreSubsystemConfiguration.class, JPAStoreSubsystemConfigurationBuilder.class);

        ModelNode jpaDataSourceNode = JPAStoreResourceDefinition.DATA_SOURCE.resolveModelAttribute(context, resource);
        ModelNode jpaEntityModule = JPAStoreResourceDefinition.ENTITY_MODULE.resolveModelAttribute(context, resource);
        ModelNode jpaEntityModuleUnitName = JPAStoreResourceDefinition.ENTITY_MODULE_UNIT_NAME.resolveModelAttribute(context, resource);
        ModelNode jpaEntityManagerFactoryNode = JPAStoreResourceDefinition.ENTITY_MANAGER_FACTORY.resolveModelAttribute(context, resource);

        if (jpaEntityModule.isDefined()) {
            storeConfig.entityModule(jpaEntityModule.asString());
        }

        if (jpaEntityModuleUnitName.isDefined()) {
            storeConfig.entityModuleUnitName(jpaEntityModuleUnitName.asString());
        }

        if (jpaDataSourceNode.isDefined()) {
            storeConfig.dataSourceJndiUrl(toJndiName(jpaDataSourceNode.asString()));
        }

        if (jpaEntityManagerFactoryNode.isDefined()) {
            storeConfig.entityManagerFactoryJndiName(jpaEntityManagerFactoryNode.asString());
        }

        return storeConfig;
    }

    @SuppressWarnings("unchecked")
    private void configureSupportedTypes(OperationContext context, ModelNode identityStore, Module alternativeModule, IdentityStoreConfigurationBuilder storeConfig) throws OperationFailedException {
        if (identityStore.hasDefined(SUPPORTED_TYPES.getName())) {
            ModelNode featuresSet = identityStore.get(SUPPORTED_TYPES.getName()).asProperty().getValue();
            ModelNode supportsAll = SupportedTypesResourceDefinition.SUPPORTS_ALL.resolveModelAttribute(context, featuresSet);

            if (supportsAll.isDefined() && supportsAll.asBoolean()) {
                storeConfig.supportAllFeatures();
            }

            if (featuresSet.hasDefined(SUPPORTED_TYPE.getName())) {
                for (ModelNode featureNode : featuresSet.get(SUPPORTED_TYPE.getName()).asList()) {
                    ModelNode feature = featureNode.asProperty().getValue();
                    String typeName = SupportedTypeResourceDefinition.COMMON_CLASS.resolveModelAttribute(context, feature).asString();

                    try {
                        Class<? extends AttributedType> attributedTypeClass = loadClass(alternativeModule, typeName);

                        if (Relationship.class.isAssignableFrom(attributedTypeClass)) {
                            storeConfig.supportGlobalRelationship((Class<? extends Relationship>) attributedTypeClass);
                        } else {
                            storeConfig.supportType(attributedTypeClass);
                        }
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("Could not find type [" + typeName + "].", e);
                    }
                }
            }
        }
    }

    private void configureCredentialHandlers(OperationContext context, ModelNode identityStore, Module alternativeModule, IdentityStoreConfigurationBuilder storeConfig) throws OperationFailedException {
        if (identityStore.hasDefined(IDENTITY_STORE_CREDENTIAL_HANDLER.getName())) {
            for (ModelNode credentialHandler : identityStore.get(IDENTITY_STORE_CREDENTIAL_HANDLER.getName()).asList()) {
                String typeName = CredentialHandlerResourceDefinition.CLASS.resolveModelAttribute(context, credentialHandler.asProperty().getValue()).asString();

                try {
                    storeConfig.addCredentialHandler(loadClass(alternativeModule, typeName));
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Could not find type [" + typeName + "].", e);
                }
            }
        }
    }
}
