/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.command.cfind.dcm4che.tool.CFindSCUSpecificLevel
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.command.cfind.dcm4che.tool;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.net.ConfigurationException;
import org.dcm4che2.tool.dcmqr.DcmQR;
import org.dcm4che2.tool.dcmqr.DcmQR.QueryRetrieveLevel;
import org.nrg.xnatx.dqr.dicom.command.cecho.CEchoSCU;
import org.nrg.xnatx.dqr.dicom.command.cfind.SearchCriteriaTooVagueException;
import org.nrg.xnatx.dqr.dicom.command.cmove.CMoveTargetNotFoundException;
import org.nrg.xnatx.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.xnatx.dqr.dicom.strategy.orm.DicomPersonNameSearchCriteria;
import org.nrg.xnatx.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.xnatx.dqr.domain.DqrDomainObject;
import org.nrg.xnatx.dqr.domain.RequestContext;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;
import org.nrg.xnatx.dqr.dto.PacsSearchResults;
import org.nrg.xnatx.dqr.dto.StudyDateRangeLimitResults;
import org.nrg.xnatx.dqr.exceptions.DqrRuntimeException;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;
import org.nrg.xnatx.dqr.utils.DqrDateRange;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter(AccessLevel.PROTECTED)
@Slf4j
public abstract class CFindSCUSpecificLevel<T extends DqrDomainObject> {
    /**
     * Creates a new instance of the class.
     *
     * @param preferences               DQR preferences
     * @param dicomConnectionProperties The connection properties for the external AE
     * @param cechoSCU                  Used to test connectivity to the external AE
     * @param ormStrategy               The ORM strategy to use
     */
    protected CFindSCUSpecificLevel(final DqrPreferences preferences,
                                    final DicomConnectionProperties dicomConnectionProperties,
                                    final CEchoSCU cechoSCU,
                                    final OrmStrategy ormStrategy) {
        dcmQR = createDcmQR(StringUtils.defaultIfBlank(preferences.getDqrCallingAe(), dicomConnectionProperties.getLocalAeTitle()));
        dcmQR.setRemoteHost(dicomConnectionProperties.getRemoteHost());
        dcmQR.setRemotePort(dicomConnectionProperties.getRemoteQueryRetrievePort());
        dcmQR.setCalledAET(dicomConnectionProperties.getRemoteAeTitle(), true);
        dcmQR.setNoExtNegotiation(!dicomConnectionProperties.getSupportsExtendedNegotiations());
        if (cMoveRequestedOnResults()) {
            dcmQR.setMoveDest(dicomConnectionProperties.getLocalAeTitle());
        }
        this.cechoSCU    = cechoSCU;
        this.ormStrategy = ormStrategy;
    }

    /**
     * Performs a C-FIND against the select PACS.
     *
     * @param context Request context in which this operation occurs.
     * @param searchCriteria The search criteria to use for the C-FIND operation.
     * @return The results of the search.
     * @see DicomPersonNameSearchCriteria for an explanation of why we (potentially) query more than once.
     */
    public PacsSearchResults<T> cfind(
            final RequestContext context, final PacsSearchCriteria searchCriteria
    ) {
        pingPacs();

        validatePacsSearchCriteria(searchCriteria);

        dcmQR.setCancelAfter(getMaxResults());
        dcmQR.setQueryLevel(getQueryLevel());
        dcmQR.addDefReturnKeys();//This needed to be added because between the 2.0.25 and 2.0.29 versions of dcm4che2, this stopped being done by the setQueryLevel method

        if (dcmQR.getKeys().contains(Tag.NumberOfStudyRelatedInstances)) {
            dcmQR.getKeys().remove(Tag.NumberOfStudyRelatedInstances);
        }
        getReturnTagPaths().stream().map(this::dicomTagPathToArray).forEach(dcmQR::addReturnKey);

        // Counter-intuitive, but this needs to happen *after* the query level is set
        dcmQR.configureTransferCapability(false);

        try {
            dcmQR.open();
        } catch (ConfigurationException | IOException | InterruptedException e) {
            throw new DqrRuntimeException("unable to open association", e);
        }
        try {
            final List<DicomObject> dicomResults;
            try {
                dicomResults = setParamsAndSendQuery(searchCriteria);
            } catch (IOException | InterruptedException e) {
                throw new DqrRuntimeException(String.format("C-FIND failed for %s: %s", context, searchCriteria), e);
            }

            if (cMoveRequestedOnResults()) {
                dicomResults.forEach(result -> context.recordRequest(getDestinationProject(), result));
                try {
                    performCMoveOnResults(searchCriteria, dicomResults);
                } catch (IOException | InterruptedException e) {
                    throw new DqrRuntimeException(String.format("C-MOVE failed for %s: %d series into %s",
                            context, dicomResults.size(), getDestinationProject()
                    ), e);
                }
            }

            return mapDicomResultsToDomainResults(searchCriteria, dicomResults);
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

    /**
     * Validates the submitted search criteria. If the criteria are valid, this method returns quietly.
     * Otherwise, it throws {@link SearchCriteriaTooVagueException}.
     *
     * @param searchCriteria The search criteria to be validated.
     *
     * @throws SearchCriteriaTooVagueException Thrown when the search criteria are not well defined.
     */
    protected abstract void validatePacsSearchCriteria(final PacsSearchCriteria searchCriteria) throws SearchCriteriaTooVagueException;

    /**
     * These are in addition to whatever DcmQR returns.
     *
     * @return The paths to be returned.
     */
    protected abstract List<Integer> getReturnTagPaths();

    /**
     * Indicates the query/retrieve level for the particular implementation. This can be any valid value for
     * <b>QueryRetrieveLevel</b>: <b>PATIENT</b>, <b>STUDY</b>, <b>SERIES</b>, or <b>IMAGE</b>.
     *
     * @return The query/retrieve level for the particular implementation.
     */
    protected abstract QueryRetrieveLevel getQueryLevel();

    /**
     * Maps the DICOM object returned from the PACS to the domain object for the particular implementation.
     *
     * @param dicomObject The DICOM object returned from the PACS.
     *
     * @return An instance of the domain object for this implementation, populated from the DICOM object.
     */
    protected abstract T mapDicomObjectToDomainObject(final DicomObject dicomObject);

    /**
     * Wraps the populated domain objects in a {@link PacsSearchResults} instance.
     *
     * @param results                    The results from the query (usually created by {@link #mapDicomObjectToDomainObject(DicomObject)}
     * @param hasLimitedResults          Indicates whether the results were limited (e.g. paged)
     * @param studyDateRangeLimitResults Indicates whether the results were limited by a date range
     *
     * @return Returns the {@link PacsSearchResults} instance.
     */
    protected abstract PacsSearchResults<T> wrapResults(final Collection<T> results, final boolean hasLimitedResults, final StudyDateRangeLimitResults studyDateRangeLimitResults);

    protected DcmQR createDcmQR(final String localAETitle) {
        return new DcmQR(localAETitle);
    }

    protected void setSearchCriteriaInQuery(final PacsSearchCriteria searchCriteria, final String dicomPatientNameSearchCriterion) {
        searchCriteria.getDicomKeys().stream().filter(pair -> pair.getKey()[0] != Tag.PatientName && pair.getKey()[0] != Tag.StudyDate).forEach(pair -> dcmQR.addMatchingKey(pair.getKey(), pair.getValue()));
        if (!StringUtils.isBlank(dicomPatientNameSearchCriterion)) {
            dcmQR.addMatchingKey(dicomTagPathToArray(Tag.PatientName), dicomPatientNameSearchCriterion);
        }
        final DqrDateRange studyDateRange = getOrmStrategy().getResultSetLimitStrategy().limitStudyDateRange(searchCriteria).getDateRange();
        if (studyDateRange != null && studyDateRange.isBounded()) {
            dcmQR.addMatchingKey(dicomTagPathToArray(Tag.StudyDate), buildStudyDateCriterion(studyDateRange));
        }
    }

    protected List<DicomObject> setParamsAndSendQuery(final PacsSearchCriteria searchCriteria) throws IOException, InterruptedException {
        log.trace("Querying PACS {} with search criteria: {}", searchCriteria.getPacsId(), searchCriteria);
        try {
            for (final String criterion : ormStrategy.getPatientNameStrategy().dqrSearchCriteriaToDicomSearchCriteria(searchCriteria).getCriteriaInOrderOfPreference()) {
                setSearchCriteriaInQuery(searchCriteria, criterion);
                log.trace("Querying PACS {} with criterion [{}] keys:\n{}", searchCriteria.getPacsId(), criterion, dcmQR.getKeys());
                final List<DicomObject> results = dcmQR.query();
                if (!results.isEmpty()) {
                    if (log.isDebugEnabled()) {
                        log.debug("Query result:\n{}", results.stream()
                                .map(Object::toString).collect(Collectors.joining("\n")));
                    }
                    return results;
                }
                log.debug("Query returned no results");
            }
            return Collections.emptyList();
        } catch (IOException e) {
            log.error("C-FIND failed", e);
            throw e;
        } catch (InterruptedException e) {
            log.error("C-FIND interrupted", e);
            throw e;
        }
    }

    protected PacsSearchResults<T> mapDicomResultsToDomainResults(final PacsSearchCriteria searchCriteria, final List<DicomObject> dicomResults) {
        return wrapResults(dicomResults.stream().map(this::mapDicomObjectToDomainObject).collect(Collectors.toList()), dicomResults.size() == getMaxResults(), getOrmStrategy().getResultSetLimitStrategy().limitStudyDateRange(searchCriteria));
    }

    protected void performCMoveOnResults(final PacsSearchCriteria searchCriteria, final List<DicomObject> dicomResults) throws IOException, InterruptedException {
        if (dicomResults.isEmpty()) {
            reportCMoveTargetNotFound(searchCriteria);
        } else {
            log.debug("Starting C-MOVE for {}", dicomResults);
            dcmQR.move(dicomResults);
        }
    }

    protected boolean cMoveRequestedOnResults() {
        return false;
    }

    /**
     * Destination XNAT project, used only for series retrieval records.
     * @return destination project name for C-MOVE, or null if not applicable
     */
    protected String getDestinationProject() {
        return null;
    }

    protected void reportCMoveTargetNotFound(final PacsSearchCriteria searchCriteria) {
        throw new CMoveTargetNotFoundException(searchCriteria.toString());
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

    private String buildStudyDateCriterion(final DqrDateRange studyDateRange) {
        final String startDate = studyDateRange.isBoundedAtStart() ? DqrDateRange.formatDicomDate(studyDateRange.getStart()) : "";
        final String endDate   = studyDateRange.isBoundedAtEnd() ? DqrDateRange.formatDicomDate(studyDateRange.getEnd()) : "";
        return startDate + DICOM_DATE_RANGE_SEPARATOR + endDate;
    }

    private static final String DICOM_DATE_RANGE_SEPARATOR = "-";

    private final DcmQR       dcmQR;
    private final CEchoSCU    cechoSCU;
    private final OrmStrategy ormStrategy;
}
