package org.nrg.xnatx.dqr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PacsSettings implements Serializable {
    private static final long serialVersionUID = 2489150736680736066L;

    private String  aeTitle;
    private String  host;
    private String  label;
    private Boolean queryable;
    private Integer queryRetrievePort;
    private Boolean defaultStoragePacs;
    private Boolean storable;
    private Boolean defaultQueryRetrievePacs;
    private String  ormStrategySpringBeanId;
    private Boolean supportsExtendedNegotiations;
    private Boolean dicomWebEnabled;
    private String dicomWebRootUrl;
    private String dicomObjectIdentifier;
}
