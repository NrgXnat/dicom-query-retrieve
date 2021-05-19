package org.nrg.xnatx.dqr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nrg.xnatx.dqr.domain.entities.DqrProjectSettings;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectSettings implements Serializable {
    private static final long serialVersionUID = -2976543935143705719L;

    public ProjectSettings(final DqrProjectSettings settings) {
        projectId = settings.getProjectId();
        dqrEnabled = settings.isDqrEnabled();
    }

    private String  projectId;
    private Boolean dqrEnabled;
}
