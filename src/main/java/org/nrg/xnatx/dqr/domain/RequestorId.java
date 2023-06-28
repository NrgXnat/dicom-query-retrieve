/**
 * Copyright (c) 2023 Flywheel
 */
package org.nrg.xnatx.dqr.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.nrg.xft.security.UserI;

/**
 * Identifiers for the originator of a request.
 */
@AllArgsConstructor
@Getter
public class RequestorId {
    private final UserI user;
    private final String userDefinedId;
}
