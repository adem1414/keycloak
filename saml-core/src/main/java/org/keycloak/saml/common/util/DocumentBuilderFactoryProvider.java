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
package org.keycloak.saml.common.util;

import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;
import org.keycloak.saml.common.constants.GeneralConstants;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Provides a securely configured singleton instance of {@link DocumentBuilderFactory}.
 *
 * This provider ensures that the factory is configured to prevent common XML external entity (XXE) attacks.
 *
 * @author Anil.Saldhana@redhat.com
 * @since 2023-11-20
 */
public class DocumentBuilderFactoryProvider {

    private static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    private static volatile DocumentBuilderFactory documentBuilderFactory;

    public static final String FEATURE_EXTERNAL_GENERAL_ENTITIES = "http://xml.org/sax/features/external-general-entities";
    public static final String FEATURE_EXTERNAL_PARAMETER_ENTITIES = "http://xml.org/sax/features/external-parameter-entities";
    public static final String FEATURE_DISALLOW_DOCTYPE_DECL = "http://apache.org/xml/features/disallow-doctype-decl";

    private DocumentBuilderFactoryProvider() {
        // private constructor to prevent instantiation
    }

    /**
     * <p>Creates a namespace aware {@link DocumentBuilderFactory}. The returned instance is cached and shared between
     * different threads.</p>
     *
     * The factory is configured with security features to prevent XXE attacks.
     *
     * @return A singleton, namespace-aware, and securely configured {@link DocumentBuilderFactory}.
     */
    public static DocumentBuilderFactory getDocumentBuilderFactory() {
        if (documentBuilderFactory == null) {
            synchronized (DocumentBuilderFactoryProvider.class) {
                if (documentBuilderFactory == null) {
                    boolean tccl_jaxp = SystemPropertiesUtil.getSystemProperty(GeneralConstants.TCCL_JAXP, "false")
                            .equalsIgnoreCase("true");
                    ClassLoader prevTCCL = SecurityActions.getTCCL();
                    try {
                        if (tccl_jaxp) {
                            SecurityActions.setTCCL(DocumentBuilderFactoryProvider.class.getClassLoader());
                        }
                        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                        factory.setNamespaceAware(true);
                        factory.setXIncludeAware(false);
                        String feature = "";
                        try {
                            feature = FEATURE_DISALLOW_DOCTYPE_DECL;
                            factory.setFeature(feature, true);
                            feature = FEATURE_EXTERNAL_GENERAL_ENTITIES;
                            factory.setFeature(feature, false);
                            feature = FEATURE_EXTERNAL_PARAMETER_ENTITIES;
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
