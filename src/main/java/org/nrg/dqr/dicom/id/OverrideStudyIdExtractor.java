
package org.nrg.dqr.dicom.id;

import static org.nrg.framework.orm.DatabaseHelper.convertPGIntervalToIntSeconds;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.jetbrains.annotations.Nullable;
import org.nrg.dcm.Extractor;
import org.nrg.dqr.domain.entities.StudyIdStudyInstanceUidMapping;
import org.nrg.dqr.preferences.DqrPreferences;
import org.nrg.dqr.services.StudyIdStudyInstanceUidMappingService;
import org.nrg.xnat.helpers.merge.AnonUtils;
import org.nrg.xnat.helpers.merge.anonymize.DefaultAnonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class OverrideStudyIdExtractor implements Extractor {
    @Autowired
    public OverrideStudyIdExtractor(final DqrPreferences preferences, final StudyIdStudyInstanceUidMappingService mappingService) {
        _preferences = preferences;
        _mappingService = mappingService;
        _tags = Arrays.asList(Tag.StudyID, Tag.StudyInstanceUID);
        _anonymizer = DefaultAnonUtils.getService();
    }

    public String extract(final DicomObject object) {
        final String studyId          = object.getString(_tags.get(0));
        final String studyInstanceUID = object.getString(_tags.get(1));
        final String script           = getStudyScript(studyInstanceUID);
        if (StringUtils.contains(script, "(0020,0010)")) {
            //Study ID has already been relabeled, so even if Study IDs were inconsistent in the source data, they will have already been made consistent.
            return studyId;
        }

        final List<StudyIdStudyInstanceUidMapping> mappings                        = _mappingService.getAllForStudyInstanceUid(studyInstanceUID);
        final StudyIdStudyInstanceUidMapping       mostRecentMapping               = !mappings.isEmpty() ? mappings.get(0) : null;
        final Date                                 assumeSameSessionIfArrivedAfter = new Date(System.currentTimeMillis() - (1000 * convertPGIntervalToIntSeconds(_preferences.getAssumeSameSessionIfArrivedWithin())));
        if (mostRecentMapping != null && mostRecentMapping.getCreated() != null && mostRecentMapping.getCreated().after(assumeSameSessionIfArrivedAfter)) {
            //Mapping was added within the assumeSameSessionIfArrivedWithin interval, so this is likely a case of one study with multiple study IDs
            if (!StringUtils.equals(studyId, mostRecentMapping.getStudyId())) {
                log.error("DICOM files with the same StudyInstanceUID ({}) and different study IDs ({}, {}) received. Since they were received within half an hour, XNAT is assuming the same session label should be used and is using the study ID that arrived first ({}) to determine that.", studyInstanceUID, mostRecentMapping.getStudyId(), studyId, mostRecentMapping.getStudyId());
            }
            return mostRecentMapping.getStudyId();
        }

        _mappingService.create(StudyIdStudyInstanceUidMapping.builder().studyId(studyId).studyInstanceUid(studyInstanceUID).build());
        return studyId;
    }

    @Nullable
    private String getStudyScript(final String studyInstanceUID) {
        try {
            return _anonymizer.getStudyScript(studyInstanceUID);
        } catch (Exception e) {
            log.error("Error checking whether there was a relabel script for incoming data.", e);
            return null;
        }
    }

    public SortedSet<Integer> getTags() {
        return new TreeSet<>(_tags);
    }

    private final DqrPreferences                        _preferences;
    private final StudyIdStudyInstanceUidMappingService _mappingService;
    private final List<Integer>                         _tags;
    private final AnonUtils                             _anonymizer;
}
