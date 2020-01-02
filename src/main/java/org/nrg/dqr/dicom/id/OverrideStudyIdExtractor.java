
package org.nrg.dqr.dicom.id;

import org.apache.commons.lang3.StringUtils;
import org.dcm4che2.data.DicomObject;
import org.nrg.dcm.Extractor;
import org.nrg.dqr.domain.entities.StudyIdStudyInstanceUidMapping;
import org.nrg.dqr.preferences.DqrPreferences;
import org.nrg.dqr.services.StudyIdStudyInstanceUidMappingService;
import org.nrg.xdat.XDAT;
import org.nrg.xnat.helpers.merge.anonymize.DefaultAnonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.nrg.framework.orm.DatabaseHelper.convertPGIntervalToIntSeconds;

import java.util.*;

public class OverrideStudyIdExtractor implements Extractor {

    private final List<Integer> tags;
    private static final Logger _log = LoggerFactory.getLogger(OverrideStudyIdExtractor.class);

    public OverrideStudyIdExtractor(int tag, int referenceTag) {
        this.tags = Arrays.asList(tag,referenceTag);
    }

    public String extract(DicomObject o) {
        String studyId = o.getString(this.tags.get(0));
        String studyInstanceUID = o.getString(this.tags.get(1));
        String script = null;
        try {
            script = DefaultAnonUtils.getService().getStudyScript(studyInstanceUID);
        }
        catch(Exception e){
            _log.error("Error checking whether there was a relabel script for incoming data.",e);
        }
        if(script!=null && script.contains("(0020,0010)")){
            //Study ID has already been relabeled, so even if Study IDs were inconsistent in the source data, they will have already been made consistent.
            return studyId;
        }
        else{
            StudyIdStudyInstanceUidMappingService mappingService = XDAT.getContextService().getBean(StudyIdStudyInstanceUidMappingService.class);
            List<StudyIdStudyInstanceUidMapping> mappings = mappingService.getAllForStudyInstanceUid(studyInstanceUID);
            StudyIdStudyInstanceUidMapping mostRecentMapping = mappings.size() > 0 ? mappings.get(0) : null;
            String interval = XDAT.getContextService().getBean(DqrPreferences.class).getAssumeSameSessionIfArrivedWithin();
            Date assumeSameSessionIfArrivedAfter = new Date(System.currentTimeMillis() - (1000 * convertPGIntervalToIntSeconds(interval)));
            if (mostRecentMapping != null && mostRecentMapping.getCreated() != null && mostRecentMapping.getCreated().after(assumeSameSessionIfArrivedAfter)) {
                //Mapping was added within the assumeSameSessionIfArrivedWithin interval, so this is likely a case of one study with multiple study IDs
                if (!StringUtils.equals(studyId, mostRecentMapping.getStudyId())) {
                    _log.error("DICOM files with the same StudyInstanceUID (" + studyInstanceUID + ") and different study IDs (" + mostRecentMapping.getStudyId() + ", " + studyId + ") received. Since they were received within half an hour, XNAT is assuming the same session label should be used and is using the study ID that arrived first (" + mostRecentMapping.getStudyId() + ") to determine that.");
                }
                return mostRecentMapping.getStudyId();
            } else {
                StudyIdStudyInstanceUidMapping mapping = new StudyIdStudyInstanceUidMapping();
                mapping.setStudyId(studyId);
                mapping.setStudyInstanceUid(studyInstanceUID);
                mappingService.create(mapping);
                return studyId;
            }
        }
    }

    public SortedSet<Integer> getTags() {
        return new TreeSet<>(this.tags);
    }

}
