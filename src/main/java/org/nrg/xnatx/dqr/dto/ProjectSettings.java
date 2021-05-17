package org.nrg.xnatx.dqr.dto;

import lombok.Data;
import org.nrg.xnatx.dqr.domain.entities.DqrProjectSettings;

@Data
public class ProjectSettings {
    public ProjectSettings(final DqrProjectSettings settings) {
        projectId = settings.getProjectId();
        dqrEnabled = settings.isDqrEnabled();
    }

    private String  projectId;
    private Boolean dqrEnabled;
}
