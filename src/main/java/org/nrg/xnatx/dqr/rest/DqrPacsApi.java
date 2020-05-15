/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.rest.DqrPacsApi
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.rest;

import static org.nrg.xdat.security.helpers.AccessLevel.Admin;
import static org.nrg.xdat.security.helpers.AccessLevel.Authenticated;
import static org.nrg.xdat.security.helpers.AccessLevel.Authorizer;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.nrg.framework.annotations.XapiRestController;
import org.nrg.xapi.exceptions.DataFormatException;
import org.nrg.xapi.exceptions.InsufficientPrivilegesException;
import org.nrg.xapi.exceptions.NoContentException;
import org.nrg.xapi.exceptions.NotFoundException;
import org.nrg.xapi.rest.AbstractXapiRestController;
import org.nrg.xapi.rest.AuthDelegate;
import org.nrg.xapi.rest.Experiment;
import org.nrg.xapi.rest.XapiRequestMapping;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.security.services.RoleHolder;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xnatx.dqr.dicom.command.cmove.CMoveFailureException;
import org.nrg.xnatx.dqr.dicom.command.cmove.CMoveTargetNotFoundException;
import org.nrg.xnatx.dqr.domain.Patient;
import org.nrg.xnatx.dqr.domain.Series;
import org.nrg.xnatx.dqr.domain.Study;
import org.nrg.xnatx.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.domain.entities.PacsRequest;
import org.nrg.xnatx.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.xnatx.dqr.dto.PacsImportRequest;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;
import org.nrg.xnatx.dqr.dto.PacsSearchResults;
import org.nrg.xnatx.dqr.exceptions.PacsNotFoundException;
import org.nrg.xnatx.dqr.exceptions.PacsNotQueryableException;
import org.nrg.xnatx.dqr.exceptions.PacsNotStorableException;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;
import org.nrg.xnatx.dqr.security.DqrUserXapiAuthorization;
import org.nrg.xnatx.dqr.services.DqrProjectSettingsService;
import org.nrg.xnatx.dqr.services.PacsEntityService;
import org.nrg.xnatx.dqr.services.PacsService;
import org.nrg.xnatx.dqr.services.QueuedPacsRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Api(value = "XNAT External PACS API", tags = {"pacs", "send", "retrieve", "query", "import", "export"})
@XapiRestController
@Slf4j
@RequestMapping(value = "/pacs")
public class DqrPacsApi extends AbstractXapiRestController {
    @Autowired
    public DqrPacsApi(final UserManagementServiceI userManagementService, final RoleHolder roleHolder, final DqrPreferences preferences, final PacsService pacsService, final PacsEntityService pacsEntityService, final QueuedPacsRequestService queuedPacsRequestService, final DqrProjectSettingsService projectSettings, final NamedParameterJdbcTemplate template) {
        super(userManagementService, roleHolder);
        _preferences = preferences;
        _pacsService = pacsService;
        _pacsEntityService = pacsEntityService;
        _queuedPacsRequestService = queuedPacsRequestService;
        _projectSettings = projectSettings;
        _template = template;
    }

    @ApiOperation(value = "Get list of all existing PACS systems.", notes = "This API call accepts two optional query-string parameters that can limit the scope of the PACS returned.", response = ExecutedPacsRequest.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "A list of PACS configured in this system."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the list of PACS."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authenticated)
    public List<Pacs> getAllPacs(final @ApiParam("Indicates whether the results should include only PACS that allow store (C-PUT) operations.") @RequestParam boolean storable,
                                 final @ApiParam("Indicates whether the results should include only PACS that allow query (C-FIND) operations.") @RequestParam boolean queryable) {
        return _pacsEntityService.findAll(storable, queryable);
    }

    @ApiOperation(value = "Creates a new PACS entry.", response = Pacs.class)
    @ApiResponses({@ApiResponse(code = 200, message = "New PACS entry successfully created."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to create a new PACS."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST, restrictTo = Admin)
    public Pacs createPacs(final @ApiParam("Attributes for the new PACS entry.") @RequestBody Pacs pacs) {
        return _pacsEntityService.create(pacs);
    }

    @ApiOperation(value = "Retrieves an existing PACS entry.", response = Pacs.class)
    @ApiResponses({@ApiResponse(code = 200, message = "PACS entry successfully retrieved."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to retrieve the PACS entry."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "{id}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authenticated)
    public Pacs retrievePacs(final @ApiParam("ID of the PACS entry to be retrieved.") @PathVariable long id) throws PacsNotFoundException {
        return getPacs(id);
    }

    @ApiOperation(value = "Updates an existing PACS entry.")
    @ApiResponses({@ApiResponse(code = 200, message = "PACS entry successfully updated."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to update the PACS entry."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.PUT, restrictTo = Admin)
    public void updatePacs(final @ApiParam("ID of the PACS entry to be updated.") @PathVariable long id, final @ApiParam("Attributes for the updated PACS entry.") @RequestBody Pacs pacs) throws DataFormatException, PacsNotFoundException {
        validate(id, pacs);
        _pacsEntityService.update(pacs);
    }

    @ApiOperation(value = "Deletes an existing PACS entry.")
    @ApiResponses({@ApiResponse(code = 200, message = "PACS entry successfully deleted."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to delete the PACS entry."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "{id}", method = RequestMethod.DELETE, restrictTo = Admin)
    public void deletePacs(final @ApiParam("ID of the PACS entry to be deleted.") @PathVariable long id) throws PacsNotFoundException {
        validate(id);
        _pacsEntityService.delete(id);
    }

    @ApiOperation(value = "Exports the DICOM files from a single series from an imaging session to the specified PACS.")
    @ApiResponses({@ApiResponse(code = 200, message = "Series successfully updated."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to update the PACS entry."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "{id}/experiments/{experimentId}/scans/{scanId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.PUT, restrictTo = Authorizer)
    @AuthDelegate(DqrUserXapiAuthorization.class)
    public void exportToPacs(final @ApiParam("ID of the PACS entry to which data should be exported.") @PathVariable long id, final @ApiParam("ID of the experiment to be exported.") @PathVariable @Experiment String experimentId, final @ApiParam("ID of the scan to be exported.") @PathVariable String scanId) throws NotFoundException, PacsNotStorableException, PacsNotFoundException {
        _pacsService.exportSeries(getSessionUser(), getStorablePacs(id), XnatImagescandata.getXnatImagescandatasByXnatImagescandataId(validate(experimentId, scanId), getSessionUser(), false));
    }

    @ApiOperation(value = "Imports the specified DICOM series from a single study from the specified PACS.")
    @ApiResponses({@ApiResponse(code = 200, message = "Series successfully requested."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to import data from the specified PACS."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST, restrictTo = Authorizer)
    @AuthDelegate(DqrUserXapiAuthorization.class)
    public ResponseEntity<QueuedPacsRequest> importFromPacs(final @ApiParam("ID of the PACS entry from which data should be imported.") @PathVariable long id, final @ApiParam("Import request.") @RequestBody PacsImportRequest request) throws DataFormatException, NotFoundException, InsufficientPrivilegesException {
        if (StringUtils.isBlank(request.getStudyInstanceUid())) {
            throw new DataFormatException("The study instance UID for the source DICOM study that contains the desired DICOM series must be specified.");
        }
        final String  projectId    = request.getProject();
        final boolean hasProjectId = StringUtils.isNotBlank(projectId);
        if (!_preferences.getAllowAllProjectsToUseDqr() && !_projectSettings.isDqrEnabledForProject(projectId)) {
            if (!hasProjectId) {
                throw new InsufficientPrivilegesException(getSessionUser().getUsername(), request.getStudyInstanceUid(), "DQR is not enabled for all projects on this system, so you must specify a specific project for the import operation.");
            } else {
                throw new InsufficientPrivilegesException(getSessionUser().getUsername(), projectId, "DQR is not enabled for the specified project.");
            }
        }
        try {
            final QueuedPacsRequest queuedPacsRequest = new QueuedPacsRequest();
            queuedPacsRequest.setPacsId(id);
            queuedPacsRequest.setUsername(getSessionUser().getUsername());
            if (hasProjectId) {
                queuedPacsRequest.setXnatProject(projectId);
            }
            queuedPacsRequest.setStudyInstanceUid(request.getStudyInstanceUid());
            queuedPacsRequest.setSeriesIds(request.getSeriesIds());
            if (StringUtils.isNotBlank(request.getAe())) {
                queuedPacsRequest.setDestinationAeTitle(StringUtils.substringBefore(request.getAe(), ":"));
            }
            queuedPacsRequest.setPriority(PacsRequest.HIGH_PRIORITY);
            queuedPacsRequest.setStatus(PacsRequest.QUEUED_STATUS_TEXT);
            queuedPacsRequest.setQueuedTime(new Date());

            final QueuedPacsRequest persisted = _queuedPacsRequestService.create(queuedPacsRequest);
            return ResponseEntity.ok().header(HttpHeaders.WARNING, "Your request is queued and will be serviced when the PACS is available.").body(persisted);
        } catch (Exception e) {
            final Throwable cause = e.getCause();
            final String    message;
            final Throwable error;
            if (cause instanceof CMoveFailureException) {
                message = "C-MOVE operation failed: " + cause.getMessage();
                error = cause;
            } else if (cause instanceof CMoveTargetNotFoundException) {
                message = "C-MOVE operation target not found: " + cause.getMessage();
                error = cause;
            } else if (cause != null) {
                message = "Unknown error: " + cause.getMessage();
                error = cause;
            } else {
                message = "Unknown error: " + e.getMessage();
                error = e;
            }
            log.error(message, error);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).header(HttpHeaders.WARNING, message).build();
        }
    }

    @ApiOperation(value = "Searches for patients on the specified PACS.")
    @ApiResponses({@ApiResponse(code = 200, message = "Series successfully requested."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to import data from the specified PACS."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "{id}/patients", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE}, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST, restrictTo = Authorizer)
    @AuthDelegate(DqrUserXapiAuthorization.class)
    public PacsSearchResults<String, Patient> searchForPatients(final @ApiParam("ID of the PACS entry from which data should be imported.") @PathVariable long id, final @ApiParam("Import request.") @RequestBody PacsSearchCriteria criteria) throws PacsNotFoundException, NoContentException, PacsNotQueryableException {
        final PacsSearchResults<String, Patient> patients;
        final Pacs                               pacs = getQueryablePacs(id);
        patients = _pacsService.getPatientsByExample(getSessionUser(), pacs, criteria);
        if (patients.getResults().isEmpty()) {
            throw new NoContentException("No patients were found that met the specified criteria");
        }
        return patients;
    }

    @ApiOperation(value = "Searches for a particular patient on the specified PACS.")
    @ApiResponses({@ApiResponse(code = 200, message = "Series successfully requested."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to import data from the specified PACS."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "{id}/patients/{patientId}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authorizer)
    @AuthDelegate(DqrUserXapiAuthorization.class)
    public Patient searchForPatient(final @ApiParam("ID of the PACS entry from which data should be imported.") @PathVariable long id, final @ApiParam("Import request.") @PathVariable String patientId) throws PacsNotFoundException, NoContentException, PacsNotQueryableException {
        final Patient patient = _pacsService.getPatientById(getSessionUser(), getPacs(id), patientId);
        if (patient == null) {
            throw new NoContentException("No patient was found with the ID " + patientId + " on the PACS " + id);
        }
        return patient;
    }

    @ApiOperation(value = "Searches for studies on the specified PACS.")
    @ApiResponses({@ApiResponse(code = 200, message = "Series successfully requested."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to import data from the specified PACS."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "{id}/studies", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE}, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST, restrictTo = Authorizer)
    @AuthDelegate(DqrUserXapiAuthorization.class)
    public PacsSearchResults<String, Study> searchForStudies(final @ApiParam("ID of the PACS entry from which data should be imported.") @PathVariable long id, final @ApiParam("Import request.") @RequestBody PacsSearchCriteria criteria) throws PacsNotFoundException, NoContentException, PacsNotQueryableException {
        final PacsSearchResults<String, Study> studies = _pacsService.getStudiesByExample(getSessionUser(), getQueryablePacs(id), criteria);
        if (studies.getResults().isEmpty()) {
            throw new NoContentException("No studies were found that met the specified criteria");
        }
        return studies;
    }

    @ApiOperation(value = "Searches for a particular study on the specified PACS.")
    @ApiResponses({@ApiResponse(code = 200, message = "Series successfully requested."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to import data from the specified PACS."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "{id}/studies/{studyId}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authorizer)
    @AuthDelegate(DqrUserXapiAuthorization.class)
    public Study searchForStudy(final @ApiParam("ID of the PACS entry from which data should be imported.") @PathVariable long id, final @ApiParam("Import request.") @PathVariable String studyId) throws PacsNotFoundException, NoContentException, PacsNotQueryableException {
        final Study study = _pacsService.getStudyById(getSessionUser(), getQueryablePacs(id), studyId);
        if (study == null) {
            throw new NoContentException("No study was found with the ID " + studyId + " on the PACS " + id);
        }
        return study;
    }

    @ApiOperation(value = "Searches for a particular study on the specified PACS.", response = Series.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "Series successfully requested."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to import data from the specified PACS."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "{id}/studies/{studyId}/series", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authorizer)
    @AuthDelegate(DqrUserXapiAuthorization.class)
    public Collection<Series> searchForStudySeries(final @ApiParam("ID of the PACS entry from which data should be imported.") @PathVariable long id, final @ApiParam("Import request.") @PathVariable String studyId) throws PacsNotFoundException, NoContentException, PacsNotQueryableException {
        final PacsSearchResults<String, Series> series = _pacsService.getSeriesByStudy(getSessionUser(), getQueryablePacs(id), Study.builder().studyId(studyId).build());
        if (series.getResults().isEmpty()) {
            throw new NoContentException("No series found for study with the ID " + studyId + " on the PACS " + id);
        }
        return series.getResults().values();
    }

    private Pacs getPacs(final long pacsId) throws PacsNotFoundException {
        try {
            return _pacsEntityService.get(pacsId);
        } catch (org.nrg.framework.exceptions.NotFoundException e) {
            throw new PacsNotFoundException(pacsId);
        }
    }

    private Pacs getQueryablePacs(final long pacsId) throws PacsNotFoundException, PacsNotQueryableException {
        final Pacs pacs = getPacs(pacsId);
        if (pacs.isQueryable()) {
            return pacs;
        }
        throw new PacsNotQueryableException(pacsId);
    }

    private Pacs getStorablePacs(final long pacsId) throws PacsNotFoundException, PacsNotStorableException {
        final Pacs pacs = getPacs(pacsId);
        if (pacs.isStorable()) {
            return pacs;
        }
        throw new PacsNotStorableException(pacsId);
    }

    private long validate(final String experimentId, final String scanId) throws NotFoundException {
        try {
            return _template.queryForObject(QUERY_IMAGE_SESSION_AND_SCAN, new MapSqlParameterSource("experimentId", experimentId).addValue("scanId", scanId), Long.class);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException(String.format(MESSAGE_SESSION_SCAN_NOT_FOUND, experimentId, scanId));
        }
    }

    @SuppressWarnings("unused")
    private void validate(final String experimentId) throws NotFoundException {
        if (!_template.queryForObject(QUERY_IMAGE_SESSION_EXISTS, new MapSqlParameterSource("experimentId", experimentId), Boolean.class)) {
            throw new NotFoundException(String.format(MESSAGE_SESSION_NOT_FOUND, experimentId));
        }
    }

    private void validate(final long id, final Pacs pacs) throws DataFormatException, PacsNotFoundException {
        if (pacs.getId() == 0) {
            pacs.setId(id);
        } else if (id != pacs.getId()) {
            throw new DataFormatException(String.format(MESSAGE_PACS_ID_MISMATCH, id, pacs.getId()));
        }
        validate(id);
    }

    private void validate(final long id) throws PacsNotFoundException {
        if (!_template.queryForObject(QUERY_PACS_EXISTS, new MapSqlParameterSource("id", id), Boolean.class)) {
            throw new PacsNotFoundException(id);
        }
    }

    private static final String QUERY_PACS_EXISTS              = "SELECT EXISTS(SELECT id FROM xhbm_pacs WHERE id = :id)";
    private static final String QUERY_IMAGE_SESSION_EXISTS     = "SELECT EXISTS(SELECT id FROM xnat_imagesessiondata WHERE id = :experimentId)";
    private static final String QUERY_IMAGE_SESSION_AND_SCAN   = "SELECT s.xnat_imagescandata_id FROM xnat_imagesessiondata i LEFT JOIN xnat_imagescandata s ON i.id = s.image_session_id WHERE i.id = :experimentId AND s.id = :scanId";
    private static final String MESSAGE_PACS_ID_MISMATCH       = "The ID for the update call \"%d\" does not match the PACS entity ID \"%d\"";
    private static final String MESSAGE_SESSION_NOT_FOUND      = "No image session with ID \"%s\" exists on this system";
    private static final String MESSAGE_SESSION_SCAN_NOT_FOUND = "No image session \"%s\" with scan ID \"%s\" exists on this system";

    private final DqrPreferences             _preferences;
    private final PacsService                _pacsService;
    private final PacsEntityService          _pacsEntityService;
    private final QueuedPacsRequestService   _queuedPacsRequestService;
    private final DqrProjectSettingsService  _projectSettings;
    private final NamedParameterJdbcTemplate _template;
}
