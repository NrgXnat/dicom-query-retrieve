package org.nrg.xnatx.dqr.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.nrg.framework.exceptions.NrgServiceRuntimeException;
import org.nrg.xdat.XDAT;
import org.nrg.xnatx.dqr.exceptions.PacsException;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class RetryablePacsOperation<T> implements Callable<T> {

    private static final DqrPreferences dqrPreferences
            = XDAT.getContextService().getBeanSafely(DqrPreferences.class);

    private final int maxRetries;
    private final long secondsBeforeRetry;

    public RetryablePacsOperation() {
        maxRetries = Integer.parseInt(dqrPreferences.getDqrMaxPacsRequestAttempts());
        secondsBeforeRetry = Long.parseLong(dqrPreferences.getDqrWaitToRetryRequestInSeconds());
    }

    @Override
    public T call() throws PacsException {

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                return doOperationWithRetry();
            } catch (PacsException e) {
                if (!e.isRetryable()) {
                    // The PACS rejected the request rather than failing to carry it out, so the
                    // remaining attempts would fail identically. Fail now with the real reason.
                    log.debug("PACS operation failed permanently on attempt {}/{}; not retrying", attempt + 1, maxRetries, e);
                    throw e;
                }
                if (attempt + 1 < maxRetries) {
                    try {
                        TimeUnit.SECONDS.sleep(secondsBeforeRetry);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new NrgServiceRuntimeException("Thread interrupted while performing pacs operation.", ex);
                    }

                    log.debug("PACS operation attempt: {}/{}", attempt + 1, maxRetries, e);
                    continue;
                }
                throw e;
            }
        }
        return null;
    }


    public abstract T doOperationWithRetry() throws PacsException;
}
