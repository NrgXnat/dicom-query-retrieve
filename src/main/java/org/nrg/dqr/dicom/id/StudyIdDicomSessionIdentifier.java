/*
 * StudyIdDicomSessionIdentifier
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */
package org.nrg.dqr.dicom.id;

import java.util.List;

import org.dcm4che2.data.Tag;
import org.nrg.dcm.Extractor;

import com.google.common.collect.ImmutableList;
import org.nrg.dcm.TextExtractor;

public class StudyIdDicomSessionIdentifier {
    private static final ImmutableList<Extractor> sessionExtractors;

    static {
        final ImmutableList.Builder<Extractor> sessb = new ImmutableList.Builder<Extractor>();
        sessb.add(new OverrideStudyIdExtractor(Tag.StudyID,Tag.StudyInstanceUID));
        sessionExtractors = sessb.build();
    }

    public static final List<Extractor> getSessionExtractors() {
        return sessionExtractors;
    }

    private StudyIdDicomSessionIdentifier() {
    }
}
