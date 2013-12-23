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

package org.picketlink.as.subsystem.parser;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.picketlink.as.subsystem.Namespace;
import org.picketlink.as.subsystem.PicketLinkExtension;
import org.picketlink.as.subsystem.federation.model.FederationResourceDefinition;
import org.picketlink.as.subsystem.federation.model.KeyProviderResourceDefinition;
import org.picketlink.as.subsystem.federation.model.handlers.HandlerParameterResourceDefinition;
import org.picketlink.as.subsystem.federation.model.handlers.HandlerResourceDefinition;
import org.picketlink.as.subsystem.federation.model.idp.IdentityProviderResourceDefinition;
import org.picketlink.as.subsystem.federation.model.idp.TrustDomainResourceDefinition;
import org.picketlink.as.subsystem.federation.model.saml.SAMLResourceDefinition;
import org.picketlink.as.subsystem.federation.model.sp.ServiceProviderResourceDefinition;
import org.picketlink.as.subsystem.idm.model.CredentialHandlerResourceDefinition;
import org.picketlink.as.subsystem.idm.model.FileStoreResourceDefinition;
import org.picketlink.as.subsystem.idm.model.IdentityConfigurationResourceDefinition;
import org.picketlink.as.subsystem.idm.model.IdentityManagementResourceDefinition;
import org.picketlink.as.subsystem.idm.model.JPAStoreResourceDefinition;
import org.picketlink.as.subsystem.idm.model.LDAPStoreAttributeResourceDefinition;
import org.picketlink.as.subsystem.idm.model.LDAPStoreMappingResourceDefinition;
import org.picketlink.as.subsystem.idm.model.LDAPStoreResourceDefinition;
import org.picketlink.as.subsystem.idm.model.SupportedTypeResourceDefinition;
import org.picketlink.as.subsystem.idm.model.SupportedTypesResourceDefinition;
import org.picketlink.as.subsystem.model.ModelElement;
import org.picketlink.as.subsystem.model.XMLElement;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.picketlink.as.subsystem.model.ModelElement.COMMON_HANDLER;
import static org.picketlink.as.subsystem.model.ModelElement.COMMON_HANDLER_PARAMETER;
import static org.picketlink.as.subsystem.model.ModelElement.COMMON_NAME;
import static org.picketlink.as.subsystem.model.ModelElement.FEDERATION;
import static org.picketlink.as.subsystem.model.ModelElement.FILE_STORE;
import static org.picketlink.as.subsystem.model.ModelElement.IDENTITY_CONFIGURATION;
import static org.picketlink.as.subsystem.model.ModelElement.IDENTITY_MANAGEMENT;
import static org.picketlink.as.subsystem.model.ModelElement.IDENTITY_PROVIDER;
import static org.picketlink.as.subsystem.model.ModelElement.IDENTITY_PROVIDER_TRUST_DOMAIN;
import static org.picketlink.as.subsystem.model.ModelElement.IDENTITY_STORE_CREDENTIAL_HANDLER;
import static org.picketlink.as.subsystem.model.ModelElement.JPA_STORE;
import static org.picketlink.as.subsystem.model.ModelElement.KEY_STORE;
import static org.picketlink.as.subsystem.model.ModelElement.LDAP_STORE;
import static org.picketlink.as.subsystem.model.ModelElement.LDAP_STORE_ATTRIBUTE;
import static org.picketlink.as.subsystem.model.ModelElement.LDAP_STORE_MAPPING;
import static org.picketlink.as.subsystem.model.ModelElement.SAML;
import static org.picketlink.as.subsystem.model.ModelElement.SERVICE_PROVIDER;
import static org.picketlink.as.subsystem.model.ModelElement.SUPPORTED_TYPE;
import static org.picketlink.as.subsystem.model.ModelElement.SUPPORTED_TYPES;

/**
 * <p> XML Reader for the subsystem schema, version 1.0. </p>
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class PicketLinkSubsystemReader_1_0 implements XMLStreamConstants, XMLElementReader<List<ModelNode>> {

    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> addOperations) throws XMLStreamException {
        requireNoAttributes(reader);

        Namespace nameSpace = Namespace.forUri(reader.getNamespaceURI());

        ModelNode subsystemNode = createSubsystemRoot();

        addOperations.add(subsystemNode);

        switch (nameSpace) {
            case PICKETLINK_1_0:
                this.readElement_1_0(reader, subsystemNode, addOperations);
                break;
            default:
                throw unexpectedElement(reader);
        }
    }

    private void readElement_1_0(XMLExtendedStreamReader reader, ModelNode subsystemNode, List<ModelNode> addOperations)
            throws XMLStreamException {
        if (Namespace.PICKETLINK_1_0 != Namespace.forUri(reader.getNamespaceURI())) {
            throw unexpectedElement(reader);
        }

        while (reader.hasNext() && reader.nextTag() != END_DOCUMENT) {
            if (!reader.isStartElement()) {
                continue;
            }

            // if the current element is supported but is not a model element
            if (XMLElement.forName(reader.getLocalName()) != null) {
                continue;
            }

            ModelElement modelKey = ModelElement.forName(reader.getLocalName());

            if (modelKey == null) {
                throw unexpectedElement(reader);
            }

            switch (modelKey) {
                case FEDERATION:
                    parseFederation(reader, subsystemNode, addOperations);
                    break;
                case IDENTITY_MANAGEMENT:
                    parseIdentityManagementConfig(reader, subsystemNode, addOperations);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    private void parseFederation(final XMLExtendedStreamReader reader, final ModelNode subsystemNode,
                                        final List<ModelNode> addOperations) throws XMLStreamException {
        ModelNode federationNode = parseConfig(reader, FEDERATION, FederationResourceDefinition.ALIAS.getName(), subsystemNode,
                                                      FederationResourceDefinition.INSTANCE.getAttributes(), addOperations);

        parseElement(new ElementParser() {
            @Override
            public void parse(final XMLExtendedStreamReader reader, final ModelElement element, final ModelNode parentNode,
                                     List<ModelNode> addOperations) throws XMLStreamException {
                switch (element) {
                    case KEY_STORE:
                        parseConfig(reader, KEY_STORE, KeyProviderResourceDefinition.SIGN_KEY_ALIAS.getName(), parentNode,
                                           KeyProviderResourceDefinition.INSTANCE.getAttributes(), addOperations);
                        break;
                    case SAML:
                        parseConfig(reader, SAML, null, parentNode, SAMLResourceDefinition.INSTANCE.getAttributes(),
                                           addOperations);
                        break;
                    case IDENTITY_PROVIDER:
                        parseIdentityProviderConfig(reader, parentNode, addOperations);
                        break;
                    case SERVICE_PROVIDER:
                        parseServiceProviderConfig(reader, parentNode, addOperations);
                        break;
                    default:
                        throw unexpectedElement(reader);
                }
            }
        }, FEDERATION, federationNode, reader, addOperations);
    }

    private void parseServiceProviderConfig(final XMLExtendedStreamReader reader, ModelNode federationNode,
                                                   final List<ModelNode> addOperations) throws XMLStreamException {
        ModelNode serviceProviderNode = parseConfig(reader, SERVICE_PROVIDER,
                                                           ServiceProviderResourceDefinition.ALIAS.getName(), federationNode,
                                                           ServiceProviderResourceDefinition.INSTANCE.getAttributes(), addOperations);

        parseElement(new ElementParser() {
            @Override
            public void parse(final XMLExtendedStreamReader reader, final ModelElement element, final ModelNode parentNode,
                                     List<ModelNode> addOperations) throws XMLStreamException {
                switch (element) {
                    case COMMON_HANDLER:
                        parseHandlerConfig(reader, parentNode, addOperations);
                        break;
                }
            }
        }, SERVICE_PROVIDER, serviceProviderNode, reader, addOperations);
    }

    private void parseHandlerConfig(final XMLExtendedStreamReader reader, final ModelNode entityProviderNode,
                                           final List<ModelNode> addOperations) throws XMLStreamException {
        ModelNode handlerNode = parseConfig(reader, COMMON_HANDLER, HandlerResourceDefinition.CLASS.getName(),
                                                   entityProviderNode, HandlerResourceDefinition.INSTANCE.getAttributes(), addOperations);

        parseElement(new ElementParser() {
            @Override
            public void parse(final XMLExtendedStreamReader reader, final ModelElement element, final ModelNode parentNode,
                                     List<ModelNode> addOperations) throws XMLStreamException {
                switch (element) {
                    case COMMON_HANDLER_PARAMETER:
                        parseConfig(reader, COMMON_HANDLER_PARAMETER, COMMON_NAME.getName(), parentNode,
                                           HandlerParameterResourceDefinition.INSTANCE.getAttributes(), addOperations);
                        break;
                    default:
                        throw unexpectedElement(reader);
                }
            }
        }, COMMON_HANDLER, handlerNode, reader, addOperations);
    }

    private void parseIdentityProviderConfig(final XMLExtendedStreamReader reader, final ModelNode federationNode,
                                                    final List<ModelNode> addOperations) throws XMLStreamException {
        ModelNode identityProviderNode = parseConfig(reader, IDENTITY_PROVIDER,
                                                            IdentityProviderResourceDefinition.ALIAS.getName(), federationNode,
                                                            IdentityProviderResourceDefinition.INSTANCE.getAttributes(), addOperations);

        parseElement(new ElementParser() {
            @Override
            public void parse(final XMLExtendedStreamReader reader, final ModelElement element, final ModelNode parentNode,
                                     List<ModelNode> addOperations) throws XMLStreamException {
                switch (element) {
                    case IDENTITY_PROVIDER_TRUST_DOMAIN:
                        parseConfig(reader, IDENTITY_PROVIDER_TRUST_DOMAIN, COMMON_NAME.getName(), parentNode,
                                           TrustDomainResourceDefinition.INSTANCE.getAttributes(), addOperations);
                        break;
                    case COMMON_HANDLER:
                        parseHandlerConfig(reader, parentNode, addOperations);
                        break;
                    default:
                        throw unexpectedElement(reader);
                }
            }
        }, IDENTITY_PROVIDER, identityProviderNode, reader, addOperations);
    }

    private void parseIdentityManagementConfig(final XMLExtendedStreamReader reader, final ModelNode parentNode,
                                                      final List<ModelNode> addOperations) throws XMLStreamException {
        ModelNode identityManagementNode = parseConfig(reader, IDENTITY_MANAGEMENT,
                                                              IdentityManagementResourceDefinition.ALIAS.getName(), parentNode,
                                                              IdentityManagementResourceDefinition.INSTANCE.getAttributes(), addOperations);

        parseElement(new ElementParser() {
            @Override
            public void parse(final XMLExtendedStreamReader reader, final ModelElement element, final ModelNode parentNode,
                                     List<ModelNode> addOperations) throws XMLStreamException {
                switch (element) {
                    case IDENTITY_CONFIGURATION:
                        parseIdentityConfigurationConfig(reader, parentNode, addOperations);
                        break;
                }
            }
        }, IDENTITY_MANAGEMENT, identityManagementNode, reader, addOperations);
    }

    private void parseIdentityConfigurationConfig(final XMLExtendedStreamReader reader, final ModelNode parentNode,
                                                         final List<ModelNode> addOperations) throws XMLStreamException {
        ModelNode identityConfigurationNode = parseConfig(reader, IDENTITY_CONFIGURATION, COMMON_NAME.getName(), parentNode,
                                                                 IdentityConfigurationResourceDefinition.INSTANCE.getAttributes(), addOperations);

        parseElement(new ElementParser() {
            @Override
            public void parse(final XMLExtendedStreamReader reader, final ModelElement element, final ModelNode parentNode,
                                     List<ModelNode> addOperations) throws XMLStreamException {
                switch (element) {
                    case JPA_STORE:
                        parseJPAStoreConfig(reader, parentNode, addOperations);
                        break;
                    case FILE_STORE:
                        parseFileStoreConfig(reader, parentNode, addOperations);
                        break;
                    case LDAP_STORE:
                        parseLDAPStoreConfig(reader, addOperations, parentNode);
                        break;
                }
            }
        }, IDENTITY_CONFIGURATION, identityConfigurationNode, reader, addOperations);
    }

    private void parseJPAStoreConfig(final XMLExtendedStreamReader reader, final ModelNode identityConfigurationNode,
                                            final List<ModelNode> addOperations) throws XMLStreamException {
        ModelNode jpaStoreNode = parseConfig(reader, JPA_STORE, null, identityConfigurationNode,
                                                    JPAStoreResourceDefinition.INSTANCE.getAttributes(), addOperations);

        parseElement(new ElementParser() {
            @Override
            public void parse(final XMLExtendedStreamReader reader, final ModelElement element, final ModelNode parentNode,
                                     List<ModelNode> addOperations) throws XMLStreamException {
                switch (element) {
                    case IDENTITY_STORE_CREDENTIAL_HANDLER:
                        parseCredentialHandlerConfig(reader, parentNode, addOperations);
                        break;
                    case SUPPORTED_TYPES:
                        parseSupportedTypesConfig(reader, parentNode, addOperations);
                        break;
                }
            }
        }, JPA_STORE, jpaStoreNode, reader, addOperations);
    }

    private void parseFileStoreConfig(final XMLExtendedStreamReader reader, final ModelNode identityManagementNode,
                                             final List<ModelNode> addOperations) throws XMLStreamException {
        ModelNode fileStoreNode = parseConfig(reader, FILE_STORE, null, identityManagementNode,
                                                     FileStoreResourceDefinition.INSTANCE.getAttributes(), addOperations);

        parseElement(new ElementParser() {
            @Override
            public void parse(final XMLExtendedStreamReader reader, final ModelElement element, final ModelNode parentNode,
                                     List<ModelNode> addOperations) throws XMLStreamException {
                switch (element) {
                    case IDENTITY_STORE_CREDENTIAL_HANDLER:
                        parseCredentialHandlerConfig(reader, parentNode, addOperations);
                        break;
                    case SUPPORTED_TYPES:
                        parseSupportedTypesConfig(reader, parentNode, addOperations);
                        break;
                }
            }
        }, FILE_STORE, fileStoreNode, reader, addOperations);
    }

    private void parseLDAPStoreConfig(final XMLExtendedStreamReader reader, final List<ModelNode> addOperations,
                                             final ModelNode identityManagementNode) throws XMLStreamException {
        ModelNode ldapStoreNode = parseConfig(reader, LDAP_STORE, null, identityManagementNode,
                                                     LDAPStoreResourceDefinition.INSTANCE.getAttributes(), addOperations);

        parseElement(new ElementParser() {
            @Override
            public void parse(final XMLExtendedStreamReader reader, final ModelElement element, final ModelNode parentNode,
                                     List<ModelNode> addOperations) throws XMLStreamException {
                switch (element) {
                    case IDENTITY_STORE_CREDENTIAL_HANDLER:
                        parseCredentialHandlerConfig(reader, parentNode, addOperations);
                        break;
                    case LDAP_STORE_MAPPING:
                        parseLDAPMappingConfig(reader, parentNode, addOperations);
                        break;
                    case SUPPORTED_TYPES:
                        parseSupportedTypesConfig(reader, parentNode, addOperations);
                        break;
                }
            }
        }, LDAP_STORE, ldapStoreNode, reader, addOperations);
    }

    private void parseLDAPMappingConfig(final XMLExtendedStreamReader reader, final ModelNode identityProviderNode,
                                               final List<ModelNode> addOperations) throws XMLStreamException {
        ModelNode ldapMappingConfig = parseConfig(reader, LDAP_STORE_MAPPING,
                                                         LDAPStoreMappingResourceDefinition.CLASS.getName(), identityProviderNode,
                                                         LDAPStoreMappingResourceDefinition.INSTANCE.getAttributes(), addOperations);

        parseElement(new ElementParser() {
            @Override
            public void parse(final XMLExtendedStreamReader reader, final ModelElement element, final ModelNode parentNode,
                                     List<ModelNode> addOperations) throws XMLStreamException {
                switch (element) {
                    case LDAP_STORE_ATTRIBUTE:
                        parseConfig(reader, LDAP_STORE_ATTRIBUTE, LDAPStoreAttributeResourceDefinition.NAME.getName(),
                                           parentNode, LDAPStoreAttributeResourceDefinition.INSTANCE.getAttributes(), addOperations);
                        break;
                }
            }
        }, LDAP_STORE_MAPPING, ldapMappingConfig, reader, addOperations);
    }

    private ModelNode parseCredentialHandlerConfig(XMLExtendedStreamReader reader, ModelNode identityProviderNode,
                                                          List<ModelNode> addOperations) throws XMLStreamException {
        return parseConfig(reader, IDENTITY_STORE_CREDENTIAL_HANDLER, CredentialHandlerResourceDefinition.CLASS.getName(),
                                  identityProviderNode, CredentialHandlerResourceDefinition.INSTANCE.getAttributes(), addOperations);
    }

    private ModelNode parseSupportedTypesConfig(final XMLExtendedStreamReader reader, final ModelNode identityStoreNode,
                                                       final List<ModelNode> addOperations) throws XMLStreamException {
        ModelNode supportedTypesNode = parseConfig(reader, SUPPORTED_TYPES, null, identityStoreNode,
                                                          SupportedTypesResourceDefinition.INSTANCE.getAttributes(), addOperations);

        parseElement(new ElementParser() {
            @Override
            public void parse(final XMLExtendedStreamReader reader, final ModelElement element, final ModelNode parentNode,
                                     List<ModelNode> addOperations) throws XMLStreamException {
                switch (element) {
                    case SUPPORTED_TYPE:
                        parseConfig(reader, SUPPORTED_TYPE, SupportedTypeResourceDefinition.COMMON_CLASS.getName(), parentNode,
                                           SupportedTypeResourceDefinition.INSTANCE.getAttributes(), addOperations);
                        break;
                }
            }
        }, SUPPORTED_TYPES, supportedTypesNode, reader, addOperations);

        return supportedTypesNode;
    }

    /**
     * Creates the root subsystem's root address.
     *
     * @return
     */
    private ModelNode createSubsystemRoot() {
        ModelNode subsystemAddress = new ModelNode();

        subsystemAddress.add(ModelDescriptionConstants.SUBSYSTEM, PicketLinkExtension.SUBSYSTEM_NAME);

        subsystemAddress.protect();

        return Util.getEmptyOperation(ADD, subsystemAddress);
    }

    /**
     * Reads a element from the stream considering the parameters.
     *
     * @param reader XMLExtendedStreamReader instance from which the elements are read.
     * @param xmlElement Name of the Model Element to be parsed.
     * @param key Name of the attribute to be used to as the key for the model.
     * @param addOperations List of operations.
     * @param lastNode Parent ModelNode instance.
     * @param attributes AttributeDefinition instances to be used to extract the attributes and populate the resulting model.
     *
     * @return A ModelNode instance populated.
     *
     * @throws XMLStreamException
     */
    private ModelNode parseConfig(XMLExtendedStreamReader reader, ModelElement xmlElement, String key, ModelNode lastNode,
                                         List<SimpleAttributeDefinition> attributes, List<ModelNode> addOperations) throws XMLStreamException {
        if (!reader.getLocalName().equals(xmlElement.getName())) {
            return null;
        }

        ModelNode modelNode = Util.getEmptyOperation(ADD, null);

        for (SimpleAttributeDefinition simpleAttributeDefinition : attributes) {
            simpleAttributeDefinition.parseAndSetParameter(
                                                                  reader.getAttributeValue("", simpleAttributeDefinition.getXmlName()), modelNode, reader);
        }

        if (key != null) {
            if (modelNode.hasDefined(key)) {
                modelNode.get(ModelDescriptionConstants.OP_ADDR).set(
                                                                            lastNode.clone().get(OP_ADDR).add(xmlElement.getName(), modelNode.get(key)));
            } else {
                modelNode.get(ModelDescriptionConstants.OP_ADDR).set(
                                                                            lastNode.clone().get(OP_ADDR).add(xmlElement.getName(), reader.getAttributeValue("", key)));
            }
        } else {
            modelNode.get(ModelDescriptionConstants.OP_ADDR).set(
                                                                        lastNode.clone().get(OP_ADDR).add(xmlElement.getName(), xmlElement.getName()));
        }

        addOperations.add(modelNode);

        return modelNode;
    }

    private void parseElement(final ElementParser parser, ModelElement parentElement, final ModelNode parentNode,
                                     final XMLExtendedStreamReader reader, final List<ModelNode> addOperations) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_DOCUMENT) {
            if (!reader.isStartElement()) {
                if (reader.isEndElement() && reader.getLocalName().equals(parentElement.getName())) {
                    break;
                }
                continue;
            }

            if (reader.getLocalName().equals(parentElement.getName())) {
                continue;
            }

            ModelElement element = ModelElement.forName(reader.getLocalName());

            if (element == null) {
                if (XMLElement.forName(reader.getLocalName()) != null) {
                    continue;
                }

                throw unexpectedElement(reader);
            }

            parser.parse(reader, element, parentNode, addOperations);
        }
    }

    private interface ElementParser {

        void parse(XMLExtendedStreamReader reader, ModelElement element, ModelNode parentNode, List<ModelNode> addOperations)
                throws XMLStreamException;
    }
}
