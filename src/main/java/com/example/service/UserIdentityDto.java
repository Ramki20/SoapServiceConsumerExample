package com.example.service;

/**
 * User Identity DTO to avoid conflicts with generated classes
 */
public class UserIdentityDto {
    private String authenticationSystemIdentifier;
    private String authorizationSystemIdentifier;
    private String userLoginName;

    // Constructors
    public UserIdentityDto() {}

    public UserIdentityDto(String authenticationSystemIdentifier, 
                          String authorizationSystemIdentifier, 
                          String userLoginName) {
        this.authenticationSystemIdentifier = authenticationSystemIdentifier;
        this.authorizationSystemIdentifier = authorizationSystemIdentifier;
        this.userLoginName = userLoginName;
    }

    // Getters and Setters
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
        return "UserIdentityDto{" +
                "authenticationSystemIdentifier='" + authenticationSystemIdentifier + '\'' +
                ", authorizationSystemIdentifier='" + authorizationSystemIdentifier + '\'' +
                ", userLoginName='" + userLoginName + '\'' +
                '}';
    }
}