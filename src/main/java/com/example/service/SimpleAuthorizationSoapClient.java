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
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Corrected SOAP client that avoids double envelope wrapping
 */
@Service
public class SimpleAuthorizationSoapClient {

    private static final Logger logger = LoggerFactory.getLogger(SimpleAuthorizationSoapClient.class);

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
            Document requestDoc = createSoapBodyContent("isHealthy", null);
            String response = sendSoapRequest(requestDoc);
            
            // Parse response to extract boolean value
            DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
            Document doc = builder.parse(new java.io.ByteArrayInputStream(response.getBytes()));
            
            XPath xpath = xPathFactory.newXPath();
            String result = (String) xpath.evaluate("//return/text()", doc, XPathConstants.STRING);
            
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
            String response = sendSoapRequest(requestDoc);
            return parseUserIdentity(response);
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
            String response = sendSoapRequest(requestDoc);
            return parseListValues(response, "//ns2:Offices/ns2:ListValue");
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
            String response = sendSoapRequest(requestDoc);
            return parseUserRoles(response);
        } catch (Exception e) {
            logger.error("Error getting user roles for user: {}", userIdentityDto.getUserLoginName(), e);
            throw new RuntimeException("Failed to get user roles", e);
        }
    }

    private Document createSoapBodyContent(String operation, Element arg0) throws Exception {
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        // Create only the operation element (not full SOAP envelope)
        Element operationElement = doc.createElementNS("http://web.service.eas.citso.fsa.usda.gov", operation);
        doc.appendChild(operationElement);

        if (arg0 != null) {
            // Import the arg0 element to this document
            Node importedArg0 = doc.importNode(arg0, true);
            operationElement.appendChild(importedArg0);
        }

        return doc;
    }

    private Document createFindMatchingUserIdentityRequest(String usdaEauthId) throws Exception {
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        // Create operation element
        Element operationElement = doc.createElementNS("http://web.service.eas.citso.fsa.usda.gov", "findMatchingUserIdentity");
        doc.appendChild(operationElement);

        // Create arg0
        Element arg0 = doc.createElementNS("http://web.service.eas.citso.fsa.usda.gov", "arg0");
        operationElement.appendChild(arg0);

        // Create MapEntry
        Element mapEntry = doc.createElementNS("http://web.service.eas.citso.fsa.usda.gov", "MapEntry");
        arg0.appendChild(mapEntry);

        // Create Key and Value
        Element key = doc.createElementNS("http://web.service.eas.citso.fsa.usda.gov", "Key");
        key.setTextContent("usda_eauth_id");
        mapEntry.appendChild(key);

        Element value = doc.createElementNS("http://web.service.eas.citso.fsa.usda.gov", "Value");
        value.setTextContent(usdaEauthId);
        mapEntry.appendChild(value);

        return doc;
    }

    private Document createFindOfficesByEauthIdRequest(String usdaEauthId, String officeType) throws Exception {
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        // Create operation element
        Element operationElement = doc.createElementNS("http://web.service.eas.citso.fsa.usda.gov", "findOfficesByEauthId");
        doc.appendChild(operationElement);

        // Create arg0
        Element arg0 = doc.createElementNS("http://web.service.eas.citso.fsa.usda.gov", "arg0");
        operationElement.appendChild(arg0);

        // Create UsdaEauthId
        Element eauthIdElement = doc.createElementNS("http://web.service.eas.citso.fsa.usda.gov", "UsdaEauthId");
        eauthIdElement.setTextContent(usdaEauthId);
        arg0.appendChild(eauthIdElement);

        // Create OfficeType
        Element officeTypeElement = doc.createElementNS("http://web.service.eas.citso.fsa.usda.gov", "OfficeType");
        officeTypeElement.setTextContent(officeType);
        arg0.appendChild(officeTypeElement);

        return doc;
    }

    private Document createGetUserRolesRequest(UserIdentityDto userIdentityDto) throws Exception {
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        // Create operation element
        Element operationElement = doc.createElementNS("http://web.service.eas.citso.fsa.usda.gov", "getUserRoles");
        doc.appendChild(operationElement);

        // Create arg0
        Element arg0 = doc.createElementNS("http://web.service.eas.citso.fsa.usda.gov", "arg0");
        operationElement.appendChild(arg0);

        // Create UserIdentity
        Element userIdentityElement = doc.createElementNS("http://web.service.eas.citso.fsa.usda.gov", "UserIdentity");
        arg0.appendChild(userIdentityElement);

        // Create UserIdentity fields
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

    private String sendSoapRequest(Document requestDoc) {
        try {
            logger.debug("Sending SOAP request to: {}", serviceUrl);
            
            // Log the body content only (not full envelope)
            if (logger.isDebugEnabled()) {
                logger.debug("Request body content: {}", documentToString(requestDoc));
            }

            webServiceTemplate.setDefaultUri(serviceUrl);

            // Create DOMSource from request body content
            DOMSource requestSource = new DOMSource(requestDoc);

            // Send request - WebServiceTemplate will wrap it in proper SOAP envelope
            DOMSource responseSource = webServiceTemplate.sendSourceAndReceive(
                serviceUrl,
                requestSource,
                new SoapActionCallback(""),
                source -> (DOMSource) source
            );

            // Convert response to string
            Document responseDoc = (Document) responseSource.getNode();
            String response = documentToString(responseDoc);
            
            logger.debug("Response: {}", response);
            return response;
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

    private UserIdentityDto parseUserIdentity(String response) throws Exception {
        DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
        Document doc = builder.parse(new java.io.ByteArrayInputStream(response.getBytes()));
        
        XPath xpath = xPathFactory.newXPath();
        
        String authSysId = (String) xpath.evaluate("//AuthenticationSystemIdentifier/text()", doc, XPathConstants.STRING);
        String authzSysId = (String) xpath.evaluate("//AuthorizationSystemIdentifier/text()", doc, XPathConstants.STRING);
        String loginName = (String) xpath.evaluate("//UserLoginName/text()", doc, XPathConstants.STRING);
        
        return new UserIdentityDto(authSysId, authzSysId, loginName);
    }

    private List<String> parseListValues(String response, String xpathExpression) throws Exception {
        DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
        Document doc = builder.parse(new java.io.ByteArrayInputStream(response.getBytes()));
        
        XPath xpath = xPathFactory.newXPath();
        NodeList nodeList = (NodeList) xpath.evaluate(xpathExpression, doc, XPathConstants.NODESET);
        
        List<String> values = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            values.add(node.getTextContent());
        }
        
        return values;
    }

    private UserRolesResponseDto parseUserRoles(String response) throws Exception {
        DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
        Document doc = builder.parse(new java.io.ByteArrayInputStream(response.getBytes()));
        
        XPath xpath = xPathFactory.newXPath();
        
        // Parse roles
        List<String> roles = parseListValues(response, "//ns2:UserRoles/ns2:ListValue");
        
        // Parse user identity
        String authSysId = (String) xpath.evaluate("//ns2:UserIdentity/AuthenticationSystemIdentifier/text()", doc, XPathConstants.STRING);
        String authzSysId = (String) xpath.evaluate("//ns2:UserIdentity/AuthorizationSystemIdentifier/text()", doc, XPathConstants.STRING);
        String loginName = (String) xpath.evaluate("//ns2:UserIdentity/UserLoginName/text()", doc, XPathConstants.STRING);
        
        UserIdentityDto userIdentity = new UserIdentityDto(authSysId, authzSysId, loginName);
        
        return new UserRolesResponseDto(roles, userIdentity);
    }
}