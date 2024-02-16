package org.nrg.xnatx.dqr.services;

import org.nrg.xnatx.dqr.domain.entities.Pacs;

public interface PacsClientRoutingService {
    PacsClientService getPacsClientService(Pacs pacs);
}
