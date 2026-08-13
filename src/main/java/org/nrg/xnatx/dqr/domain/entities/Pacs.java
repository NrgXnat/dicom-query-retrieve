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
import org.apache.commons.lang3.StringUtils;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;
import org.nrg.xnatx.dqr.dto.PacsSettings;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.validator.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.beans.FeatureDescriptor;
import java.io.Serializable;
import java.util.stream.Stream;

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = "label"), @UniqueConstraint(columnNames = {"host", "queryRetrievePort", "aeTitle"})})
@Cacheable
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
    private boolean _dicomWebEnabled;
    private String _dicomWebRootUrl;
    private String _dicomObjectIdentifier;
    @Builder.Default
    private boolean _anonymizationEnabled = true;

    public Pacs(final PacsSettings settings) {
        copySettings(settings);
    }

    public void copySettings(final PacsSettings settings) {
        final BeanWrapper wrappedPacs = PropertyAccessorFactory.forBeanPropertyAccess(settings);
        BeanUtils.copyProperties(settings, this, Stream.of(wrappedPacs.getPropertyDescriptors()).map(FeatureDescriptor::getName).filter(name -> wrappedPacs.getPropertyValue(name) == null).toArray(String[]::new));

        if (_dicomWebEnabled && StringUtils.isBlank(_aeTitle)) {
            _aeTitle = _label;
        }
    }

    @NotBlank
    @Size(max = 100)
    public String getAeTitle() {
        return _aeTitle;
    }

    public void setAeTitle(final String aeTitle) {
        _aeTitle = aeTitle;
    }

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
    @NotNull
    @Column(columnDefinition = "boolean default false")
    public boolean isDicomWebEnabled() {
        return _dicomWebEnabled;
    }

    public void setDicomWebEnabled(boolean dicomWebEnabled) {
        this._dicomWebEnabled = dicomWebEnabled;
    }

    public String getDicomWebRootUrl() {
        return _dicomWebRootUrl;
    }

    public void setDicomWebRootUrl(String dicomWebRootUrl) {
        this._dicomWebRootUrl = dicomWebRootUrl;
    }


    public String getDicomObjectIdentifier() {
        return _dicomObjectIdentifier;
    }

    public void setDicomObjectIdentifier(String _dicomObjectIdentifier) {
        this._dicomObjectIdentifier = _dicomObjectIdentifier;
    }

    @Column(columnDefinition = "boolean default true")
    public boolean isAnonymizationEnabled() {
        return _anonymizationEnabled;
    }

    public void setAnonymizationEnabled(boolean _anonymization) {
        this._anonymizationEnabled = _anonymization;
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
                + "dicomObjectIdentifier: " + _dicomObjectIdentifier + ", "
                + "isDefaultStoragePacs: " + _defaultStoragePacs + ", "
                + "supportsExtendedNegotiations: " + _supportsExtendedNegotiations + ", "
                + "Dicom-web Enabled: " + _dicomWebEnabled + ", "
                + "Dicom-Web Root Url: " + _dicomWebRootUrl + " }";
    }
}
