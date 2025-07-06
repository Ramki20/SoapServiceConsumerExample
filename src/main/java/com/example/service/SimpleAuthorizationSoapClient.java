package com.example.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.core.SoapActionCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import javax.xml.XMLConstants;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Fixed Simple SOAP client with proper Source handling
 */
@Service
public class SimpleAuthorizationSoapClient {

    private static final Logger logger = LoggerFactory.getLogger(SimpleAuthorizationSoapClient.class);
    
    private static final String NAMESPACE_URI = "http://web.service.eas.citso.fsa.usda.gov";
    private static final String NAMESPACE_PREFIX = "web";

    @Value("${soap.service.url:http://10.29.60.95:8080/easws/sharedservice/AuthorizationSharedService}")
    private String serviceUrl;

    private final WebServiceTemplate webServiceTemplate;
    private final DocumentBuilderFactory docBuilderFactory;
    private final XPathFactory xPathFactory;
    private final TransformerFactory transformerFactory;

    public SimpleAuthorizationSoapClient(WebServiceTemplate webServiceTemplate) {
        this.webServiceTemplate = webServiceTemplate;
        this.docBuilderFactory = DocumentBuilderFactory.newInstance();
        this.docBuilderFactory.setNamespaceAware(true);
        this.xPathFactory = XPathFactory.newInstance();
        this.transformerFactory = TransformerFactory.newInstance();
    }

    /**
     * Check if the service is healthy
     */
    public boolean isHealthy() {
        try {
            Document requestDoc = createIsHealthyRequest();
            Node responseNode = sendSoapRequest(requestDoc);
            
            XPath xpath = xPathFactory.newXPath();
            String result = (String) xpath.evaluate("//return/text()", responseNode, XPathConstants.STRING);
            
            return Boolean.parseBoolean(result);
        } catch (Exception e) {
            logger.error("Error checking service health", e);
            return false;
        }
    }

    /**
     * Find matching user identity by eauth ID
     */
    public UserIdentityDto findMatchingUserIdentity(String usdaEauthId) {
        try {
            Document requestDoc = createFindMatchingUserIdentityRequest(usdaEauthId);
            Node responseNode = sendSoapRequest(requestDoc);
            return parseUserIdentity(responseNode);
        } catch (Exception e) {
            logger.error("Error finding matching user identity for eauth ID: {}", usdaEauthId, e);
            throw new RuntimeException("Failed to find matching user identity", e);
        }
    }

    /**
     * Find offices by eauth ID
     */
    public List<String> findOfficesByEauthId(String usdaEauthId, String officeType) {
        try {
            Document requestDoc = createFindOfficesByEauthIdRequest(usdaEauthId, officeType);
            Node responseNode = sendSoapRequest(requestDoc);
            return parseListValues(responseNode, "//ns2:Offices/ns2:ListValue");
        } catch (Exception e) {
            logger.error("Error finding offices for eauth ID: {} and office type: {}", usdaEauthId, officeType, e);
            throw new RuntimeException("Failed to find offices by eauth ID", e);
        }
    }

    /**
     * Get user roles
     */
    public UserRolesResponseDto getUserRoles(UserIdentityDto userIdentityDto) {
        try {
            Document requestDoc = createGetUserRolesRequest(userIdentityDto);
            Node responseNode = sendSoapRequest(requestDoc);
            return parseUserRoles(responseNode);
        } catch (Exception e) {
            logger.error("Error getting user roles for user: {}", userIdentityDto.getUserLoginName(), e);
            throw new RuntimeException("Failed to get user roles", e);
        }
    }

    private Document createIsHealthyRequest() throws Exception {
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        Element operationElement = doc.createElementNS(NAMESPACE_URI, NAMESPACE_PREFIX + ":isHealthy");
        doc.appendChild(operationElement);

        return doc;
    }

    private Document createFindMatchingUserIdentityRequest(String usdaEauthId) throws Exception {
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        Element operationElement = doc.createElementNS(NAMESPACE_URI, NAMESPACE_PREFIX + ":findMatchingUserIdentity");
        doc.appendChild(operationElement);

        Element arg0 = doc.createElement("arg0");
        operationElement.appendChild(arg0);

        Element mapEntry = doc.createElementNS(NAMESPACE_URI, NAMESPACE_PREFIX + ":MapEntry");
        arg0.appendChild(mapEntry);

        Element key = doc.createElementNS(NAMESPACE_URI, NAMESPACE_PREFIX + ":Key");
        key.setTextContent("usda_eauth_id");
        mapEntry.appendChild(key);

        Element value = doc.createElementNS(NAMESPACE_URI, NAMESPACE_PREFIX + ":Value");
        value.setTextContent(usdaEauthId);
        mapEntry.appendChild(value);

        return doc;
    }

    private Document createFindOfficesByEauthIdRequest(String usdaEauthId, String officeType) throws Exception {
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        Element operationElement = doc.createElementNS(NAMESPACE_URI, NAMESPACE_PREFIX + ":findOfficesByEauthId");
        doc.appendChild(operationElement);

        Element arg0 = doc.createElement("arg0");
        operationElement.appendChild(arg0);

        Element eauthIdElement = doc.createElementNS(NAMESPACE_URI, NAMESPACE_PREFIX + ":UsdaEauthId");
        eauthIdElement.setTextContent(usdaEauthId);
        arg0.appendChild(eauthIdElement);

        Element officeTypeElement = doc.createElementNS(NAMESPACE_URI, NAMESPACE_PREFIX + ":OfficeType");
        officeTypeElement.setTextContent(officeType);
        arg0.appendChild(officeTypeElement);

        return doc;
    }

    private Document createGetUserRolesRequest(UserIdentityDto userIdentityDto) throws Exception {
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        Element operationElement = doc.createElementNS(NAMESPACE_URI, NAMESPACE_PREFIX + ":getUserRoles");
        doc.appendChild(operationElement);

        Element arg0 = doc.createElement("arg0");
        operationElement.appendChild(arg0);

        Element userIdentityElement = doc.createElementNS(NAMESPACE_URI, NAMESPACE_PREFIX + ":UserIdentity");
        arg0.appendChild(userIdentityElement);

        Element authSysId = doc.createElement("AuthenticationSystemIdentifier");
        authSysId.setTextContent(userIdentityDto.getAuthenticationSystemIdentifier());
        userIdentityElement.appendChild(authSysId);

        Element authzSysId = doc.createElement("AuthorizationSystemIdentifier");
        authzSysId.setTextContent(userIdentityDto.getAuthorizationSystemIdentifier());
        userIdentityElement.appendChild(authzSysId);

        Element loginName = doc.createElement("UserLoginName");
        loginName.setTextContent(userIdentityDto.getUserLoginName());
        userIdentityElement.appendChild(loginName);

        return doc;
    }

    private Node sendSoapRequest(Document requestDoc) {
        try {
            logger.debug("Sending SOAP request to: {}", serviceUrl);
            
            if (logger.isDebugEnabled()) {
                logger.debug("Request body content: {}", documentToString(requestDoc));
            }

            webServiceTemplate.setDefaultUri(serviceUrl);
            DOMSource requestSource = new DOMSource(requestDoc);

            // Fixed: Properly handle the Source response
            Node responseNode = webServiceTemplate.sendSourceAndReceive(
                serviceUrl,
                requestSource,
                new SoapActionCallback(""),
                source -> {
                    try {
                        // Check if it's a DOMSource
                        if (source instanceof DOMSource) {
                            DOMSource domSource = (DOMSource) source;
                            Node node = domSource.getNode();
                            if (logger.isDebugEnabled()) {
                                logger.debug("Response: {}", nodeToString(node));
                            }
                            return node;
                        } else {
                            // Handle other source types by converting to DOM
                            DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
                            Document doc = builder.newDocument();
                            
                            // Create a transformer to convert Source to DOM
                            Transformer transformer = transformerFactory.newTransformer();
                            DOMResult result = new DOMResult(doc);
                            transformer.transform(source, result);
                            
                            Node node = doc.getDocumentElement();
                            if (logger.isDebugEnabled()) {
                                logger.debug("Response: {}", nodeToString(node));
                            }
                            return node;
                        }
                    } catch (Exception e) {
                        logger.error("Error processing response", e);
                        throw new RuntimeException("Failed to process response", e);
                    }
                }
            );

            return responseNode;
        } catch (Exception e) {
            logger.error("Error sending SOAP request", e);
            throw new RuntimeException("Failed to send SOAP request", e);
        }
    }

    private String documentToString(Document doc) {
        try {
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            return "Error converting document to string: " + e.getMessage();
        }
    }

    private String nodeToString(Node node) {
        try {
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(node), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            return "Error converting node to string: " + e.getMessage();
        }
    }

    private UserIdentityDto parseUserIdentity(Node responseNode) throws Exception {
        XPath xpath = xPathFactory.newXPath();
        
        String authSysId = (String) xpath.evaluate("//AuthenticationSystemIdentifier/text()", responseNode, XPathConstants.STRING);
        String authzSysId = (String) xpath.evaluate("//AuthorizationSystemIdentifier/text()", responseNode, XPathConstants.STRING);
        String loginName = (String) xpath.evaluate("//UserLoginName/text()", responseNode, XPathConstants.STRING);
        
        return new UserIdentityDto(authSysId, authzSysId, loginName);
    }

    private List<String> parseListValues(Node responseNode, String xpathExpression) throws Exception {
        XPath xpath = xPathFactory.newXPath();
        
        // Set up namespace context for xpath
        xpath.setNamespaceContext(new javax.xml.namespace.NamespaceContext() {
            @Override
            public String getNamespaceURI(String prefix) {
                switch (prefix) {
                    case "ns2":
                        return "http://web.service.eas.citso.fsa.usda.gov";
                    case "soap":
                        return "http://schemas.xmlsoap.org/soap/envelope/";
                    default:
                        return javax.xml.XMLConstants.NULL_NS_URI;
                }
            }

            @Override
            public String getPrefix(String namespaceURI) {
                return null;
            }

            @Override
            public java.util.Iterator<String> getPrefixes(String namespaceURI) {
                return null;
            }
        });
        
        NodeList nodeList = (NodeList) xpath.evaluate(xpathExpression, responseNode, XPathConstants.NODESET);
        
        List<String> values = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            values.add(node.getTextContent());
        }
        
        logger.debug("Parsed {} values from xpath: {}", values.size(), xpathExpression);
        logger.debug("Values: {}", values);
        
        return values;
    }

    private UserRolesResponseDto parseUserRoles(Node responseNode) throws Exception {
        XPath xpath = xPathFactory.newXPath();
        
        // Set up namespace context for xpath
        xpath.setNamespaceContext(new javax.xml.namespace.NamespaceContext() {
            @Override
            public String getNamespaceURI(String prefix) {
                switch (prefix) {
                    case "ns2":
                        return "http://web.service.eas.citso.fsa.usda.gov";
                    case "soap":
                        return "http://schemas.xmlsoap.org/soap/envelope/";
                    default:
                        return javax.xml.XMLConstants.NULL_NS_URI;
                }
            }

            @Override
            public String getPrefix(String namespaceURI) {
                return null;
            }

            @Override
            public java.util.Iterator<String> getPrefixes(String namespaceURI) {
                return null;
            }
        });
        
        // Parse roles
        List<String> roles = parseListValues(responseNode, "//ns2:UserRoles/ns2:ListValue");
        
        // Parse user identity
        String authSysId = (String) xpath.evaluate("//ns2:UserIdentity/AuthenticationSystemIdentifier/text()", responseNode, XPathConstants.STRING);
        String authzSysId = (String) xpath.evaluate("//ns2:UserIdentity/AuthorizationSystemIdentifier/text()", responseNode, XPathConstants.STRING);
        String loginName = (String) xpath.evaluate("//ns2:UserIdentity/UserLoginName/text()", responseNode, XPathConstants.STRING);
        
        UserIdentityDto userIdentity = new UserIdentityDto(authSysId, authzSysId, loginName);
        
        return new UserRolesResponseDto(roles, userIdentity);
    }
}