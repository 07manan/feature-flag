package com.github._manan.featureflags.dto;

public record OAuthUserInfo(
    String email,
    String firstName,
    String lastName,
    String providerId
) {
    public static OAuthUserInfo fromFullName(String email, String fullName, String providerId) {
        String firstName = fullName;
        String lastName = "";
        
        if (fullName != null && fullName.contains(" ")) {
            int lastSpace = fullName.lastIndexOf(' ');
            firstName = fullName.substring(0, lastSpace).trim();
            lastName = fullName.substring(lastSpace + 1).trim();
        }
        
        return new OAuthUserInfo(email, firstName, lastName, providerId);
    }
}
