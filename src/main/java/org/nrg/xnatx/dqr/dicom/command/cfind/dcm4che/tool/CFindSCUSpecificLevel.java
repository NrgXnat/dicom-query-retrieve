/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.command.cfind.dcm4che.tool.CFindSCUSpecificLevel
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.command.cfind.dcm4che.tool;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.tool.dcmqr.DcmQR;
import org.dcm4che2.tool.dcmqr.DcmQR.QueryRetrieveLevel;
import org.nrg.xnatx.dqr.dicom.command.cecho.CEchoSCU;
import org.nrg.xnatx.dqr.dicom.command.cfind.SearchCriteriaTooVagueException;
import org.nrg.xnatx.dqr.dicom.command.cmove.CMoveTargetNotFoundException;
import org.nrg.xnatx.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.xnatx.dqr.dicom.strategy.orm.DicomPersonNameSearchCriteria;
import org.nrg.xnatx.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.xnatx.dqr.domain.DqrDomainObject;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;
import org.nrg.xnatx.dqr.dto.PacsSearchResults;
import org.nrg.xnatx.dqr.dto.StudyDateRangeLimitResults;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;
import org.nrg.xnatx.dqr.utils.DqrDateRange;
import org.nrg.xnatx.dqr.utils.DqrRuntimeException;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public abstract class CFindSCUSpecificLevel<T extends DqrDomainObject> {
    protected CFindSCUSpecificLevel(final DqrPreferences preferences, final DicomConnectionProperties dicomConnectionProperties, final CEchoSCU cechoSCU, final OrmStrategy ormStrategy) {
        Object callingAeObject = preferences.get("dqrCallingAe");
        String callingAe       = dicomConnectionProperties.getLocalAeTitle();
        if (callingAeObject != null && callingAeObject.toString() != null) {
            callingAe = callingAeObject.toString();
        }
        dcmQR = createDcmQR(callingAe);
        dcmQR.setRemoteHost(dicomConnectionProperties.getRemoteHost());
        dcmQR.setRemotePort(dicomConnectionProperties.getRemoteQueryRetrievePort());
        dcmQR.setCalledAET(dicomConnectionProperties.getRemoteAeTitle(), true);
        dcmQR.setNoExtNegotiation(!dicomConnectionProperties.getSupportsExtendedNegotiations());
        if (cMoveRequestedOnResults()) {
            dcmQR.setMoveDest(dicomConnectionProperties.getLocalAeTitle());
        }
        this.cechoSCU = cechoSCU;
        this.ormStrategy = ormStrategy;
    }

    protected DcmQR createDcmQR(String localAETitle) {
        return new DcmQR(localAETitle);
    }

    /**
     * Performs a C-FIND against the select PACS.
     *
     * @param searchCriteria The search criteria to use for the C-FIND operation.
     *
     * @return The results of the search.
     *
     * @see DicomPersonNameSearchCriteria for an explanation of why we (potentially) query more than once.
     */
    public PacsSearchResults<T> cfind(final PacsSearchCriteria searchCriteria) {
        try {
            pingPacs();

            validatePacsSearchCriteria(searchCriteria);

            dcmQR.setCancelAfter(getMaxResults());
            dcmQR.setQueryLevel(getQueryLevel());
            dcmQR.addDefReturnKeys();//This needed to be added because between the 2.0.25 and 2.0.29 versions of dcm4che2, this stopped being done by the setQueryLevel method
            if (dcmQR.getKeys().contains(Tag.NumberOfStudyRelatedInstances)) {
                dcmQR.getKeys().remove(Tag.NumberOfStudyRelatedInstances);
            }
            for (int returnTagPath : getReturnTagPaths()) {
                dcmQR.addReturnKey(dicomTagPathToArray(returnTagPath));
            }
            // Counter-intuitive, but this needs to happen *after* the query level is set
            dcmQR.configureTransferCapability(false);
            dcmQR.open();

            final List<DicomObject> dicomResults = setParamsAndSendQuery(searchCriteria);

            if (cMoveRequestedOnResults()) {
                performCMoveOnResults(searchCriteria, dicomResults);
            }

            return mapDicomResultsToDomainResults(searchCriteria, dicomResults);

        } catch (DqrRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                dcmQR.close();
            } catch (NullPointerException ignored) {
                // dcmQR throws these intermittently when closing the association, for unknown reasons
            } catch (Exception e) {
                log.error("There was a problem closing the DICOM network connection used for the C-FIND command", e);
            }
        }
    }

    protected void setSearchCriteriaInQuery(final PacsSearchCriteria searchCriteria,
                                            final String dicomPatientNameSearchCriterion) {

        if (!StringUtils.isBlank(searchCriteria.getPatientId())) {
            dcmQR.addMatchingKey(dicomTagPathToArray(Tag.PatientID), searchCriteria.getPatientId());
        }
        if (!StringUtils.isBlank(dicomPatientNameSearchCriterion)) {
            dcmQR.addMatchingKey(dicomTagPathToArray(Tag.PatientName), dicomPatientNameSearchCriterion);
        }
        if (!StringUtils.isBlank(searchCriteria.getStudyInstanceUid())) {
            dcmQR.addMatchingKey(dicomTagPathToArray(Tag.StudyInstanceUID), searchCriteria.getStudyInstanceUid());
        }
        if (!StringUtils.isBlank(searchCriteria.getSeriesInstanceUid())) {
            dcmQR.addMatchingKey(dicomTagPathToArray(Tag.SeriesInstanceUID), searchCriteria.getSeriesInstanceUid());
        }
        if (!StringUtils.isBlank(searchCriteria.getAccessionNumber())) {
            dcmQR.addMatchingKey(dicomTagPathToArray(Tag.AccessionNumber), searchCriteria.getAccessionNumber());
        }

        if (!StringUtils.isBlank(searchCriteria.getDob())) {
            dcmQR.addMatchingKey(dicomTagPathToArray(Tag.PatientBirthDate), searchCriteria.getDob());
        }
        if (!StringUtils.isBlank(searchCriteria.getModality())) {
            dcmQR.addMatchingKey(dicomTagPathToArray(Tag.ModalitiesInStudy), searchCriteria.getModality());
        }

        DqrDateRange studyDateRange = getOrmStrategy().getResultSetLimitStrategy().limitStudyDateRange(searchCriteria)
                                                      .getDateRange();
        if (null != studyDateRange && studyDateRange.isBounded()) {
            dcmQR.addMatchingKey(dicomTagPathToArray(Tag.StudyDate), buildStudyDateCriterion(studyDateRange));
        }
    }

    private String buildStudyDateCriterion(DqrDateRange studyDateRange) {
        String startDate = "", endDate = "";
        if (studyDateRange.isBoundedAtStart()) {
            startDate = DqrDateRange.format(studyDateRange.getStart());
        }
        if (studyDateRange.isBoundedAtEnd()) {
            endDate = DqrDateRange.format(studyDateRange.getEnd());
        }
        return startDate + DICOM_DATE_RANGE_SEPARATOR + endDate;
    }

    protected List<DicomObject> setParamsAndSendQuery(final PacsSearchCriteria searchCriteria) throws IOException, InterruptedException {
        List<DicomObject> dicomResults = null;
        for (String dicomPatientNameSearchCriterion : ormStrategy.getPatientNameStrategy()
                                                                 .dqrSearchCriteriaToDicomSearchCriteria(searchCriteria).getCriteriaInOrderOfPreference()) {
            setSearchCriteriaInQuery(searchCriteria, dicomPatientNameSearchCriterion);
            dicomResults = dcmQR.query();
            if (dicomResults.size() > 0) {
                break;
            }
        }
        return dicomResults;
    }

    protected PacsSearchResults<T> mapDicomResultsToDomainResults(final PacsSearchCriteria searchCriteria, final List<DicomObject> dicomResults) {
        return wrapResults(dicomResults.stream().map(this::mapDicomObjectToDomainObject).collect(Collectors.toList()), dicomResults.size() == getMaxResults(), getOrmStrategy().getResultSetLimitStrategy().limitStudyDateRange(searchCriteria));
    }

    protected void performCMoveOnResults(PacsSearchCriteria searchCriteria, List<DicomObject> dicomResults) throws IOException, InterruptedException {
        if (dicomResults.isEmpty()) {
            reportCMoveTargetNotFound(searchCriteria);
        } else {
            dcmQR.move(dicomResults);
        }
    }

    protected boolean cMoveRequestedOnResults() {
        return false;
    }

    protected void reportCMoveTargetNotFound(PacsSearchCriteria searchCriteria) {
        throw new CMoveTargetNotFoundException(searchCriteria.toString());
    }

    @SuppressWarnings("unused")
    protected DcmQR getDcmQR() {
        return dcmQR;
    }

    @SuppressWarnings("unused")
    protected CEchoSCU getCechoSCU() {
        return cechoSCU;
    }

    protected OrmStrategy getOrmStrategy() {
        return ormStrategy;
    }

    protected int[] dicomTagPathToArray(final int dicomTagPath) {
        return new int[]{
            dicomTagPath
        };
    }

    private void pingPacs() {
        cechoSCU.cecho();
    }

    private int getMaxResults() {
        return getOrmStrategy().getResultSetLimitStrategy().getMaxResultsForQueryLevel(getQueryLevel());
    }

    /**
     * @param searchCriteria The search criteria to be validated.
     *
     * @throws SearchCriteriaTooVagueException Thrown when the search criteria are not well defined.
     */
    protected abstract void validatePacsSearchCriteria(final PacsSearchCriteria searchCriteria)
        throws SearchCriteriaTooVagueException;

    /**
     * These are in addition to whatever DcmQR returns.
     *
     * @return The paths
     */
    protected abstract List<Integer> getReturnTagPaths();

    protected abstract QueryRetrieveLevel getQueryLevel();

    protected abstract T mapDicomObjectToDomainObject(final DicomObject d);

    protected abstract PacsSearchResults<T> wrapResults(final Collection<T> results, final boolean hasLimitedResults, final StudyDateRangeLimitResults studyDateRangeLimitResults);

    private final static String DICOM_DATE_RANGE_SEPARATOR = "-";

    private final DcmQR dcmQR;

    private final CEchoSCU cechoSCU;

    private final OrmStrategy ormStrategy;
}
