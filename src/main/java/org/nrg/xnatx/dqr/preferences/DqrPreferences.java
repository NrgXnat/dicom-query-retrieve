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
public class DqrPreferences extends EventTriggeringAbstractPreferenceBean {
    public static final String DQR_TOOL_ID = "dqr";

    @Autowired
    public DqrPreferences(final NrgPreferenceService preferenceService, final NrgEventServiceI eventService, final ConfigPaths configPaths, final OrderedProperties initPrefs) {
        super(preferenceService, eventService, configPaths, initPrefs);
    }

    @NrgPreference(defaultValue = "XNAT")
    public String getDqrCallingAe() {
        return getValue("dqrCallingAe");
    }

    @SuppressWarnings("unused")
    public void setDqrCallingAe(final String dqrCallingAe) {
        try {
            set(dqrCallingAe, "dqrCallingAe");
        } catch (InvalidPreferenceName e) {
            log.error("Invalid preference name dqrCallingAe: something is very wrong here.", e);
        }
    }

    @NrgPreference(defaultValue = "1 minute")
    public String getPacsAvailabilityCheckFrequency() {
        return getValue("pacsAvailabilityCheckFrequency");
    }

    @SuppressWarnings("unused")
    public void setPacsAvailabilityCheckFrequency(final String pacsAvailabilityCheckFrequency) {
        try {
            set(pacsAvailabilityCheckFrequency, "pacsAvailabilityCheckFrequency");
        } catch (InvalidPreferenceName e) {
            log.error("Invalid preference name 'pacsAvailabilityCheckFrequency': something is very wrong here.", e);
        }
    }

    @NrgPreference(defaultValue = "false")
    public boolean getNotifyAdminOnImport() {
        return getBooleanValue("notifyAdminOnImport");
    }

    @SuppressWarnings("unused")
    public void setNotifyAdminOnImport(final boolean notifyAdminOnImport) {
        try {
            setBooleanValue(notifyAdminOnImport, "notifyAdminOnImport");
        } catch (InvalidPreferenceName e) {
            log.error("Invalid preference name 'notifyAdminOnImport': something is very wrong here.", e);
        }
    }

    @NrgPreference(defaultValue = "30 minutes")
    public String getAssumeSameSessionIfArrivedWithin() {
        return getValue("assumeSameSessionIfArrivedWithin");
    }

    @SuppressWarnings("unused")
    public void setAssumeSameSessionIfArrivedWithin(final String assumeSameSessionIfArrivedWithin) {
        try {
            set(assumeSameSessionIfArrivedWithin, "assumeSameSessionIfArrivedWithin");
        } catch (InvalidPreferenceName e) {
            log.error("Invalid preference name 'assumeSameSessionIfArrivedWithin': something is very wrong here.", e);
        }
    }

    @NrgPreference(defaultValue = "false")
    public boolean getAllowAllUsersToUseDqr() {
        return getBooleanValue("allowAllUsersToUseDqr");
    }

    @SuppressWarnings("unused")
    public void setAllowAllUsersToUseDqr(final boolean allowAllUsersToUseDqr) {
        try {
            setBooleanValue(allowAllUsersToUseDqr, "allowAllUsersToUseDqr");
        } catch (InvalidPreferenceName e) {
            log.error("Invalid preference name 'allowAllUsersToUseDqr': something is very wrong here.", e);
        }
    }

    @NrgPreference(defaultValue = "false")
    public boolean getAllowAllProjectsToUseDqr() {
        return getBooleanValue("allowAllProjectsToUseDqr");
    }

    @SuppressWarnings("unused")
    public void setAllowAllProjectsToUseDqr(final boolean allowAllProjectsToUseDqr) {
        try {
            setBooleanValue(allowAllProjectsToUseDqr, "allowAllProjectsToUseDqr");
        } catch (InvalidPreferenceName e) {
            log.error("Invalid preference name 'allowAllProjectsToUseDqr': something is very wrong here.", e);
        }
    }

    @NrgPreference(defaultValue = "org.nrg.xnatx.dqr.dicom.strategy.orm.BasicOrmStrategy")
    public String getOrmStrategy() {
        return getStringValue("ormStrategy");
    }

    public void setOrmStrategy(final String ormStrategy) {
        try {
            set(ormStrategy, "ormStrategy");
        } catch (InvalidPreferenceName e) {
            log.error("Invalid preference name 'ormStrategy': something is very wrong here.", e);
        }
    }

    @NrgPreference(defaultValue = "org.nrg.xnatx.dqr.dicom.strategy.orm.BasicPatientNameStrategy")
    public String getPatientNameStrategy() {
        return getStringValue("patientNameStrategy");
    }

    @SuppressWarnings("unused")
    public void setPatientNameStrategy(final String patientNameStrategy) {
        try {
            set(patientNameStrategy, "patientNameStrategy");
        } catch (InvalidPreferenceName e) {
            log.error("Invalid preference name 'patientNameStrategy': something is very wrong here.", e);
        }
    }

    @NrgPreference(defaultValue = "org.nrg.xnatx.dqr.dicom.strategy.orm.BasicResultSetLimitStrategy")
    public String getResultSetLimitStrategy() {
        return getStringValue("resultSetLimitStrategy");
    }

    @SuppressWarnings("unused")
    public void setResultSetLimitStrategy(final String resultSetLimitStrategy) {
        try {
            set(resultSetLimitStrategy, "resultSetLimitStrategy");
        } catch (InvalidPreferenceName e) {
            log.error("Invalid preference name 'resultSetLimitStrategy': something is very wrong here.", e);
        }
    }

    @NrgPreference(defaultValue = "false")
    public boolean getLeavePacsAuditTrail() {
        return getBooleanValue("leavePacsAuditTrail");
    }

    @SuppressWarnings("unused")
    public void setLeavePacsAuditTrail(final boolean leavePacsAuditTrail) {
        try {
            setBooleanValue(leavePacsAuditTrail, "leavePacsAuditTrail");
        } catch (InvalidPreferenceName e) {
            log.error("Invalid preference name 'leavePacsAuditTrail': something is very wrong here.", e);
        }
    }

    @NrgPreference(defaultValue = "false")
    public boolean getDicomWebEnabled() {
        return getBooleanValue("dicomWebEnabled");
    }

    @SuppressWarnings("unused")
    public void setDicomWebEnabled(final boolean dicomWebEnabled) {
        try {
            setBooleanValue(dicomWebEnabled, "dicomWebEnabled");
        } catch (InvalidPreferenceName e) {
            log.error("Invalid preference name 'dicomWebEnabled': something is very wrong here.", e);
        }
    }

}
