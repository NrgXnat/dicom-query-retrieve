package org.nrg.xnat.plugin;

import org.nrg.dqr.events.PacsRequestDequeuer;
import org.nrg.framework.annotations.XnatDataModel;
import org.nrg.framework.annotations.XnatPlugin;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.scheduling.config.TriggerTask;
import org.springframework.scheduling.support.PeriodicTrigger;

import java.util.concurrent.TimeUnit;

@XnatPlugin(value = "dicom_query_retrieve", name = "DICOM Query Retrieve Plugin", description = "Enables users to search for images in PACS, retrieve them, and push them.",
    entityPackages = "org.nrg.dqr.domain.entities")
@ComponentScan({"org.nrg.dqr.services", "org.nrg.dqr.daos", "org.nrg.dcm.scp", "org.nrg.dcm.edit.mizer",
        "org.nrg.dicom.dicomedit.mizer", "org.nrg.dicom.mizer.service.impl", "org.nrg.dqr.events", "org.nrg.dqr.preferences"})
public class DicomQueryRetrievePlugin {

//    @Bean
//    public TriggerTask pacsRequestDequeuerTask(final PacsRequestDequeuer pacsRequestDequeuer) {
//        long availabilityIntervalSeconds = 60;
//        if(XDAT.getSiteConfigPreferences()!=null && XDAT.getSiteConfigPreferences().get("pacsAvailabilityCheckFrequency")!=null){
//            Object availabilityInterval = XDAT.getSiteConfigPreferences().get("pacsAvailabilityCheckFrequency");
//            String availabilityString = availabilityInterval.toString();
//            try{
//                availabilityIntervalSeconds = SiteConfigPreferences.convertPGIntervalToSeconds(availabilityString);
//            }
//            catch(Throwable e){
//
//            }
//        }
//
////        return new TriggerTask(
////                pacsRequestDequeuer,
////                new PeriodicTrigger(1000 * availabilityIntervalSeconds)
////        );
//        _service.triggerEvent(new PreferenceEvent("refreshGuestFrequency", String.valueOf(_siteConfigPreferences.getRefreshGuestFrequency())));
//    }
}