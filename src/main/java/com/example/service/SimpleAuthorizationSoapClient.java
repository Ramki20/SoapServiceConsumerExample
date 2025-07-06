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

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Working SOAP client using WebServiceTemplate with correct method signatures
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
            String soapRequest = """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" 
                                  xmlns:web="http://web.service.eas.citso.fsa.usda.gov">
                   <soapenv:Header/>
                   <soapenv:Body>
                      <web:isHealthy/>
                   </soapenv:Body>
                </soapenv:Envelope>
                """;

            String response = sendSoapRequest(soapRequest);
            
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
            String soapRequest = String.format("""
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" 
                                  xmlns:web="http://web.service.eas.citso.fsa.usda.gov">
                   <soapenv:Header/>
                   <soapenv:Body>
                      <web:findMatchingUserIdentity>
                         <arg0>
                             <web:MapEntry>
                                 <web:Key>usda_eauth_id</web:Key>
                                 <web:Value>%s</web:Value>
                             </web:MapEntry>
                         </arg0>
                      </web:findMatchingUserIdentity>
                   </soapenv:Body>
                </soapenv:Envelope>
                """, usdaEauthId);

            String response = sendSoapRequest(soapRequest);
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
            String soapRequest = String.format("""
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" 
                                  xmlns:web="http://web.service.eas.citso.fsa.usda.gov">
                   <soapenv:Header/>
                   <soapenv:Body>
                      <web:findOfficesByEauthId>
                         <arg0>
                             <web:UsdaEauthId>%s</web:UsdaEauthId>
                             <web:OfficeType>%s</web:OfficeType>
                         </arg0>
                      </web:findOfficesByEauthId>
                   </soapenv:Body>
                </soapenv:Envelope>
                """, usdaEauthId, officeType);

            String response = sendSoapRequest(soapRequest);
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
            String soapRequest = String.format("""
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" 
                                  xmlns:web="http://web.service.eas.citso.fsa.usda.gov">
                   <soapenv:Header/>
                   <soapenv:Body>
                      <web:getUserRoles>
                         <arg0>
                            <web:UserIdentity>
                               <AuthenticationSystemIdentifier>%s</AuthenticationSystemIdentifier>
                               <AuthorizationSystemIdentifier>%s</AuthorizationSystemIdentifier>
                               <UserLoginName>%s</UserLoginName>
                            </web:UserIdentity>
                         </arg0>
                      </web:getUserRoles>
                   </soapenv:Body>
                </soapenv:Envelope>
                """, 
                userIdentityDto.getAuthenticationSystemIdentifier(),
                userIdentityDto.getAuthorizationSystemIdentifier(),
                userIdentityDto.getUserLoginName());

            String response = sendSoapRequest(soapRequest);
            return parseUserRoles(response);
        } catch (Exception e) {
            logger.error("Error getting user roles for user: {}", userIdentityDto.getUserLoginName(), e);
            throw new RuntimeException("Failed to get user roles", e);
        }
    }

    private String sendSoapRequest(String soapRequest) {
        try {
            logger.debug("Sending SOAP request to: {}", serviceUrl);
            logger.debug("Request: {}", soapRequest);

            webServiceTemplate.setDefaultUri(serviceUrl);

            // Convert string to Document
            DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
            Document requestDoc = builder.parse(new java.io.ByteArrayInputStream(soapRequest.getBytes()));

            // Create DOMSource from request
            DOMSource requestSource = new DOMSource(requestDoc);

            // Send request and receive response using the correct method signature
            DOMSource responseSource = webServiceTemplate.sendSourceAndReceive(
                serviceUrl,
                requestSource,
                new SoapActionCallback(""),
                source -> (DOMSource) source
            );

            // Convert response to string
            Document responseDoc = (Document) responseSource.getNode();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(responseDoc), new StreamResult(writer));
            String response = writer.toString();
            
            logger.debug("Response: {}", response);
            return response;
        } catch (Exception e) {
            logger.error("Error sending SOAP request", e);
            throw new RuntimeException("Failed to send SOAP request", e);
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