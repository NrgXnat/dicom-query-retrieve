/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.command.cfind.dcm4che.tool.CFindSCUSpecificLevel
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2024, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.command.cfind.dcm4che.tool;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.nrg.xnatx.dqr.dicom.command.cecho.CEchoSCU;
import org.nrg.xnatx.dqr.dicom.command.cfind.SearchCriteriaTooVagueException;
import org.nrg.xnatx.dqr.dicom.command.cfind.dcm4che3.Dcm4che3CFindSCU;
import org.nrg.xnatx.dqr.dicom.command.cmove.CMoveFailureException;
import org.nrg.xnatx.dqr.dicom.command.cmove.CMoveTargetNotFoundException;
import org.nrg.xnatx.dqr.dicom.command.dcm4che3.QueryRetrieveLevel;
import org.nrg.xnatx.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.xnatx.dqr.dicom.strategy.orm.DicomPersonNameSearchCriteria;
import org.nrg.xnatx.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.xnatx.dqr.domain.DqrDomainObject;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;
import org.nrg.xnatx.dqr.dto.PacsSearchResults;
import org.nrg.xnatx.dqr.dto.StudyDateRangeLimitResults;
import org.nrg.xnatx.dqr.exceptions.DqrRuntimeException;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;
import org.nrg.xnatx.dqr.utils.DqrDateRange;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter(AccessLevel.PROTECTED)
@Slf4j
public abstract class CFindSCUSpecificLevel<T extends DqrDomainObject> {

    private final Dcm4che3CFindSCU cfindSCU;
    private final CEchoSCU cechoSCU;
    private final OrmStrategy ormStrategy;
    private final DicomConnectionProperties connectionProperties;

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
        String localAETitle = StringUtils.defaultIfBlank(preferences.getDqrCallingAe(), dicomConnectionProperties.getLocalAeTitle());
        this.cfindSCU = new Dcm4che3CFindSCU(localAETitle, dicomConnectionProperties);
        this.connectionProperties = dicomConnectionProperties;
        this.cechoSCU = cechoSCU;
        this.ormStrategy = ormStrategy;
    }

    /**
     * Performs a C-FIND against the select PACS.
     *
     * @param searchCriteria The search criteria to use for the C-FIND operation.
     * @return The results of the search.
     * @see DicomPersonNameSearchCriteria for an explanation of why we (potentially) query more than once.
     */
    public PacsSearchResults<T> cfind(final PacsSearchCriteria searchCriteria) {
        pingPacs();

        validatePacsSearchCriteria(searchCriteria);

        cfindSCU.setCancelAfter(getMaxResults());
        cfindSCU.setQueryLevel(getQueryLevel());
        cfindSCU.addDefReturnKeys();

        // Add additional return keys
        getReturnTagPaths().stream().map(this::dicomTagPathToArray).forEach(cfindSCU::addReturnKey);

        try {
            List<Attributes> dicomResults = setParamsAndSendQuery(searchCriteria);

            if (cMoveRequestedOnResults()) {
                performCMoveOnResults(searchCriteria, dicomResults);
            }

            return mapDicomResultsToDomainResults(searchCriteria, dicomResults);
        } catch (DqrRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new DqrRuntimeException(e);
        } finally {
            try {
                cfindSCU.close();
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
     * @throws SearchCriteriaTooVagueException Thrown when the search criteria are not well defined.
     */
    protected abstract void validatePacsSearchCriteria(final PacsSearchCriteria searchCriteria) throws SearchCriteriaTooVagueException;

    /**
     * These are in addition to whatever default return keys are added.
     *
     * @return The paths to be returned.
     */
    protected abstract List<Integer> getReturnTagPaths();

    /**
     * Indicates the query/retrieve level for the particular implementation.
     *
     * @return The query/retrieve level for the particular implementation.
     */
    protected abstract QueryRetrieveLevel getQueryLevel();

    /**
     * Maps the DICOM Attributes returned from the PACS to the domain object for the particular implementation.
     *
     * @param attributes The DICOM Attributes returned from the PACS.
     * @return An instance of the domain object for this implementation, populated from the Attributes.
     */
    protected abstract T mapAttributesToDomainObject(final Attributes attributes);

    /**
     * Wraps the populated domain objects in a {@link PacsSearchResults} instance.
     *
     * @param results                    The results from the query
     * @param hasLimitedResults          Indicates whether the results were limited (e.g. paged)
     * @param studyDateRangeLimitResults Indicates whether the results were limited by a date range
     * @return Returns the {@link PacsSearchResults} instance.
     */
    protected abstract PacsSearchResults<T> wrapResults(final Collection<T> results, final boolean hasLimitedResults, final StudyDateRangeLimitResults studyDateRangeLimitResults);

    protected void setSearchCriteriaInQuery(final PacsSearchCriteria searchCriteria, final String dicomPatientNameSearchCriterion) {
        cfindSCU.clearKeys();

        // Add matching keys from search criteria
        searchCriteria.getDicomKeys().stream()
                .filter(pair -> pair.getKey()[0] != Tag.PatientName && pair.getKey()[0] != Tag.StudyDate)
                .forEach(pair -> cfindSCU.addMatchingKey(pair.getKey(), pair.getValue()));

        if (!StringUtils.isBlank(dicomPatientNameSearchCriterion)) {
            cfindSCU.addMatchingKey(dicomTagPathToArray(Tag.PatientName), dicomPatientNameSearchCriterion);
        }

        final DqrDateRange studyDateRange = getOrmStrategy().getResultSetLimitStrategy().limitStudyDateRange(searchCriteria).getDateRange();
        if (studyDateRange != null && studyDateRange.isBounded()) {
            cfindSCU.addMatchingKey(dicomTagPathToArray(Tag.StudyDate), studyDateRange.getStudyDateCriterion());
        }

        // Re-add return keys after clearing
        cfindSCU.addDefReturnKeys();
        getReturnTagPaths().stream().map(this::dicomTagPathToArray).forEach(cfindSCU::addReturnKey);
    }

    protected List<Attributes> setParamsAndSendQuery(final PacsSearchCriteria searchCriteria) throws Exception {
        log.debug("Querying PACS {} with search criteria: {}", searchCriteria.getPacsId(), searchCriteria);

        for (final String criterion : ormStrategy.getPatientNameStrategy().dqrSearchCriteriaToDicomSearchCriteria(searchCriteria).getCriteriaInOrderOfPreference()) {
            setSearchCriteriaInQuery(searchCriteria, criterion);
            log.debug("Querying PACS {} with criterion {}: {}", searchCriteria.getPacsId(), criterion, cfindSCU.getKeys());

            final List<Attributes> results = cfindSCU.query();
            if (!results.isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("Query with criterion {} got results:\n{}", criterion, results.stream().map(Object::toString).collect(Collectors.joining("\n")));
                }
                return results;
            }
            log.debug("Query with criterion {} got no results", criterion);
        }
        return Collections.emptyList();
    }

    protected PacsSearchResults<T> mapDicomResultsToDomainResults(final PacsSearchCriteria searchCriteria, final List<Attributes> dicomResults) {
        return wrapResults(
                dicomResults.stream().map(this::mapAttributesToDomainObject).collect(Collectors.toList()),
                dicomResults.size() == getMaxResults(),
                getOrmStrategy().getResultSetLimitStrategy().limitStudyDateRange(searchCriteria)
        );
    }

    protected void performCMoveOnResults(final PacsSearchCriteria searchCriteria, final List<Attributes> dicomResults) {
        if (dicomResults.isEmpty()) {
            reportCMoveTargetNotFound(searchCriteria);
        } else {
            // TODO: Implement C-MOVE using dcm4che3 when needed
            throw new UnsupportedOperationException("C-MOVE not yet implemented for dcm4che3");
        }
    }

    protected boolean cMoveRequestedOnResults() {
        return false;
    }

    protected void reportCMoveTargetNotFound(final PacsSearchCriteria searchCriteria) {
        throw new CMoveTargetNotFoundException(searchCriteria.toString());
    }

    protected int[] dicomTagPathToArray(final int dicomTagPath) {
        return new int[]{dicomTagPath};
    }

    private void pingPacs() {
        cechoSCU.cecho();
    }

    private int getMaxResults() {
        return getOrmStrategy().getResultSetLimitStrategy().getMaxResultsForQueryLevel(getQueryLevel());
    }
}
