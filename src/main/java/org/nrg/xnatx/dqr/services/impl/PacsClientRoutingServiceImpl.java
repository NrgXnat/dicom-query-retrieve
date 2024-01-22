package org.nrg.xnatx.dqr.services.impl;

import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.services.PacsClientRoutingService;
import org.nrg.xnatx.dqr.services.PacsClientService;
import org.springframework.stereotype.Service;

@Service
public class PacsClientRoutingServiceImpl implements PacsClientRoutingService {
    private final DicomWebPacsClientService dicomWebPacsClientService;
    private final DimsePacsClientService dimsePacsClientService;

    public PacsClientRoutingServiceImpl(final DicomWebPacsClientService dicomWebPacsClientService, final DimsePacsClientService dimsePacsClientService) {
        this.dicomWebPacsClientService = dicomWebPacsClientService;
        this.dimsePacsClientService = dimsePacsClientService;
    }

    @Override
    public PacsClientService getPacsClientService(Pacs pacs) {
        return pacs.isDicomWebEnabled() ? dicomWebPacsClientService : dimsePacsClientService;
    }
}
