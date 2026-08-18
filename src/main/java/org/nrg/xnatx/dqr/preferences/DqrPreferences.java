/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.preferences.DqrPreferences
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.preferences;

import static org.apache.fop.configuration.Configuration.getStringValue;

import lombok.extern.slf4j.Slf4j;
import org.nrg.framework.configuration.ConfigPaths;
import org.nrg.framework.services.NrgEventServiceI;
import org.nrg.framework.utilities.OrderedProperties;
import org.nrg.prefs.annotations.NrgPreference;
import org.nrg.prefs.annotations.NrgPreferenceBean;
import org.nrg.prefs.exceptions.InvalidPreferenceName;
import org.nrg.prefs.services.NrgPreferenceService;
import org.nrg.xdat.preferences.EventTriggeringAbstractPreferenceBean;
import org.springframework.beans.factory.annotation.Autowired;

@NrgPreferenceBean(toolId = DqrPreferences.DQR_TOOL_ID,
                   toolName = "XNAT DQR Preferences",
                   description = "Manages preferences and settings for the dicom query retrieve plugin.")
@Slf4j
@SuppressWarnings("unused")
public class DqrPreferences extends EventTriggeringAbstractPreferenceBean {
    public static final String INVALID_PREFERENCE_NAME_TEMPLATE = "Invalid preference name {}: something is very wrong here.";
    public static final String DQR_TOOL_ID = "dqr";

    public static final String DQR_CALLING_AE = "dqrCallingAe";
    public static final String DQR_CALLING_AE_DEFAULT_VALUE = "XNAT";
    public static final String PACS_AVAILABILITY_CHECK_FREQUENCY = "pacsAvailabilityCheckFrequency";
    public static final String PACS_AVAILABILITY_CHECK_FREQUENCY_DEFAULT_VALUE = "1 minute";
    public static final String DQR_MAX_PACS_REQUEST_ATTEMPTS = "dqrMaxPacsRequestAttempts";
    public static final String DQR_MAX_PACS_REQUEST_ATTEMPTS_OLD_NAME = "dqrMaxPacsCMOVEAttempts";
    public static final String DQR_MAX_PACS_REQUEST_ATTEMPTS_DEFAULT_VALUE = "5";
    public static final String DQR_WAIT_TO_RETRY_REQUEST_IN_SECONDS = "dqrWaitToRetryRequestInSeconds";
    public static final String DQR_WAIT_TO_RETRY_REQUEST_IN_SECONDS_OLD_NAME = "dqrWaitToRetryCMOVETimeInSeconds";
    public static final String DQR_WAIT_TO_RETRY_REQUEST_IN_SECONDS_DEFAULT_VALUE = "300";
    public static final String NOTIFY_ADMIN_ON_IMPORT = "notifyAdminOnImport";
    public static final String NOTIFY_ADMIN_ON_IMPORT_DEFAULT_VALUE = "false";
    public static final String ASSUME_SAME_SESSION_IF_ARRIVED_WITHIN = "assumeSameSessionIfArrivedWithin";
    public static final String ASSUME_SAME_SESSION_IF_ARRIVED_WITHIN_DEFAULT_VALUE = "30 minutes";
    public static final String ALLOW_ALL_USERS_TO_USE_DQR = "allowAllUsersToUseDqr";
    public static final String ALLOW_ALL_USERS_TO_USE_DQR_DEFAULT_VALUE = "false";
    public static final String ALLOW_ALL_PROJECTS_TO_USE_DQR = "allowAllProjectsToUseDqr";
    public static final String ALLOW_ALL_PROJECTS_TO_USE_DQR_DEFAULT_VALUE = "false";
    public static final String ORM_STRATEGY = "ormStrategy";
    public static final String ORM_STRATEGY_DEFAULT_VALUE = "org.nrg.xnatx.dqr.dicom.strategy.orm.BasicOrmStrategy";
    public static final String PATIENT_NAME_STRATEGY = "patientNameStrategy";
    public static final String PATIENT_NAME_STRATEGY_DEFAULT_VALUE = "org.nrg.xnatx.dqr.dicom.strategy.orm.BasicPatientNameStrategy";
    public static final String RESULT_SET_LIMIT_STRATEGY = "resultSetLimitStrategy";
    public static final String RESULT_SET_LIMIT_STRATEGY_DEFAULT_VALUE = "org.nrg.xnatx.dqr.dicom.strategy.orm.BasicResultSetLimitStrategy";
    public static final String LEAVE_PACS_AUDIT_TRAIL = "leavePacsAuditTrail";
    public static final String LEAVE_PACS_AUDIT_TRAIL_DEFAULT_VALUE = "false";
    public static final String DICOM_WEB_METADATA_READ_TIMEOUT_SECONDS = "dicomWebMetadataReadTimeoutSeconds";
    public static final String DICOM_WEB_METADATA_READ_TIMEOUT_SECONDS_DEFAULT_VALUE = "20";
    public static final String DICOM_WEB_RETRIEVE_READ_TIMEOUT_SECONDS = "dicomWebRetrieveReadTimeoutSeconds";
    public static final String DICOM_WEB_RETRIEVE_READ_TIMEOUT_SECONDS_DEFAULT_VALUE = "20";
    public static final String DIMSE_RESPONSE_TIMEOUT_SECONDS = "dimseResponseTimeoutSeconds";
    public static final String DIMSE_RESPONSE_TIMEOUT_SECONDS_DEFAULT_VALUE = "60";
    public static final String DIMSE_RETRIEVE_TIMEOUT_SECONDS = "dimseRetrieveTimeoutSeconds";
    public static final String DIMSE_RETRIEVE_TIMEOUT_SECONDS_DEFAULT_VALUE = "60";
    public static final String DIMSE_ACCEPT_TIMEOUT_SECONDS = "dimseAcceptTimeoutSeconds";
    public static final String DIMSE_ACCEPT_TIMEOUT_SECONDS_DEFAULT_VALUE = "60";
    public static final String DIMSE_CONNECT_TIMEOUT_SECONDS = "dimseConnectTimeoutSeconds";
    public static final String DIMSE_CONNECT_TIMEOUT_SECONDS_DEFAULT_VALUE = "30";
    public static final String DQR_MAX_THROTTLE_SLEEP_SECONDS = "dqrMaxThrottleSleepSeconds";
    public static final String DQR_MAX_THROTTLE_SLEEP_SECONDS_DEFAULT_VALUE = "1800";

    @Autowired
    public DqrPreferences(final NrgPreferenceService preferenceService, final NrgEventServiceI eventService, final ConfigPaths configPaths, final OrderedProperties initPrefs) {
        super(preferenceService, eventService, configPaths, initPrefs);
    }

    private void safeSet(final String value, final String name) {
        try {
            set(value, name);
        } catch (InvalidPreferenceName e) {
            log.error(INVALID_PREFERENCE_NAME_TEMPLATE, name, e);
        }
    }

    private void safeSet(final Boolean value, final String name) {
        try {
            setBooleanValue(value, name);
        } catch (InvalidPreferenceName e) {
            log.error(INVALID_PREFERENCE_NAME_TEMPLATE, name, e);
        }
    }

    @NrgPreference(defaultValue = DQR_CALLING_AE_DEFAULT_VALUE)
    public String getDqrCallingAe() {
        return getValue(DQR_CALLING_AE);
    }

    public void setDqrCallingAe(final String dqrCallingAe) {
        safeSet(dqrCallingAe, DQR_CALLING_AE);
    }

    @NrgPreference(defaultValue = PACS_AVAILABILITY_CHECK_FREQUENCY_DEFAULT_VALUE)
    public String getPacsAvailabilityCheckFrequency() {
        return getValue(PACS_AVAILABILITY_CHECK_FREQUENCY);
    }

    public void setPacsAvailabilityCheckFrequency(final String pacsAvailabilityCheckFrequency) {
        safeSet(pacsAvailabilityCheckFrequency, PACS_AVAILABILITY_CHECK_FREQUENCY);
    }

    @NrgPreference(defaultValue = DQR_MAX_PACS_REQUEST_ATTEMPTS_DEFAULT_VALUE, aliases = DQR_MAX_PACS_REQUEST_ATTEMPTS_OLD_NAME)
    public String getDqrMaxPacsRequestAttempts() {
        return getValue(DQR_MAX_PACS_REQUEST_ATTEMPTS);
    }

    public void setDqrMaxPacsRequestAttempts(final String dqrMaxPacsRequestAttempts) {
        safeSet(dqrMaxPacsRequestAttempts, DQR_MAX_PACS_REQUEST_ATTEMPTS);

    }

    @NrgPreference(defaultValue = DQR_WAIT_TO_RETRY_REQUEST_IN_SECONDS_DEFAULT_VALUE, aliases = DQR_WAIT_TO_RETRY_REQUEST_IN_SECONDS_OLD_NAME)
    public String getDqrWaitToRetryRequestInSeconds() {
        return getValue(DQR_WAIT_TO_RETRY_REQUEST_IN_SECONDS);
    }

    public void setDqrWaitToRetryRequestInSeconds(final String dqrWaitToRetryRequestInSeconds) {
        safeSet(dqrWaitToRetryRequestInSeconds, DQR_WAIT_TO_RETRY_REQUEST_IN_SECONDS);
    }

    @NrgPreference(defaultValue = NOTIFY_ADMIN_ON_IMPORT_DEFAULT_VALUE)
    public boolean getNotifyAdminOnImport() {
        return getBooleanValue(NOTIFY_ADMIN_ON_IMPORT);
    }

    public void setNotifyAdminOnImport(final boolean notifyAdminOnImport) {
        safeSet(notifyAdminOnImport, NOTIFY_ADMIN_ON_IMPORT);
    }

    @NrgPreference(defaultValue = ASSUME_SAME_SESSION_IF_ARRIVED_WITHIN_DEFAULT_VALUE)
    public String getAssumeSameSessionIfArrivedWithin() {
        return getValue(ASSUME_SAME_SESSION_IF_ARRIVED_WITHIN);
    }

    public void setAssumeSameSessionIfArrivedWithin(final String assumeSameSessionIfArrivedWithin) {
        safeSet(assumeSameSessionIfArrivedWithin, ASSUME_SAME_SESSION_IF_ARRIVED_WITHIN);
    }

    @NrgPreference(defaultValue = ALLOW_ALL_USERS_TO_USE_DQR_DEFAULT_VALUE)
    public boolean getAllowAllUsersToUseDqr() {
        return getBooleanValue(ALLOW_ALL_USERS_TO_USE_DQR);
    }

    public void setAllowAllUsersToUseDqr(final boolean allowAllUsersToUseDqr) {
        safeSet(allowAllUsersToUseDqr, ALLOW_ALL_USERS_TO_USE_DQR);
    }

    @NrgPreference(defaultValue = ALLOW_ALL_PROJECTS_TO_USE_DQR_DEFAULT_VALUE)
    public boolean getAllowAllProjectsToUseDqr() {
        return getBooleanValue(ALLOW_ALL_PROJECTS_TO_USE_DQR);
    }

    public void setAllowAllProjectsToUseDqr(final boolean allowAllProjectsToUseDqr) {
        safeSet(allowAllProjectsToUseDqr, ALLOW_ALL_PROJECTS_TO_USE_DQR);
    }

    @NrgPreference(defaultValue = ORM_STRATEGY_DEFAULT_VALUE)
    public String getOrmStrategy() {
        return getStringValue(ORM_STRATEGY);
    }

    public void setOrmStrategy(final String ormStrategy) {
        safeSet(ormStrategy, ORM_STRATEGY);
    }

    @NrgPreference(defaultValue = PATIENT_NAME_STRATEGY_DEFAULT_VALUE)
    public String getPatientNameStrategy() {
        return getStringValue(PATIENT_NAME_STRATEGY);
    }

    public void setPatientNameStrategy(final String patientNameStrategy) {
        safeSet(patientNameStrategy, PATIENT_NAME_STRATEGY);
    }

    @NrgPreference(defaultValue = RESULT_SET_LIMIT_STRATEGY_DEFAULT_VALUE)
    public String getResultSetLimitStrategy() {
        return getStringValue(RESULT_SET_LIMIT_STRATEGY);
    }

    public void setResultSetLimitStrategy(final String resultSetLimitStrategy) {
        safeSet(resultSetLimitStrategy, RESULT_SET_LIMIT_STRATEGY);
    }

    @NrgPreference(defaultValue = LEAVE_PACS_AUDIT_TRAIL_DEFAULT_VALUE)
    public boolean getLeavePacsAuditTrail() {
        return getBooleanValue(LEAVE_PACS_AUDIT_TRAIL);
    }

    public void setLeavePacsAuditTrail(final boolean leavePacsAuditTrail) {
        safeSet(leavePacsAuditTrail, LEAVE_PACS_AUDIT_TRAIL);
    }

    @NrgPreference(defaultValue = DICOM_WEB_METADATA_READ_TIMEOUT_SECONDS_DEFAULT_VALUE)
    public String getDicomWebMetadataReadTimeoutSeconds() {
        return getValue(DICOM_WEB_METADATA_READ_TIMEOUT_SECONDS);
    }

    public void setDicomWebMetadataReadTimeoutSeconds(final String dicomWebMetadataReadTimeoutSeconds) {
        safeSet(dicomWebMetadataReadTimeoutSeconds, DICOM_WEB_METADATA_READ_TIMEOUT_SECONDS);
    }

    @NrgPreference(defaultValue = DICOM_WEB_RETRIEVE_READ_TIMEOUT_SECONDS_DEFAULT_VALUE)
    public String getDicomWebRetrieveReadTimeoutSeconds() {
        return getValue(DICOM_WEB_RETRIEVE_READ_TIMEOUT_SECONDS);
    }

    public void setDicomWebRetrieveReadTimeoutSeconds(final String dicomWebRetrieveReadTimeoutSeconds) {
        safeSet(dicomWebRetrieveReadTimeoutSeconds, DICOM_WEB_RETRIEVE_READ_TIMEOUT_SECONDS);
    }

    @NrgPreference(defaultValue = DIMSE_RESPONSE_TIMEOUT_SECONDS_DEFAULT_VALUE)
    public String getDimseResponseTimeoutSeconds() {
        return getValue(DIMSE_RESPONSE_TIMEOUT_SECONDS);
    }

    public void setDimseResponseTimeoutSeconds(final String dimseResponseTimeoutSeconds) {
        safeSet(dimseResponseTimeoutSeconds, DIMSE_RESPONSE_TIMEOUT_SECONDS);
    }

    @NrgPreference(defaultValue = DIMSE_RETRIEVE_TIMEOUT_SECONDS_DEFAULT_VALUE)
    public String getDimseRetrieveTimeoutSeconds() {
        return getValue(DIMSE_RETRIEVE_TIMEOUT_SECONDS);
    }

    public void setDimseRetrieveTimeoutSeconds(final String dimseRetrieveTimeoutSeconds) {
        safeSet(dimseRetrieveTimeoutSeconds, DIMSE_RETRIEVE_TIMEOUT_SECONDS);
    }

    @NrgPreference(defaultValue = DIMSE_ACCEPT_TIMEOUT_SECONDS_DEFAULT_VALUE)
    public String getDimseAcceptTimeoutSeconds() {
        return getValue(DIMSE_ACCEPT_TIMEOUT_SECONDS);
    }

    public void setDimseAcceptTimeoutSeconds(final String dimseAcceptTimeoutSeconds) {
        safeSet(dimseAcceptTimeoutSeconds, DIMSE_ACCEPT_TIMEOUT_SECONDS);
    }

    @NrgPreference(defaultValue = DIMSE_CONNECT_TIMEOUT_SECONDS_DEFAULT_VALUE)
    public String getDimseConnectTimeoutSeconds() {
        return getValue(DIMSE_CONNECT_TIMEOUT_SECONDS);
    }

    public void setDimseConnectTimeoutSeconds(final String dimseConnectTimeoutSeconds) {
        safeSet(dimseConnectTimeoutSeconds, DIMSE_CONNECT_TIMEOUT_SECONDS);
    }

    @NrgPreference(defaultValue = DQR_MAX_THROTTLE_SLEEP_SECONDS_DEFAULT_VALUE)
    public String getDqrMaxThrottleSleepSeconds() {
        return getValue(DQR_MAX_THROTTLE_SLEEP_SECONDS);
    }

    public void setDqrMaxThrottleSleepSeconds(final String dqrMaxThrottleSleepSeconds) {
        safeSet(dqrMaxThrottleSleepSeconds, DQR_MAX_THROTTLE_SLEEP_SECONDS);
    }

    public int getDicomWebMetadataReadTimeoutMs() {
        return parseTimeoutMs(getDicomWebMetadataReadTimeoutSeconds(),
                DICOM_WEB_METADATA_READ_TIMEOUT_SECONDS,
                DICOM_WEB_METADATA_READ_TIMEOUT_SECONDS_DEFAULT_VALUE);
    }

    public int getDicomWebRetrieveReadTimeoutMs() {
        return parseTimeoutMs(getDicomWebRetrieveReadTimeoutSeconds(),
                DICOM_WEB_RETRIEVE_READ_TIMEOUT_SECONDS,
                DICOM_WEB_RETRIEVE_READ_TIMEOUT_SECONDS_DEFAULT_VALUE);
    }

    public int getDimseResponseTimeoutMs() {
        return parseTimeoutMs(getDimseResponseTimeoutSeconds(),
                DIMSE_RESPONSE_TIMEOUT_SECONDS,
                DIMSE_RESPONSE_TIMEOUT_SECONDS_DEFAULT_VALUE);
    }

    public int getDimseRetrieveTimeoutMs() {
        return parseTimeoutMs(getDimseRetrieveTimeoutSeconds(),
                DIMSE_RETRIEVE_TIMEOUT_SECONDS,
                DIMSE_RETRIEVE_TIMEOUT_SECONDS_DEFAULT_VALUE);
    }

    public int getDimseAcceptTimeoutMs() {
        return parseTimeoutMs(getDimseAcceptTimeoutSeconds(),
                DIMSE_ACCEPT_TIMEOUT_SECONDS,
                DIMSE_ACCEPT_TIMEOUT_SECONDS_DEFAULT_VALUE);
    }

    public int getDimseConnectTimeoutMs() {
        return parseTimeoutMs(getDimseConnectTimeoutSeconds(),
                DIMSE_CONNECT_TIMEOUT_SECONDS,
                DIMSE_CONNECT_TIMEOUT_SECONDS_DEFAULT_VALUE);
    }

    public int getDqrMaxThrottleSleepMs() {
        return parseTimeoutMs(getDqrMaxThrottleSleepSeconds(),
                DQR_MAX_THROTTLE_SLEEP_SECONDS,
                DQR_MAX_THROTTLE_SLEEP_SECONDS_DEFAULT_VALUE);
    }

    private static int parseTimeoutMs(final String value, final String prefName, final String defaultValue) {
        final String effective = (value == null || value.trim().isEmpty()) ? defaultValue : value;
        try {
            return Integer.parseInt(effective.trim()) * 1000;
        } catch (NumberFormatException e) {
            log.warn("Invalid value '{}' for preference {}; falling back to default {}", value, prefName, defaultValue);
            return Integer.parseInt(defaultValue) * 1000;
        }
    }
}
