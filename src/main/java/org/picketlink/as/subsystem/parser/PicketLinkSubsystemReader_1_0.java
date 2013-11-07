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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

/**
 * <p>
 * XML Reader for the subsystem schema, version 1.0.
 * </p>
 * 
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class PicketLinkSubsystemReader_1_0 implements XMLStreamConstants, XMLElementReader<List<ModelNode>> {

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.staxmapper.XMLElementReader#readElement(org.jboss.staxmapper.XMLExtendedStreamReader, java.lang.Object)
     */
    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        requireNoAttributes(reader);

        Namespace nameSpace = Namespace.forUri(reader.getNamespaceURI());

        ModelNode subsystemNode = createSubsystemRoot();

        list.add(subsystemNode);

        switch (nameSpace) {
            case PICKETLINK_1_0:
                this.readElement_1_0(reader, list, subsystemNode);
                break;
            default:
                throw unexpectedElement(reader);
        }

    }

    /**
     * Parses the PicketLink subsystem configuration according to the XSD version 1.0.
     * 
     * @param reader
     * @param list
     * @throws XMLStreamException
     */
    private void readElement_1_0(XMLExtendedStreamReader reader, List<ModelNode> list, ModelNode parentNode)
            throws XMLStreamException {
        if (Namespace.PICKETLINK_1_0 != Namespace.forUri(reader.getNamespaceURI())) {
            throw unexpectedElement(reader);
        }

        ModelNode federationNode = null;
        ModelNode lastProviderNode = null;
        ModelNode lastHandlerNode = null;
        
        ModelNode identityManagementNode = null;
        ModelNode identityConfigurationNode = null;
        ModelNode lastIdentityStoreNode = null;
        ModelNode lastLDAPMappingNode = null;
        ModelNode lastFeatures = null;

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
                case IDENTITY_MANAGEMENT:
                    identityManagementNode = parseIdentityManagementConfig(reader, list, parentNode);
                    break;
                case IDENTITY_CONFIGURATION:
                    identityConfigurationNode = parseIdentityConfigurationConfig(reader, list, identityManagementNode);
                    break;
                case JPA_STORE:
                    lastIdentityStoreNode = parseJPAStoreConfig(reader, list, identityConfigurationNode);
                    break;
                case FILE_STORE:
                    lastIdentityStoreNode = parseFileStoreConfig(reader, list, identityConfigurationNode);
                    break;
                case LDAP_STORE:
                    lastIdentityStoreNode = parseLDAPStoreConfig(reader, list, identityConfigurationNode);
                    break;
                case LDAP_STORE_MAPPING:
                    lastLDAPMappingNode = parseLDAPMappingConfig(reader, list, lastIdentityStoreNode);
                    break;
                case LDAP_STORE_ATTRIBUTE:
                    parseLDAPAttributeConfig(reader, list, lastLDAPMappingNode);
                    break;
                case IDENTITY_STORE_CREDENTIAL_HANDLER:
                    parseCredentialHandlerConfig(reader, list, lastIdentityStoreNode);
                    break;
                case SUPPORTED_TYPES:
                    lastFeatures = parseSupportedTypesConfig(reader, list, lastIdentityStoreNode);
                    break;
                case SUPPORTED_TYPE:
                    parseSupportedTypeConfig(reader, list, lastFeatures);
                    break;
                default:
                    unexpectedElement(reader);
            }
        }
    }

    private ModelNode parseIdentityManagementConfig(XMLExtendedStreamReader reader, List<ModelNode> list, ModelNode parentNode)
            throws XMLStreamException {
        return parseConfig(reader, ModelElement.IDENTITY_MANAGEMENT, IdentityManagementResourceDefinition.ALIAS.getName(), list, parentNode, IdentityManagementResourceDefinition.INSTANCE.getAttributes());
    }

    private ModelNode parseIdentityConfigurationConfig(XMLExtendedStreamReader reader, List<ModelNode> list, ModelNode parentNode)
            throws XMLStreamException {
        return parseConfig(reader, ModelElement.IDENTITY_CONFIGURATION, IdentityConfigurationResourceDefinition.NAME.getName(), list, parentNode, IdentityConfigurationResourceDefinition.INSTANCE.getAttributes());
    }

    private ModelNode parseJPAStoreConfig(XMLExtendedStreamReader reader, List<ModelNode> list, ModelNode identityManagementNode)
            throws XMLStreamException {
        return parseConfig(reader, ModelElement.JPA_STORE, null, list, identityManagementNode, JPAStoreResourceDefinition.INSTANCE.getAttributes());
    }

    private ModelNode parseFileStoreConfig(XMLExtendedStreamReader reader, List<ModelNode> list, ModelNode identityManagementNode)
            throws XMLStreamException {
        return parseConfig(reader, ModelElement.FILE_STORE, null, list, identityManagementNode, FileStoreResourceDefinition.INSTANCE.getAttributes());
    }

    private ModelNode parseLDAPStoreConfig(XMLExtendedStreamReader reader, List<ModelNode> list, ModelNode identityManagementNode)
            throws XMLStreamException {
        return parseConfig(reader, ModelElement.LDAP_STORE, null, list, identityManagementNode, LDAPStoreResourceDefinition.INSTANCE.getAttributes());
    }

    private ModelNode parseLDAPMappingConfig(XMLExtendedStreamReader reader, List<ModelNode> list, ModelNode identityProviderNode)
            throws XMLStreamException {
        return parseConfig(reader, ModelElement.LDAP_STORE_MAPPING, LDAPStoreMappingResourceDefinition.CLASS.getName(), list, identityProviderNode,
                LDAPStoreMappingResourceDefinition.INSTANCE.getAttributes());
    }

    private ModelNode parseCredentialHandlerConfig(XMLExtendedStreamReader reader, List<ModelNode> list, ModelNode identityProviderNode)
            throws XMLStreamException {
        return parseConfig(reader, ModelElement.IDENTITY_STORE_CREDENTIAL_HANDLER, CredentialHandlerResourceDefinition.CLASS.getName(), list, identityProviderNode,
                CredentialHandlerResourceDefinition.INSTANCE.getAttributes());
    }

    private ModelNode parseLDAPAttributeConfig(XMLExtendedStreamReader reader, List<ModelNode> list, ModelNode identityProviderNode)
            throws XMLStreamException {
        return parseConfig(reader, ModelElement.LDAP_STORE_ATTRIBUTE, LDAPStoreAttributeResourceDefinition.NAME.getName(), list, identityProviderNode,
                LDAPStoreAttributeResourceDefinition.INSTANCE.getAttributes());
    }

    private ModelNode parseSupportedTypesConfig(XMLExtendedStreamReader reader, List<ModelNode> list, ModelNode identityStoreNode)
            throws XMLStreamException {
        return parseConfig(reader, ModelElement.SUPPORTED_TYPES, null, list, identityStoreNode, SupportedTypesResourceDefinition.INSTANCE.getAttributes());
    }

    private void parseSupportedTypeConfig(XMLExtendedStreamReader reader, List<ModelNode> list, ModelNode identityStoreNode)
            throws XMLStreamException {
        parseConfig(reader, ModelElement.SUPPORTED_TYPE, SupportedTypeResourceDefinition.COMMON_CLASS.getName(), list, identityStoreNode, SupportedTypeResourceDefinition.INSTANCE.getAttributes());
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

        return Util.getEmptyOperation(ModelDescriptionConstants.ADD, subsystemAddress);
    }

    /**
     * Reads a element from the stream considering the parameters.
     * 
     * @param reader XMLExtendedStreamReader instance from which the elements are read.
     * @param xmlElement Name of the Model Element to be parsed.
     * @param key Name of the attribute to be used to as the key for the model.
     * @param list List of operations.
     * @param lastNode Parent ModelNode instance.
     * @param attributes AttributeDefinition instances to be used to extract the attributes and populate the resulting model.
     * 
     * @return A ModelNode instance populated.
     * 
     * @throws XMLStreamException
     */
    private ModelNode parseConfig(XMLExtendedStreamReader reader, ModelElement xmlElement, String key, List<ModelNode> list,
            ModelNode lastNode, List<SimpleAttributeDefinition> attributes) throws XMLStreamException {
        if (!reader.getLocalName().equals(xmlElement.getName())) {
            return null;
        }

        ModelNode modelNode = Util.getEmptyOperation(ModelDescriptionConstants.ADD, null);

        for (SimpleAttributeDefinition simpleAttributeDefinition : attributes) {
            simpleAttributeDefinition.parseAndSetParameter(
                    reader.getAttributeValue("", simpleAttributeDefinition.getXmlName()), modelNode, reader);
        }

        if (key != null) {
            modelNode.get(ModelDescriptionConstants.OP_ADDR).set(
                    lastNode.clone().get(OP_ADDR).add(xmlElement.getName(), modelNode.get(key)));
        } else {
            modelNode.get(ModelDescriptionConstants.OP_ADDR).set(
                    lastNode.clone().get(OP_ADDR).add(xmlElement.getName(), xmlElement.getName()));
        }

        list.add(modelNode);

        return modelNode;
    }
}