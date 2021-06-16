package org.nrg.xnatx.dqr.exceptions;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.Accessors;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Value
@Accessors(prefix = "_")
@EqualsAndHashCode(callSuper = true)
@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "The specified project is not currently DQR enabled")
public class ProjectNotDqrEnabledException extends DqrException {
    private static final long serialVersionUID = -883566518505813856L;

    String _projectId;

    public ProjectNotDqrEnabledException(final String projectId) {
        super("Project " + projectId + " is not DQR enabled");
        _projectId = projectId;
    }
}
