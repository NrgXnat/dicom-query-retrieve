/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.events.PacsThreads
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.events;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PacsThreads {
    public void increment(final long pacsId) {
        final int updated = _get(pacsId).incrementAndGet();
        log.debug("PACS {} Updated thread count to {}", pacsId, updated);
    }

    public void decrement(final long pacsId) {
        // Only decrement if greater than 0
        final int updated = _get(pacsId).updateAndGet(i -> i > 0 ? i - 1 : i); ;
        log.debug("PACS {} Updated thread count to {}", pacsId, updated);
    }

    private AtomicInteger _get(final long pacsId) {
        return _threadsPerPacs.computeIfAbsent(pacsId, key -> new AtomicInteger(0));
    }

    public int get(final long pacsId) {
        return _get(pacsId).get();
    }

    public int numAvailable(final long pacsId, final int maxAvailable) {
        final int current = get(pacsId);
        log.debug("PACS {} has {}/{} threads running", pacsId, current, maxAvailable);
        return maxAvailable - current;
    }

    public boolean isOversubscribed(final long pacsId, final int maxAvailable) {
        return numAvailable(pacsId, maxAvailable) < 0;
    }

    public boolean hasAvailable(final long pacsId, final int maxAvailable) {
        return numAvailable(pacsId, maxAvailable) > 0;
    }

    private final Map<Long, AtomicInteger> _threadsPerPacs = new ConcurrentHashMap<>();
}
