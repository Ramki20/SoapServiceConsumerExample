package com.example.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.test.client.MockWebServiceServer;
import org.springframework.ws.transport.http.HttpUrlConnectionMessageSender;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.ws.test.client.RequestMatchers.*;
import static org.springframework.ws.test.client.ResponseCreators.*;

@SpringBootTest
@SpringJUnitConfig
public class AuthorizationSoapClientTest {

    private SimpleAuthorizationSoapClient soapClient;
    private MockWebServiceServer mockServer;
    private WebServiceTemplate webServiceTemplate;

    @BeforeEach
    void setUp() {
        // Create WebServiceTemplate with proper configuration
        webServiceTemplate = new WebServiceTemplate();
        webServiceTemplate.setDefaultUri("http://localhost:8080/mock-soap-service");
        webServiceTemplate.setMessageSender(new HttpUrlConnectionMessageSender());
        
        // Create the SOAP client with the configured template
        soapClient = new SimpleAuthorizationSoapClient(webServiceTemplate);
        
        // Create mock server from the template
        mockServer = MockWebServiceServer.createServer(webServiceTemplate);
    }

    @Test
    void testIsHealthy_Success() {
        // Mock response
        String responsePayload = """
            <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
               <soap:Body>
                  <ns2:isHealthyResponse xmlns:ns2="http://web.service.eas.citso.fsa.usda.gov">
                     <return>true</return>
                  </ns2:isHealthyResponse>
               </soap:Body>
            </soap:Envelope>
            """;

        Source responseSource = new StreamSource(new java.io.StringReader(responsePayload));
        
        mockServer.expect(anything())
                .andRespond(withPayload(responseSource));

        // Execute test
        boolean result = soapClient.isHealthy();

        // Verify
        assertTrue(result);
        mockServer.verify();
    }

    @Test
    void testFindMatchingUserIdentity_Success() {
        // Mock response
        String responsePayload = """
            <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
               <soap:Body>
                  <ns2:findMatchingUserIdentityResponse xmlns:ns2="http://web.service.eas.citso.fsa.usda.gov">
                     <return>
                        <ns2:UserIdentity>
                           <AuthenticationSystemIdentifier>28200310169021026877</AuthenticationSystemIdentifier>
                           <AuthorizationSystemIdentifier>-1452175789</AuthorizationSystemIdentifier>
                           <UserLoginName>emp0007966</UserLoginName>
                        </ns2:UserIdentity>
                     </return>
                  </ns2:findMatchingUserIdentityResponse>
               </soap:Body>
            </soap:Envelope>
            """;

        Source responseSource = new StreamSource(new java.io.StringReader(responsePayload));
        
        mockServer.expect(anything())
                .andRespond(withPayload(responseSource));

        // Execute test
        UserIdentityDto result = soapClient.findMatchingUserIdentity("28200310169021026877");

        // Verify
        assertNotNull(result);
        assertEquals("28200310169021026877", result.getAuthenticationSystemIdentifier());
        assertEquals("-1452175789", result.getAuthorizationSystemIdentifier());
        assertEquals("emp0007966", result.getUserLoginName());
        mockServer.verify();
    }

    @Test
    void testFindOfficesByEauthId_Success() {
        // Mock response
        String responsePayload = """
            <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
               <soap:Body>
                  <ns2:findOfficesByEauthIdResponse xmlns:ns2="http://web.service.eas.citso.fsa.usda.gov">
                     <return>
                        <ns2:Offices>
                           <ns2:ListValue>47310</ns2:ListValue>
                           <ns2:ListValue>47318</ns2:ListValue>
                           <ns2:ListValue>47321</ns2:ListValue>
                        </ns2:Offices>
                     </return>
                  </ns2:findOfficesByEauthIdResponse>
               </soap:Body>
            </soap:Envelope>
            """;

        Source responseSource = new StreamSource(new java.io.StringReader(responsePayload));
        
        mockServer.expect(anything())
                .andRespond(withPayload(responseSource));

        // Execute test
        List<String> result = soapClient.findOfficesByEauthId("28692023052412555531317", "FLP");

        // Verify
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains("47310"));
        assertTrue(result.contains("47318"));
        assertTrue(result.contains("47321"));
        mockServer.verify();
    }

    @Test
    void testGetUserRoles_Success() {
        // Mock response
        String responsePayload = """
            <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
               <soap:Body>
                  <ns2:getUserRolesResponse xmlns:ns2="http://web.service.eas.citso.fsa.usda.gov">
                     <return>
                        <ns2:UserRoles>
                           <ns2:ListValue>app.fsa.flp.dls.fsfl</ns2:ListValue>
                           <ns2:ListValue>app.fsa.flp.dls.lm</ns2:ListValue>
                           <ns2:ListValue>app.fsa.flp.dls.lm.cm</ns2:ListValue>
                        </ns2:UserRoles>
                        <ns2:UserIdentity>
                           <AuthenticationSystemIdentifier>28200310169021026877</AuthenticationSystemIdentifier>
                           <AuthorizationSystemIdentifier>-1452175789</AuthorizationSystemIdentifier>
                           <UserLoginName>emp0007966</UserLoginName>
                        </ns2:UserIdentity>
                     </return>
                  </ns2:getUserRolesResponse>
               </soap:Body>
            </soap:Envelope>
            """;

        Source responseSource = new StreamSource(new java.io.StringReader(responsePayload));
        
        mockServer.expect(anything())
                .andRespond(withPayload(responseSource));

        // Create test user identity
        UserIdentityDto userIdentity = new UserIdentityDto(
            "28200310169021026877", 
            "-1452175789", 
            "emp0007966"
        );

        // Execute test
        UserRolesResponseDto result = soapClient.getUserRoles(userIdentity);

        // Verify
        assertNotNull(result);
        assertNotNull(result.getUserRoles());
        assertEquals(3, result.getUserRoles().size());
        assertTrue(result.getUserRoles().contains("app.fsa.flp.dls.fsfl"));
        assertNotNull(result.getUserIdentity());
        assertEquals("emp0007966", result.getUserIdentity().getUserLoginName());
        mockServer.verify();
    }

    @Test
    void testIsHealthy_ServiceDown() {
        // Mock a SOAP fault response
        String faultResponse = """
            <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
               <soap:Body>
                  <soap:Fault>
                     <faultcode>Server</faultcode>
                     <faultstring>Service unavailable</faultstring>
                  </soap:Fault>
               </soap:Body>
            </soap:Envelope>
            """;

        Source responseSource = new StreamSource(new java.io.StringReader(faultResponse));
        
        mockServer.expect(anything())
                .andRespond(withPayload(responseSource));

        // Execute test - should return false when service returns fault
        boolean result = soapClient.isHealthy();

        // Verify
        assertFalse(result);
        mockServer.verify();
    }
}
