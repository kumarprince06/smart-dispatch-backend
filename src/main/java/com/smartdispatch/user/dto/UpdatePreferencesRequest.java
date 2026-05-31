package com.smartdispatch.user.dto;

import lombok.Data;

@Data
public class UpdatePreferencesRequest {
    private Boolean notificationsEnabled;
    private Boolean smsEnabled;
    private String preferredLanguage;
    private Boolean darkMode; // Note: You can add darkMode to User if you want, or just manage it locally
}
