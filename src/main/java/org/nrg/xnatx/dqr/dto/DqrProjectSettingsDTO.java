package org.nrg.xnatx.dqr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DqrProjectSettingsDTO implements Serializable {
    private static final long serialVersionUID = -2976543935143705719L;

    private String  projectId;
    private Boolean enabled;
}
