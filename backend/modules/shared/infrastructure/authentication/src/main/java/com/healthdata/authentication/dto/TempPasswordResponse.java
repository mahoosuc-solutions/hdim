package com.healthdata.authentication.dto;

import lombok.Getter;

@Getter
public class TempPasswordResponse {
    private final String temporaryPassword;
    private final String message;

    public TempPasswordResponse(String temporaryPassword) {
        this.temporaryPassword = temporaryPassword;
        this.message = "Temporary password set. User must change password on next login.";
    }
}
