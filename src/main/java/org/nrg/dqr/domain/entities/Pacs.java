/*
 * Pacs
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.dqr.domain.entities;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"aeTitle"}))
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
@PortNotNullIfDefaultPacs
public class Pacs extends AbstractHibernateEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private String _aeTitle;
    private String _host;
    private String _label;

    private Boolean _queryable;
    private Boolean _defaultStoragePacs;

    private Boolean _storable;
    private Integer _queryRetrievePort;
    private Boolean _defaultQueryRetrievePacs;
    private String _ormStrategySpringBeanId;
    private boolean _supportsExtendedNegotiations;

    private Integer _defaultSessionsPerDequeue;
    private Integer _defaultDequeuesPerHour;

    // for Hibernate
    public Pacs() {
    }

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
    public boolean isStorable() {
        return _storable;
    }

    public void setStorable(final boolean storable) {
        _storable = storable;
    }
    @NotNull
    public Boolean isDefaultStoragePacs() {
        return _defaultStoragePacs;
    }

    public void setDefaultStoragePacs(final Boolean defaultStoragePacs) {
        _defaultStoragePacs = defaultStoragePacs;
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
    public Boolean isDefaultQueryRetrievePacs() {
        return _defaultQueryRetrievePacs;
    }

    public void setDefaultQueryRetrievePacs(final Boolean defaultQueryRetrievePacs) {
        _defaultQueryRetrievePacs = defaultQueryRetrievePacs;
    }

    @NotNull
    @Column(columnDefinition = "boolean default false")
    public boolean isSupportsExtendedNegotiations() {
        return _supportsExtendedNegotiations;
    }

    public void setSupportsExtendedNegotiations(final boolean supportsExtendedNegotiations) {
        _supportsExtendedNegotiations = supportsExtendedNegotiations;
    }

    /**
     * Ugly, this doesn't really belong on a domain object, not sure where to put it though...
     */
    @NotBlank
    @Size(max = 100)
    public String getOrmStrategySpringBeanId() {
        return _ormStrategySpringBeanId;
    }

    public void setOrmStrategySpringBeanId(final String ormStrategySpringBeanId) {
        _ormStrategySpringBeanId = ormStrategySpringBeanId;
    }

    public Integer getDefaultSessionsPerDequeue() {
        return _defaultSessionsPerDequeue;
    }

    public void setDefaultSessionsPerDequeue(final Integer defaultSessionsPerDequeue) {
        _defaultSessionsPerDequeue = defaultSessionsPerDequeue;
    }

    public Integer getDefaultDequeuesPerHour() {
        return _defaultDequeuesPerHour;
    }

    public void setDefaultDequeuesPerHour(final Integer defaultDequeuesPerHour) {
        _defaultDequeuesPerHour = defaultDequeuesPerHour;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(137, 479).append(_aeTitle).append(_host)
                .append(_label).append(_storable).append(_defaultStoragePacs).append(_queryable)
                .append(_queryRetrievePort).append(_defaultQueryRetrievePacs)
                .append(_defaultSessionsPerDequeue).append(_defaultDequeuesPerHour).toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final Pacs other = (Pacs) obj;
        return new EqualsBuilder().append(_aeTitle, other._aeTitle).append(_host, other._host)
                .append(_label, other._label).append(_storable, other._storable)
                .append(_queryable, other._queryable).append(_queryRetrievePort, other._queryRetrievePort)
                .append(_defaultQueryRetrievePacs, other._defaultQueryRetrievePacs)
                .append(_supportsExtendedNegotiations, other._supportsExtendedNegotiations)
                .append(_ormStrategySpringBeanId, other._ormStrategySpringBeanId)
                .append(_defaultSessionsPerDequeue, other._defaultSessionsPerDequeue)
                .append(_defaultDequeuesPerHour, other._defaultDequeuesPerHour).isEquals();
    }

    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder("{ ");
        buffer.append("aeTitle: ").append(_aeTitle).append(", ");
        buffer.append("host: ").append(_host).append(", ");
        buffer.append("label: ").append(_label).append(", ");
        buffer.append("queryable: ").append(_queryable).append(", ");
        buffer.append("queryRetrievePort: ").append(_queryRetrievePort).append(", ");
        buffer.append("isDefaultQueryRetrievePacs: ").append(_defaultQueryRetrievePacs).append(", ");
        buffer.append("storable: ").append(_storable).append(", ");
        buffer.append("isDefaultStoragePacs: ").append(_defaultStoragePacs).append(", ");
        buffer.append("supportsExtendedNegotiations: ").append(_supportsExtendedNegotiations).append(", ");
        buffer.append("defaultSessionsPerDequeue: ").append(_defaultSessionsPerDequeue).append(", ");
        buffer.append("defaultDequeuesPerHour: ").append(_defaultDequeuesPerHour).append(" }");
        return buffer.toString();
    }
}
