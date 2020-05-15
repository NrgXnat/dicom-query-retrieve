/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.id.OverrideStudyIdExtractor
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.id;

import static org.nrg.framework.orm.DatabaseHelper.convertPGIntervalToIntSeconds;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.jetbrains.annotations.Nullable;
import org.nrg.dcm.Extractor;
import org.nrg.xnat.helpers.merge.anonymize.DefaultAnonUtils;
import org.nrg.xnatx.dqr.domain.entities.StudyIdStudyInstanceUidMapping;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;
import org.nrg.xnatx.dqr.services.StudyIdStudyInstanceUidMappingService;

@Slf4j
public class OverrideStudyIdExtractor implements Extractor {
    public OverrideStudyIdExtractor(final StudyIdStudyInstanceUidMappingService service, final DqrPreferences preferences) {
        _service = service;
        _preferences = preferences;
    }

    public String extract(final DicomObject dicomObject) {
        final String  studyId          = StringUtils.defaultIfBlank(dicomObject.getString(Tag.StudyID), dicomObject.getString(Tag.AccessionNumber));
        final String  studyInstanceUID = dicomObject.getString(Tag.StudyInstanceUID);
        final String  script           = getStudyScript(studyInstanceUID);
        final boolean hasStudyId       = StringUtils.isNotBlank(studyId);
        if (hasStudyId) {
            if (StringUtils.contains(script, "(0020,0010)")) {
                //Study ID has already been relabeled, so even if Study IDs were inconsistent in the source data, they will have already been made consistent.
                return studyId;
            }
        }
        final List<StudyIdStudyInstanceUidMapping> mappings                        = _service.getAllForStudyInstanceUid(studyInstanceUID);
        final StudyIdStudyInstanceUidMapping       mostRecentMapping               = !mappings.isEmpty() ? mappings.get(0) : null;
        final Date                                 assumeSameSessionIfArrivedAfter = new Date(System.currentTimeMillis() - (1000 * convertPGIntervalToIntSeconds(_preferences.getAssumeSameSessionIfArrivedWithin())));
        if (mostRecentMapping != null && mostRecentMapping.getCreated() != null && mostRecentMapping.getCreated().after(assumeSameSessionIfArrivedAfter)) {
            //Mapping was added within the assumeSameSessionIfArrivedWithin interval, so this is likely a case of one study with multiple study IDs
            if (hasStudyId && !StringUtils.equals(studyId, mostRecentMapping.getStudyId())) {
                log.error("DICOM files with the same study instance UID {} but different study IDs {} and {} were received. Since they arrived within half an hour, XNAT is assuming the same session label should be used and is using the study ID that arrived first {} to determine that.", studyInstanceUID, mostRecentMapping.getStudyId(), studyId, mostRecentMapping.getStudyId());
            }
            return mostRecentMapping.getStudyId();
        }
        _service.create(StudyIdStudyInstanceUidMapping.builder().studyId(studyId).studyInstanceUid(studyInstanceUID).build());
        return studyId;
    }

    @Nullable
    private String getStudyScript(final String studyInstanceUID) {
        try {
            return DefaultAnonUtils.getService().getStudyScript(studyInstanceUID);
        } catch (Exception e) {
            log.error("Error checking whether there was a relabel script for incoming data.", e);
        }
        return null;
    }

    public SortedSet<Integer> getTags() {
        return new TreeSet<>(TAGS);
    }

    private static final List<Integer> TAGS = Arrays.asList(Tag.StudyID, Tag.StudyInstanceUID);

    private final StudyIdStudyInstanceUidMappingService _service;
    private final DqrPreferences                        _preferences;
}
