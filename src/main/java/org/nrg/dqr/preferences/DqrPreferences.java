/*
 * web: org.nrg.xnat.preferences.AutomationPreferences
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.dqr.preferences;

import lombok.extern.slf4j.Slf4j;
import org.nrg.framework.configuration.ConfigPaths;
import org.nrg.framework.services.NrgEventService;
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
    public DqrPreferences(final NrgPreferenceService preferenceService, final NrgEventService eventService, final ConfigPaths configPaths, final OrderedProperties initPrefs) {
        super(preferenceService, eventService, configPaths, initPrefs);
    }

    @NrgPreference(defaultValue = "XNAT")
    public String getDqrCallingAe() {
        return getValue("dqrCallingAe");
    }

    public void setDqrCallingAe(final String dqrCallingAe) {
        try {
            set(dqrCallingAe, "dqrCallingAe");
        } catch (InvalidPreferenceName e) {
            log.error("Invalid preference name dqrCallingAe: something is very wrong here.", e);
        }
    }

    @NrgPreference(defaultValue = "10 minutes")
    public String getPacsAvailabilityCheckFrequency() {
        return getValue("pacsAvailabilityCheckFrequency");
    }

    public void setPacsAvailabilityCheckFrequency(final String pacsAvailabilityCheckFrequency) {
        try {
            set(pacsAvailabilityCheckFrequency, "pacsAvailabilityCheckFrequency");
        } catch (InvalidPreferenceName e) {
            log.error("Invalid preference name 'pacsAvailabilityCheckFrequency': something is very wrong here.", e);
        }
    }
}
