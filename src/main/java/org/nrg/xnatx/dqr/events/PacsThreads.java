/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.events.PacsThreads
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.events;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PacsThreads {
    public void add(final long pacsId) {
        final int current;
        final int updated;
        synchronized (THREAD_COUNT_LOCK) {
            current = get(pacsId);
            updated = current + 1;
            _threadsPerPacs.put(pacsId, updated);
        }
        log.debug("Updated thread count for PACS {} from {} to {}", pacsId, current, updated);
    }

    public void remove(final long pacsId) {
        if (!_threadsPerPacs.containsKey(pacsId)) {
            log.warn("Tried to decrement thread count for PACS {} but that's not in the thread table", pacsId);
            return;
        }
        final int current;
        final int updated;
        synchronized (THREAD_COUNT_LOCK) {
            current = get(pacsId);
            if (current == 0) {
                log.warn("Tried to decrement thread count for PACS {} but that's already set to 0", pacsId);
                return;
            }
            updated = current - 1;
            _threadsPerPacs.put(pacsId, updated);
        }
        log.debug("Updated thread count for PACS {} from {} to {}", pacsId, current, updated);
    }

    public int get(final long pacsId) {
        return _threadsPerPacs.getOrDefault(pacsId, 0);
    }

    public int numAvailable(final long pacsId, final int maxAvailable) {
        final int current = get(pacsId);
        log.debug("PACS {} currently has {} threads running with {} available", pacsId, current, maxAvailable);
        return maxAvailable - current;
    }

    public boolean isOversubscribed(final long pacsId, final int maxAvailable) {
        return numAvailable(pacsId, maxAvailable) < 0;
    }

    public boolean hasAvailable(final long pacsId, final int maxAvailable) {
        return numAvailable(pacsId, maxAvailable) > 0;
    }

    private final static Object THREAD_COUNT_LOCK = new Object();

    private final Map<Long, Integer> _threadsPerPacs = new HashMap<>();
}
