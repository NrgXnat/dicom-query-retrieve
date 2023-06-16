/**
 * Copyright (c) 2023 Flywheel
 */
package org.nrg.xnatx.dqr.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

public class OptionalString {
    /**
     * Make an Optional from a CharSequence.
     * @param cs character sequence
     * @return Optional.of(cs) if cs is non-blank, Optional::empty otherwise.
     */
    public static <T extends CharSequence> Optional<T> of(final T cs) {
        return StringUtils.isBlank(cs) ? Optional.empty() : Optional.of(cs);
    }
}
