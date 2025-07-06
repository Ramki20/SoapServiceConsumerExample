package com.example.service;

import java.util.List;

/**
 * User Roles Response DTO to avoid conflicts with generated classes
 */
public class UserRolesResponseDto {
    private List<String> userRoles;
    private UserIdentityDto userIdentity;

    // Constructors
    public UserRolesResponseDto() {}

    public UserRolesResponseDto(List<String> userRoles, UserIdentityDto userIdentity) {
        this.userRoles = userRoles;
        this.userIdentity = userIdentity;
    }

    // Getters and Setters
    public List<String> getUserRoles() {
        return userRoles;
    }

    public void setUserRoles(List<String> userRoles) {
        this.userRoles = userRoles;
    }

    public UserIdentityDto getUserIdentity() {
        return userIdentity;
    }

    public void setUserIdentity(UserIdentityDto userIdentity) {
        this.userIdentity = userIdentity;
    }

    @Override
    public String toString() {
        return "UserRolesResponseDto{" +
                "userRoles=" + userRoles +
                ", userIdentity=" + userIdentity +
                '}';
    }
}