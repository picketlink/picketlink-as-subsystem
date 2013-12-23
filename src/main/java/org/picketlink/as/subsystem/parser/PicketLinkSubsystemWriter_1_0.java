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

import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.picketlink.as.subsystem.Namespace;
import org.picketlink.as.subsystem.model.ModelElement;
import org.picketlink.as.subsystem.model.XMLElement;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.picketlink.as.subsystem.model.ModelElement.COMMON_HANDLER;
import static org.picketlink.as.subsystem.model.ModelElement.COMMON_HANDLER_PARAMETER;
import static org.picketlink.as.subsystem.model.ModelElement.COMMON_NAME;
import static org.picketlink.as.subsystem.model.ModelElement.FEDERATION;
import static org.picketlink.as.subsystem.model.ModelElement.FILE_STORE;
import static org.picketlink.as.subsystem.model.ModelElement.IDENTITY_CONFIGURATION;
import static org.picketlink.as.subsystem.model.ModelElement.IDENTITY_MANAGEMENT;
import static org.picketlink.as.subsystem.model.ModelElement.IDENTITY_PROVIDER;
import static org.picketlink.as.subsystem.model.ModelElement.IDENTITY_PROVIDER_SAML_METADATA;
import static org.picketlink.as.subsystem.model.ModelElement.IDENTITY_PROVIDER_SAML_METADATA_ORGANIZATION;
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
import static org.picketlink.as.subsystem.model.XMLElement.HANDLERS;
import static org.picketlink.as.subsystem.model.XMLElement.SERVICE_PROVIDERS;

/**
 * <p> XML Writer for the subsystem schema, version 1.0. </p>
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class PicketLinkSubsystemWriter_1_0 implements XMLStreamConstants, XMLElementWriter<SubsystemMarshallingContext> {

    private static final Map<String, ModelXMLElementWriter> writers = new HashMap<String, ModelXMLElementWriter>();

    static {
        // identity management elements writers
        registerWriter(IDENTITY_MANAGEMENT, COMMON_NAME);
        registerWriter(IDENTITY_CONFIGURATION, COMMON_NAME);
        registerWriter(JPA_STORE);
        registerWriter(FILE_STORE);
        registerWriter(LDAP_STORE);
        registerWriter(LDAP_STORE_MAPPING, XMLElement.LDAP_MAPPINGS);
        registerWriter(LDAP_STORE_ATTRIBUTE);
        registerWriter(SUPPORTED_TYPES);
        registerWriter(SUPPORTED_TYPE);
        registerWriter(IDENTITY_STORE_CREDENTIAL_HANDLER, XMLElement.IDENTITY_STORE_CREDENTIAL_HANDLERS);

        // federation elements writers
        registerWriter(FEDERATION);
        registerWriter(IDENTITY_PROVIDER);
        registerWriter(KEY_STORE);
        registerWriter(IDENTITY_PROVIDER_SAML_METADATA);
        registerWriter(IDENTITY_PROVIDER_SAML_METADATA_ORGANIZATION);
        registerWriter(IDENTITY_PROVIDER_TRUST_DOMAIN, XMLElement.TRUST, COMMON_NAME);
        registerWriter(COMMON_HANDLER, HANDLERS);
        registerWriter(COMMON_HANDLER_PARAMETER, COMMON_NAME);
        registerWriter(SERVICE_PROVIDER, SERVICE_PROVIDERS);
        registerWriter(SAML);
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        if (!context.getModelNode().isDefined()) {
            return;
        }

        context.startSubsystemElement(Namespace.CURRENT.getUri(), false);

        List<ModelNode> federation = context.getModelNode().asList();

        for (ModelNode modelNode : federation) {
            String modelName = modelNode.asProperty().getName();

            if (modelName.equals(FEDERATION.getName())) {
                writers.get(FEDERATION.getName()).write(writer, modelNode);
            } else if (modelName.equals(IDENTITY_MANAGEMENT.getName())) {
                writers.get(IDENTITY_MANAGEMENT.getName()).write(writer, modelNode);
            } else {
                throw new XMLStreamException("Unexpected element [" + modelName + "]");
            }
        }

        // End subsystem
        writer.writeEndElement();
    }

    private static void registerWriter(final ModelElement element, final ModelElement keyAttribute) {
        writers.put(element.getName(), new ModelXMLElementWriter(element, keyAttribute.getName(), writers));
    }

    private static void registerWriter(final ModelElement element) {
        writers.put(element.getName(), new ModelXMLElementWriter(element, writers));
    }

    private static void registerWriter(final ModelElement element, final XMLElement parent) {
        writers.put(element.getName(), new ModelXMLElementWriter(element, parent, writers));
    }

    private static void registerWriter(final ModelElement element, final XMLElement parent, final ModelElement keyAttribute) {
        writers.put(element.getName(), new ModelXMLElementWriter(element, parent, keyAttribute.getName(), writers));
    }
}
