/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.saml.common.util.xml;

import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.SecurityActions;
import org.keycloak.saml.common.util.SystemPropertiesUtil;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class DocumentFactory {

    private static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    private static DocumentBuilderFactory documentBuilderFactory;

    public static final String feature_external_general_entities = "http://xml.org/sax/features/external-general-entities";
    public static final String feature_external_parameter_entities = "http://xml.org/sax/features/external-parameter-entities";
    public static final String feature_disallow_doctype_decl = "http://apache.org/xml/features/disallow-doctype-decl";

    private static final ThreadLocal<DocumentBuilder> XML_DOCUMENT_BUILDER = new ThreadLocal<DocumentBuilder>() {
        @Override
        protected DocumentBuilder initialValue() {
            DocumentBuilderFactory factory = getDocumentBuilderFactory();
            try {
                return factory.newDocumentBuilder();
            } catch (ParserConfigurationException ex) {
                throw new RuntimeException(ex);
            }
        }
    };

    public static Document createDocument() throws ConfigurationException {
        DocumentBuilder builder;
        try {
            builder = getDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new ConfigurationException(e);
        }
        return builder.newDocument();
    }

    public static Document createDocumentWithBaseNamespace(String baseNamespace, String localPart) throws ProcessingException {
        try {
            DocumentBuilder builder = getDocumentBuilder();
            return builder.getDOMImplementation().createDocument(baseNamespace, localPart, null);
        } catch (DOMException e) {
            throw logger.processingError(e);
        } catch (ParserConfigurationException e) {
            throw logger.processingError(e);
        }
    }

    public static DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilder res = XML_DOCUMENT_BUILDER.get();
        res.reset();
        return res;
    }

    private static DocumentBuilderFactory getDocumentBuilderFactory() {
        if (documentBuilderFactory == null) {
            synchronized (DocumentFactory.class) {
                if (documentBuilderFactory == null) {
                    boolean tccl_jaxp = SystemPropertiesUtil.getSystemProperty(GeneralConstants.TCCL_JAXP, "false")
                            .equalsIgnoreCase("true");
                    ClassLoader prevTCCL = SecurityActions.getTCCL();
                    try {
                        if (tccl_jaxp) {
                            SecurityActions.setTCCL(DocumentFactory.class.getClassLoader());
                        }
                        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                        factory.setNamespaceAware(true);
                        factory.setXIncludeAware(false);
                        String feature = "";
                        try {
                            feature = feature_disallow_doctype_decl;
                            factory.setFeature(feature, true);
                            feature = feature_external_general_entities;
                            factory.setFeature(feature, false);
                            feature = feature_external_parameter_entities;
                            factory.setFeature(feature, false);
                        } catch (ParserConfigurationException e) {
                            throw logger.parserFeatureNotSupported(feature);
                        }
                        documentBuilderFactory = factory;
                    } finally {
                        if (tccl_jaxp) {
                            SecurityActions.setTCCL(prevTCCL);
                        }
                    }
                }
            }
        }
        return documentBuilderFactory;
    }
}
