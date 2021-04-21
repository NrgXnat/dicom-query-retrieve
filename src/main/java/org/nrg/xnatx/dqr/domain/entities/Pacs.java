/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.entities.Pacs
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain.entities;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"host", "aeTitle", "queryRetrievePort"}))
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
@PortNotNullIfDefaultPacs
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class Pacs extends AbstractHibernateEntity implements Serializable {
    private static final long serialVersionUID = 3741269782521664702L;

    @Override
    public String toString() {
        return "{ aeTitle: \"" + aeTitle + "\", "
               + "host: \"" + host + "\", "
               + "label: \"" + label + "\", "
               + "queryable: " + queryable + ", "
               + "queryRetrievePort: " + queryRetrievePort + ", "
               + "isDefaultQueryRetrievePacs: " + defaultQueryRetrievePacs + ", "
               + "storable: " + storable + ", "
               + "isDefaultStoragePacs: " + defaultStoragePacs + ", "
               + "supportsExtendedNegotiations: " + supportsExtendedNegotiations + " }";
    }

    @NotBlank
    @Size(max = 100)
    private String aeTitle;

    @NotBlank
    @Size(max = 100)
    private String host;

    @Size(max = 100)
    private String label;

    @NotNull
    @Column(columnDefinition = "boolean default true")
    private boolean queryable = true;

    private Integer queryRetrievePort;

    @NotNull
    @Column(columnDefinition = "boolean default false")
    private boolean defaultStoragePacs;

    @NotNull
    @Column(columnDefinition = "boolean default false")
    private boolean storable;

    @NotNull
    @Column(columnDefinition = "boolean default false")
    private boolean defaultQueryRetrievePacs;

    @NotBlank
    @Size(max = 100)
    private String ormStrategySpringBeanId;

    @NotNull
    @Column(columnDefinition = "boolean default false")
    private boolean supportsExtendedNegotiations;
}
