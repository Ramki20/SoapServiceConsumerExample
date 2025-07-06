package com.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AuthorizationServiceExample {

    @Autowired
    private SimpleAuthorizationSoapClient authorizationSoapClient;

    /**
     * Example method showing how to use the SOAP client
     */
    public void demonstrateUsage() {
        try {
            // Check if service is healthy
            boolean isHealthy = authorizationSoapClient.isHealthy();
            System.out.println("Service is healthy: " + isHealthy);

            // Find user identity by eauth ID
            String eauthId = "28200310169021026877";
            UserIdentityDto userIdentity = authorizationSoapClient.findMatchingUserIdentity(eauthId);
            System.out.println("User Identity: " + userIdentity);

            // Find offices by eauth ID
            List<String> offices = authorizationSoapClient.findOfficesByEauthId("28692023052412555531317", "FLP");
            System.out.println("Offices: " + offices);

            // Get user roles
            UserRolesResponseDto userRoles = authorizationSoapClient.getUserRoles(userIdentity);
            System.out.println("User Roles: " + userRoles);
            
            // Print individual roles
            if (userRoles != null && userRoles.getUserRoles() != null) {
                for (String role : userRoles.getUserRoles()) {
                    System.out.println("Role: " + role);
                }
            }
        } catch (Exception e) {
            System.err.println("Error in demonstration: " + e.getMessage());
            e.printStackTrace();
        }
    }
}