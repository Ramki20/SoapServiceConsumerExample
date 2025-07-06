package com.example.service;

import java.util.List;

/**
 * User Identity model
 */
public class UserIdentity {
    private String authenticationSystemIdentifier;
    private String authorizationSystemIdentifier;
    private String userLoginName;

    public UserIdentity() {}

    public UserIdentity(String authenticationSystemIdentifier, 
                       String authorizationSystemIdentifier, 
                       String userLoginName) {
        this.authenticationSystemIdentifier = authenticationSystemIdentifier;
        this.authorizationSystemIdentifier = authorizationSystemIdentifier;
        this.userLoginName = userLoginName;
    }

    public String getAuthenticationSystemIdentifier() {
        return authenticationSystemIdentifier;
    }

    public void setAuthenticationSystemIdentifier(String authenticationSystemIdentifier) {
        this.authenticationSystemIdentifier = authenticationSystemIdentifier;
    }

    public String getAuthorizationSystemIdentifier() {
        return authorizationSystemIdentifier;
    }

    public void setAuthorizationSystemIdentifier(String authorizationSystemIdentifier) {
        this.authorizationSystemIdentifier = authorizationSystemIdentifier;
    }

    public String getUserLoginName() {
        return userLoginName;
    }

    public void setUserLoginName(String userLoginName) {
        this.userLoginName = userLoginName;
    }

    @Override
    public String toString() {
        return "UserIdentity{" +
                "authenticationSystemIdentifier='" + authenticationSystemIdentifier + '\'' +
                ", authorizationSystemIdentifier='" + authorizationSystemIdentifier + '\'' +
                ", userLoginName='" + userLoginName + '\'' +
                '}';
    }
}