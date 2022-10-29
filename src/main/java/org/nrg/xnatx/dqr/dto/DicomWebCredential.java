package org.nrg.xnatx.dqr.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DicomWebCredential {
    @JsonProperty("aeTitle")
    private String aeTitle;

    @JsonProperty("username")
    private String username;

    @JsonProperty("password")
    private String password;

    public String getAeTitle() {
        return aeTitle;
    }

    public void setAeTitle(String aeTitle) {
        this.aeTitle = aeTitle;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public DicomWebCredential() {

    }

    public DicomWebCredential(String aeTitle, String username, String password) {
        this.aeTitle = aeTitle;
        this.username = username;
        this.password = password;
    }
}
