/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.entities.Pacs
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
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
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = "label"), @UniqueConstraint(columnNames = {"host", "queryRetrievePort", "aeTitle"})})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
@PortNotNullIfDefaultPacs
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Accessors(prefix = "_")
public class Pacs extends AbstractHibernateEntity implements Serializable {
    private static final long serialVersionUID = 3741269782521664702L;

    private String  _aeTitle;
    private String  _host;
    private String  _label;
    @Builder.Default
    private boolean _queryable = true;
    private Integer _queryRetrievePort;
    private boolean _defaultStoragePacs;
    private boolean _storable;
    private boolean _defaultQueryRetrievePacs;
    private String  _ormStrategySpringBeanId;
    private boolean _supportsExtendedNegotiations;

    @NotBlank
    @Size(max = 100)
    public String getAeTitle() {
        return _aeTitle;
    }

    public void setAeTitle(final String aeTitle) {
        _aeTitle = aeTitle;
    }

    @NotBlank
    @Size(max = 100)
    public String getHost() {
        return _host;
    }

    public void setHost(final String host) {
        _host = host;
    }

    @Size(max = 100)
    public String getLabel() {
        return _label;
    }

    public void setLabel(final String label) {
        _label = label;
    }

    @NotNull
    @Column(columnDefinition = "boolean default true")
    public boolean isQueryable() {
        return _queryable;
    }

    public void setQueryable(final boolean queryable) {
        _queryable = queryable;
    }

    public Integer getQueryRetrievePort() {
        return _queryRetrievePort;
    }

    public void setQueryRetrievePort(final Integer queryRetrievePort) {
        _queryRetrievePort = queryRetrievePort;
    }

    @NotNull
    @Column(columnDefinition = "boolean default false")
    public boolean isDefaultStoragePacs() {
        return _defaultStoragePacs;
    }

    public void setDefaultStoragePacs(final boolean defaultStoragePacs) {
        if (defaultStoragePacs) {
            setStorable(true);
        }
        _defaultStoragePacs = defaultStoragePacs;
    }

    @NotNull
    @Column(columnDefinition = "boolean default false")
    public boolean isStorable() {
        return _storable;
    }

    public void setStorable(final boolean storable) {
        _storable = storable;
    }

    @NotNull
    @Column(columnDefinition = "boolean default false")
    public boolean isDefaultQueryRetrievePacs() {
        return _defaultQueryRetrievePacs;
    }

    public void setDefaultQueryRetrievePacs(final boolean defaultQueryRetrievePacs) {
        if (defaultQueryRetrievePacs) {
            setQueryable(true);
        }
        _defaultQueryRetrievePacs = defaultQueryRetrievePacs;
    }

    @NotBlank
    @Size(max = 100)
    public String getOrmStrategySpringBeanId() {
        return _ormStrategySpringBeanId;
    }

    public void setOrmStrategySpringBeanId(final String ormStrategySpringBeanId) {
        _ormStrategySpringBeanId = ormStrategySpringBeanId;
    }

    @NotNull
    @Column(columnDefinition = "boolean default false")
    public boolean isSupportsExtendedNegotiations() {
        return _supportsExtendedNegotiations;
    }

    public void setSupportsExtendedNegotiations(final boolean supportsExtendedNegotiations) {
        _supportsExtendedNegotiations = supportsExtendedNegotiations;
    }

    @Override
    public String toString() {
        return "{ aeTitle: \"" + _aeTitle + "\", "
               + "host: \"" + _host + "\", "
               + "label: \"" + _label + "\", "
               + "queryable: " + _queryable + ", "
               + "queryRetrievePort: " + _queryRetrievePort + ", "
               + "isDefaultQueryRetrievePacs: " + _defaultQueryRetrievePacs + ", "
               + "storable: " + _storable + ", "
               + "isDefaultStoragePacs: " + _defaultStoragePacs + ", "
               + "supportsExtendedNegotiations: " + _supportsExtendedNegotiations + " }";
    }
}
