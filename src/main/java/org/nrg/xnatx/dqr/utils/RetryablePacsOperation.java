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

    private final Pacs pacs;
    private final int maxRetries;
    private final long secondsBeforeRetry;

    public RetryablePacsOperation(final Pacs pacs) {
        this.pacs = pacs;

        maxRetries         = Integer.parseInt(dqrPreferences.getDqrMaxPacsCMOVEAttempts());
        secondsBeforeRetry = Long.parseLong(dqrPreferences.getDqrWaitToRetryCMOVETimeInSeconds());
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
                    log.debug("Retrying Pacs: {}@{}.  Retry attempt: {}/{}", pacs.getAeTitle(), pacs.getHost(), attempt + 1, maxRetries);
                    continue;
                }
                throw new PacsConnectionException("Unable to make a connection to pacs: " + pacs.getAeTitle() + "@" + pacs.getHost()
                        + ". Exceeded the maximum number of retry attempts (" + maxRetries + " attempts).", e);
            }
        }
        return null;
    }


    public abstract T doOperationWithRetry() throws PacsConnectionException;

    public Pacs getPacs() {
        return pacs;
    }
}
