package org.nrg.xnatx.dqr.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class DicomWebCredential {
    String aeTitle;
    String username;
    String password;
    boolean preemptiveAuth;

    public DicomWebCredential(@JsonProperty("aeTitle") final String aeTitle,
                              @JsonProperty("username") final String username,
                              @JsonProperty("password") final String password,
                              @JsonProperty("preemptiveAuth") final Boolean preemptiveAuth) {
        this.aeTitle = aeTitle;
        this.username = username;
        this.password = password;
        this.preemptiveAuth = preemptiveAuth != null && preemptiveAuth;
    }
}
