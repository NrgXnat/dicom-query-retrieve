package org.nrg.xnat.plugin;

import org.nrg.framework.annotations.XnatPlugin;
import org.nrg.xnat.configuration.DicomImportConfig;
import org.nrg.xnat.configuration.DqrConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@XnatPlugin(
        value = "dicom_query_retrieve",
        name = "DICOM Query Retrieve Plugin",
        description = "Enables users to search for images in PACS, retrieve them, and push them.",
        entityPackages = "org.nrg.dqr.domain.entities"
)
@ComponentScan({
        "org.nrg.dqr.services",
        "org.nrg.dqr.daos",
        "org.nrg.dcm.scp",
        "org.nrg.dqr.processors",
        "org.nrg.dcm.edit.mizer",
        "org.nrg.dicom.dicomedit.mizer",
        "org.nrg.dicom.mizer.service.impl",
        "org.nrg.dqr.events",
        "org.nrg.dqr.preferences",
        "org.nrg.xapi.authorization"
})
@Import({DicomImportConfig.class, DqrConfig.class})
public class DicomQueryRetrievePlugin {

//    @Bean
//    public TriggerTask pacsRequestDequeuerTask(final PacsThreadsChecker pacsRequestDequeuer) {
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