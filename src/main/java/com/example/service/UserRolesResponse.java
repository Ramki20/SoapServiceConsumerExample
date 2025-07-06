package com.example.service;

import java.util.List;

/**
 * User Roles Response model
 */
public class UserRolesResponse {
    private List<String> userRoles;
    private UserIdentity userIdentity;

    public UserRolesResponse() {}

    public UserRolesResponse(List<String> userRoles, UserIdentity userIdentity) {
        this.userRoles = userRoles;
        this.userIdentity = userIdentity;
    }

    public List<String> getUserRoles() {
        return userRoles;
    }

    public void setUserRoles(List<String> userRoles) {
        this.userRoles = userRoles;
    }

    public UserIdentity getUserIdentity() {
        return userIdentity;
    }

    public void setUserIdentity(UserIdentity userIdentity) {
        this.userIdentity = userIdentity;
    }

    @Override
    public String toString() {
        return "UserRolesResponse{" +
                "userRoles=" + userRoles +
                ", userIdentity=" + userIdentity +
                '}';
    }
}