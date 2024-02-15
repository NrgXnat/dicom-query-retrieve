package org.nrg.xnatx.dqr.utils;

import org.nrg.xnatx.dqr.exceptions.PacsConnectionException;
import lombok.extern.slf4j.Slf4j;
import org.nrg.framework.exceptions.NrgServiceRuntimeException;
import org.nrg.xdat.XDAT;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class RetryablePacsOperation<T> implements Callable<T> {

    private static final DqrPreferences dqrPreferences
            = XDAT.getContextService().getBeanSafely(DqrPreferences.class);

    private final String pacsName;
    private final int maxRetries;
    private final long secondsBeforeRetry;

    public RetryablePacsOperation(final Pacs pacs) {
        this(pacs.getAeTitle());
    }

    public RetryablePacsOperation(final String pacsName) {
        this.pacsName = pacsName;

        maxRetries         = Integer.parseInt(dqrPreferences.getDqrMaxPacsRequestAttempts());
        secondsBeforeRetry = Long.parseLong(dqrPreferences.getDqrWaitToRetryRequestInSeconds());
    }

    @Override
    public T call() throws PacsConnectionException {

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                return doOperationWithRetry();
            } catch (PacsConnectionException e) {
                if (attempt + 1 < maxRetries) {
                    try {
                        TimeUnit.SECONDS.sleep(secondsBeforeRetry);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new NrgServiceRuntimeException("Thread interrupted while performing pacs operation.", ex);
                    }
                    log.debug("Retrying Pacs: {}.  Retry attempt: {}/{}", pacsName, attempt + 1, maxRetries);
                    continue;
                }
                throw new PacsConnectionException("Unable to make a connection to pacs: " + pacsName
                        + ". Exceeded the maximum number of retry attempts (" + maxRetries + " attempts).", e);
            }
        }
        return null;
    }


    public abstract T doOperationWithRetry() throws PacsConnectionException;

    public String getPacsName() {
        return pacsName;
    }
}
