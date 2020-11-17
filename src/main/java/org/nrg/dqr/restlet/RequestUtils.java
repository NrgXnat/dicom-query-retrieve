/*
 * org.nrg.dqr.restlet.RequestUtils
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.dqr.restlet;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.nrg.dqr.dto.PacsSearchCriteria;
import org.nrg.xnat.utils.DateRange;
import org.nrg.xnat.utils.DqrDateRange;
import org.restlet.data.Form;
import org.restlet.data.Request;

public class RequestUtils {

    private final static DateFormat EXPECTED_DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy");

    public static PacsSearchCriteria buildSearchCriteriaFromRequest(Request request)
            throws InvalidStudyDateRangeException {
        final Form searchForm = request.getEntityAsForm();
        final PacsSearchCriteria searchCriteria = new PacsSearchCriteria();
        searchCriteria.setPatientId(StringUtils.trimToNull(searchForm.getFirstValue("patientId")));
        searchCriteria.setPatientName(searchForm.getFirstValue("patientName"));
        searchCriteria.setAccessionNumber(searchForm.getFirstValue("accessionNumber"));
        DqrDateRange studyDateRange = buildStudyDateRange(searchForm);
        searchCriteria.setStudyDateRange(studyDateRange);
        return searchCriteria;
    }

    private static DqrDateRange buildStudyDateRange(Form searchForm) throws InvalidStudyDateRangeException {
        try {
            Date startRange = null, endRange = null;
            if (!StringUtils.isBlank(searchForm.getFirstValue("studyDateIsToday"))) {
                Date now = new Date();
                startRange = now;
                endRange = now;
            } else {
                if (!StringUtils.isBlank(searchForm.getFirstValue("studyDateFrom"))) {
                    startRange = EXPECTED_DATE_FORMAT.parse(searchForm.getFirstValue("studyDateFrom"));
                }
                if (!StringUtils.isBlank(searchForm.getFirstValue("studyDateTo"))) {
                    endRange = EXPECTED_DATE_FORMAT.parse(searchForm.getFirstValue("studyDateTo"));
                }
            }
            return new DqrDateRange(startRange, endRange);
        } catch (ParseException e) {
            throw new InvalidStudyDateRangeException(
                    "The dates in the study date range are expected to be in the following format: "
                            + EXPECTED_DATE_FORMAT);
        }
    }
}
