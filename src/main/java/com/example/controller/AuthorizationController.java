package com.example.controller;

import com.example.service.SimpleAuthorizationSoapClient;
import com.example.service.UserIdentityDto;
import com.example.service.UserRolesResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/authorization")
public class AuthorizationController {

    @Autowired
    private SimpleAuthorizationSoapClient authorizationSoapClient;

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Boolean> checkHealth() {
        try {
            boolean isHealthy = authorizationSoapClient.isHealthy();
            return ResponseEntity.ok(isHealthy);
        } catch (Exception e) {
            return ResponseEntity.ok(false);
        }
    }

    /**
     * Find matching user identity by eauth ID
     */
    @GetMapping("/user-identity/{eauthId}")
    public ResponseEntity<UserIdentityDto> findUserIdentity(@PathVariable String eauthId) {
        try {
            UserIdentityDto userIdentity = authorizationSoapClient.findMatchingUserIdentity(eauthId);
            return ResponseEntity.ok(userIdentity);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Find offices by eauth ID
     */
    @GetMapping("/offices/{eauthId}")
    public ResponseEntity<List<String>> findOffices(
            @PathVariable String eauthId,
            @RequestParam(defaultValue = "FLP") String officeType) {
        try {
            List<String> offices = authorizationSoapClient.findOfficesByEauthId(eauthId, officeType);
            return ResponseEntity.ok(offices);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get user roles
     */
    @PostMapping("/user-roles")
    public ResponseEntity<UserRolesResponseDto> getUserRoles(@RequestBody UserIdentityDto userIdentity) {
        try {
            UserRolesResponseDto userRoles = authorizationSoapClient.getUserRoles(userIdentity);
            return ResponseEntity.ok(userRoles);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get user roles by eauth ID (convenience method)
     */
    @GetMapping("/user-roles/{eauthId}")
    public ResponseEntity<UserRolesResponseDto> getUserRolesByEauthId(@PathVariable String eauthId) {
        try {
            // First get user identity
            UserIdentityDto userIdentity = authorizationSoapClient.findMatchingUserIdentity(eauthId);
            
            // Then get user roles
            UserRolesResponseDto userRoles = authorizationSoapClient.getUserRoles(userIdentity);
            return ResponseEntity.ok(userRoles);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}