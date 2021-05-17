/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.rest.DicomQueryRetrieveApi
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.rest;

import static org.nrg.xdat.security.helpers.AccessLevel.*;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.nrg.framework.ajax.PaginatedRequest;
import org.nrg.framework.annotations.XapiRestController;
import org.nrg.prefs.exceptions.InvalidPreferenceName;
import org.nrg.xapi.exceptions.*;
import org.nrg.xapi.rest.AuthDelegate;
import org.nrg.xapi.rest.Project;
import org.nrg.xapi.rest.XapiRequestMapping;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.helpers.Groups;
import org.nrg.xdat.security.helpers.Permissions;
import org.nrg.xdat.security.helpers.Roles;
import org.nrg.xdat.security.services.RoleHolder;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xft.event.EventDetails;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.security.UserI;
import org.nrg.xnatx.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.xnatx.dqr.domain.Series;
import org.nrg.xnatx.dqr.domain.entities.*;
import org.nrg.xnatx.dqr.dto.*;
import org.nrg.xnatx.dqr.exceptions.PacsNotFoundException;
import org.nrg.xnatx.dqr.exceptions.PacsNotQueryableException;
import org.nrg.xnatx.dqr.exceptions.PacsNotStorableException;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;
import org.nrg.xnatx.dqr.security.DqrUserXapiAuthorization;
import org.nrg.xnatx.dqr.services.*;
import org.nrg.xnatx.dqr.utils.FindRow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import springfox.documentation.annotations.ApiIgnore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by mike on 1/19/18.
 */
@Api("Dicom Query Retrieve API")
@XapiRestController
@Slf4j
@RequestMapping("/dqr")
public class DicomQueryRetrieveApi extends AbstractDqrRestController {
    @Autowired
    public DicomQueryRetrieveApi(final DqrPreferences preferences, final UserManagementServiceI userManagementService, final RoleHolder roleHolder, final ExecutedPacsRequestService requestService, final QueuedPacsRequestService queuedRequestService, final PacsService pacsService, final PacsEntityService pacsEntityService, final DqrProjectSettingsService dqrProjectSettingsService, final Map<String, OrmStrategy> ormStrategies, final SiteConfigPreferences siteConfigPreferences, final NamedParameterJdbcTemplate template) {
        super(pacsEntityService, template, userManagementService, roleHolder);
        _preferences = preferences;
        _executedRequestService = requestService;
        _queuedRequestService = queuedRequestService;
        _pacsService = pacsService;
        _dqrProjectSettingsService = dqrProjectSettingsService;
        _ormStrategies = ormStrategies;
        _siteConfigPreferences = siteConfigPreferences;
    }

    @ApiOperation(value = "Returns the full map of DQR settings for this XNAT application.", notes = "Complex objects may be returned as encapsulated JSON strings.", response = String.class, responseContainer = "Map")
    @ApiResponses({@ApiResponse(code = 200, message = "DQR settings successfully retrieved."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Insufficient privileges to retrieve the requested setting."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "settings", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public Map<String, Object> getDqrPreferences() {
        log.info("User {} requested the system DQR settings.", getSessionUser().getUsername());
        return new HashMap<>(_preferences);
    }

    @ApiOperation(value = "Sets a map of DQR properties.", notes = "Sets the DQR properties specified in the map.")
    @ApiResponses({@ApiResponse(code = 200, message = "Automation properties successfully set."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to set automation properties."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "settings", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.APPLICATION_JSON_VALUE}, method = RequestMethod.POST, restrictTo = Admin)
    public void setDqrPreferences(@ApiParam(value = "The map of DQR preferences to be set.", required = true) @RequestBody final Map<String, String> properties) {
        log.info("User {} requested to set a batch of DQR preferences.", getSessionUser().getUsername());
        // Is this call initializing the system?
        for (final String name : properties.keySet()) {
            try {
                _preferences.set(properties.get(name), name);
                log.info("Set property {} to value: {}", name, properties.get(name));
            } catch (InvalidPreferenceName invalidPreferenceName) {
                log.error("Got an invalid preference name error for the preference: {}, failed to set value to: {}", name, properties.get(name));
            }
        }
    }

    @ApiOperation(value = "Get list of DQR configurations.", notes = "The Dqr configurations function returns a list of all DQR configurations in the XNAT system.", response = DqrProjectSettings.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "Returns a list of all of the currently configured DQR configurations."),
                   @ApiResponse(code = 500, message = "An unexpected or unknown error occurred")})
    @XapiRequestMapping(value = "settings/project", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    @ResponseBody
    public List<DqrProjectSettings> getAllDqrProjectSettings() {
        return _dqrProjectSettingsService.getAll();
    }

    @ApiOperation(value = "Creates the DQR configuration for a project from the submitted attributes.", notes = "Returns the newly created DQR configuration for a project with the submitted attributes.", response = DqrProjectSettings.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Returns the DQR configuration for the project."),
                   @ApiResponse(code = 403, message = "Insufficient privileges to create or update the DQR configuration for the project."),
                   @ApiResponse(code = 404, message = "The project for the new DQR configuration wasn't found."),
                   @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @XapiRequestMapping(value = "settings/project", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST, restrictTo = Admin)
    @ResponseBody
    public DqrProjectSettings createDqrProjectSettings(@RequestBody final DqrProjectSettings settings) throws NotFoundException, ResourceAlreadyExistsException {
        final DqrProjectSettings existingSettings = _dqrProjectSettingsService.getProjectSettings(settings.getProjectId());
        if (existingSettings == null) {
            return _dqrProjectSettingsService.create(settings);
        }
        throw new ResourceAlreadyExistsException("DQR project settings", settings.getProjectId());
    }

    @ApiOperation(value = "Get DQR configuration for the specified project.", notes = "This function returns the DQR settings for the specified project.", response = DqrProjectSettings.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Returns DQR configuration for the project."),
                   @ApiResponse(code = 500, message = "An unexpected or unknown error occurred")})
    @XapiRequestMapping(value = "settings/project/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    @ResponseBody
    public DqrProjectSettings getDqrProjectSettings(@PathVariable final String projectId) throws NotFoundException {
        return _dqrProjectSettingsService.getProjectSettings(projectId);
    }

    @ApiOperation(value = "Returns whether project is a project that has been configured to use DQR.", response = Boolean.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Whether the project is a DQR project."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to check whether the project is a DQR project."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "settings/project/{projectId}/enabled", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Read)
    public boolean isDqrProject(@PathVariable @Project final String projectId) throws NotFoundException {
        return _preferences.getAllowAllProjectsToUseDqr() || _dqrProjectSettingsService.isDqrEnabledForProject(projectId);
    }

    @ApiOperation(value = "Get DQR configuration for the specified project.", notes = "Returns the DQR configuration for the specified project.", response = DqrProjectSettings.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Returns DQR configuration for the project."),
                   @ApiResponse(code = 500, message = "An unexpected or unknown error occurred")})
    @XapiRequestMapping(value = "settings/project/{projectId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.PUT, restrictTo = Admin)
    @ResponseBody
    public DqrProjectSettings getDqrProjectSettings(@PathVariable final String projectId, @RequestBody final ProjectSettings settings) throws NotFoundException, DataFormatException, NotModifiedException {
        if (_dqrProjectSettingsService.isDqrConfigured(projectId)) {
            settings.setProjectId(projectId);
            return _dqrProjectSettingsService.update(settings);
        }
        return _dqrProjectSettingsService.create(DqrProjectSettings.builder().projectId(projectId).dqrEnabled(settings.getDqrEnabled()).build());
    }

    @ApiOperation(value = "Deletes the requested DQR configuration for the project.", response = Boolean.class)
    @ApiResponses({@ApiResponse(code = 200, message = "DQR configuration for the project was successfully removed."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Insufficient privileges to delete the DQR configuration for the project."),
                   @ApiResponse(code = 404, message = "The requested DQR configuration for the project wasn't found."),
                   @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @XapiRequestMapping(value = "settings/project/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.DELETE, restrictTo = Admin)
    @ResponseBody
    public boolean deleteDqrProjectSettings(@PathVariable final String projectId) throws NotFoundException {
        final DqrProjectSettings existingSettings = _dqrProjectSettingsService.getProjectSettings(projectId);
        if (existingSettings == null) {
            throw new NotFoundException("No DQR settings were found for the project " + projectId);
        }
        _dqrProjectSettingsService.delete(existingSettings.getId());
        return true;
    }

    @ApiOperation(value = "Returns a list of completed DICOM query requests.", notes = "The results can be transformed and filtered using the various query string parameters, which includes user (results should be limited to requests made by the current user), sort (sort order, which can be \"asc\" or \"desc\"), page, and pageSize.", response = Integer.class)
    @ApiResponses({@ApiResponse(code = 200, message = "The list of completed DICOM query requests."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the requested DICOM query requests."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "query/history", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public List<ExecutedPacsRequest> queryHistory(@ApiParam("Indicates that the completed requests should be filtered to include only requests made by the current user.") @RequestParam(defaultValue = "false") final boolean user,
                                                  @ApiParam("Indicates the sort order to use") @RequestParam(defaultValue = "desc") final String sort,
                                                  @ApiParam("Indicates the page to return") @RequestParam(defaultValue = "1") final int page,
                                                  @ApiParam("Indicates the number of results in a page") @RequestParam(defaultValue = "100") final int pageSize) {
        final PaginatedPacsRequest request = PaginatedPacsRequest.builder().sortDir(PaginatedRequest.SortDir.valueOf(sort)).pageNumber(page).pageSize(pageSize).build();
        return user ? _executedRequestService.getAllForUser(getSessionUser(), request) : _executedRequestService.getPaginated(request);
    }

    @ApiOperation(value = "Get count of all DICOM query requests.", notes = "The DICOM query history count function returns a count of all DICOM queries that have ever been made on the XNAT system.", response = Integer.class)
    @ApiResponses({@ApiResponse(code = 200, message = "A count of completed DICOM query requests."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the count of DICOM query requests."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "query/history/count", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public long queryHistoryCount(@ApiParam("Indicates that the completed requests should be filtered to include only requests made by the current user.") @RequestParam(defaultValue = "false") final boolean user) {
        return user ? _executedRequestService.getAllForUserCount(getSessionUser()) : _executedRequestService.getCount();
    }

    @ApiOperation(value = "Get DICOM query request by ID.", notes = "The DICOM query history function returns information about the DICOM query with a given ID.", response = ExecutedPacsRequest.class)
    @ApiResponses({@ApiResponse(code = 200, message = "A DICOM query request."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the DICOM query request."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "query/history/{id}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authorizer)
    public ExecutedPacsRequest queryHistoryGet(@ApiParam(value = "ID of the query request to fetch", required = true) @PathVariable final long id) throws NotFoundException {
        try {
            final UserI user = getSessionUser();
            return Roles.isSiteAdmin(user) ? _executedRequestService.get(id) : _executedRequestService.getByIdForUser(id, user);
        } catch (org.nrg.framework.exceptions.NotFoundException e) {
            throw new NotFoundException("No executed PACS request with ID " + id + " found");
        }
    }

    @ApiOperation(value = "Returns a list of queued DICOM query requests.", notes = "The results can be transformed and filtered using the various query string parameters, which includes user (results should be limited to requests made by the current user), sort (sort order, which can be \"asc\" or \"desc\"), page, and pageSize.", response = Integer.class)
    @ApiResponses({@ApiResponse(code = 200, message = "The list of queued DICOM query requests."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the requested DICOM query requests."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "query/queue", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public List<QueuedPacsRequest> queryQueue(@ApiParam("Indicates that the queued requests should be filtered to include only requests made by the current user.") @RequestParam(defaultValue = "false") final boolean user,
                                              @ApiParam("Indicates the sort order to use") @RequestParam(defaultValue = "desc") final String sort,
                                              @ApiParam("Indicates the page to return") @RequestParam(defaultValue = "1") final int page,
                                              @ApiParam("Indicates the number of results in a page") @RequestParam(defaultValue = "100") final int pageSize) {
        final PaginatedPacsRequest request = PaginatedPacsRequest.builder().sortDir(PaginatedRequest.SortDir.valueOf(sort)).pageNumber(page).pageSize(pageSize).build();
        return user ? _queuedRequestService.getAllForUser(getSessionUser(), request) : _queuedRequestService.getPaginated(request);
    }

    @ApiOperation(value = "Get count of all queued DICOM query requests.", notes = "The DICOM query queue count function returns a count of all DICOM queries that are currently queued on the XNAT system.", response = Integer.class)
    @ApiResponses({@ApiResponse(code = 200, message = "A count of queued DICOM query requests."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the count of DICOM query requests."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "query/queue/count", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public long queryQueueCount(@ApiParam("Indicates that the completed requests should be filtered to include only requests made by the current user.") @RequestParam(defaultValue = "false") final boolean user) {
        return user ? _queuedRequestService.getAllForUserCount(getSessionUser()) : _queuedRequestService.getCount();
    }

    @ApiOperation(value = "Deletes queued DICOM query requests with the given IDs.", notes = "Returns true if the queued DICOM query request was successfully deleted. Returns false otherwise.", response = Boolean.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Returns true to indicate the queued DICOM query requests were successfully deleted."),
                   @ApiResponse(code = 403, message = "The user doesn't have permission to delete queued DICOM query requests."),
                   @ApiResponse(code = 404, message = "The queued DICOM query requests weren't found."),
                   @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "query/queue", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST, restrictTo = Authorizer)
    public boolean queryQueueDelete(@RequestBody final List<Long> idsToDelete) throws InsufficientPrivilegesException, NotFoundException {
        final List<Long> idsNotFound = new ArrayList<>();
        for (final long idToDelete : idsToDelete) {
            try {
                queryQueueDelete(idToDelete);
            } catch (NotFoundException e) {
                idsNotFound.add(idToDelete);
            }
        }
        if (idsNotFound.isEmpty()) {
            return true;
        }
        throw new NotFoundException("Deleted " + (idsToDelete.size() - idsNotFound.size()) + " queued requests, but the remaining IDs were not found: " + idsNotFound.stream().map(Object::toString).collect(Collectors.joining(", ")));
    }

    @ApiOperation(value = "Get the specified queued DICOM query request by ID.", notes = "The DICOM query queue function returns information about the queued DICOM query with a given ID.", response = QueuedPacsRequest.class)
    @ApiResponses({@ApiResponse(code = 200, message = "A queued DICOM query request."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the DICOM query request."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "query/queue/{id}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authorizer)
    public QueuedPacsRequest queryQueueGet(@ApiParam(value = "ID of the queued query request to fetch", required = true) @PathVariable final long id) throws NotFoundException {
        try {
            final UserI user = getSessionUser();
            return Roles.isSiteAdmin(user) ? _queuedRequestService.get(id) : _queuedRequestService.getByIdForUser(id, user);
        } catch (org.nrg.framework.exceptions.NotFoundException e) {
            throw new NotFoundException("No queued PACS request with ID " + id + " found");
        }
    }

    @ApiOperation(value = "Deletes the queued DICOM query request with given ID.", notes = "Returns true if the queued DICOM query request was successfully deleted. Returns false otherwise.", response = Boolean.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Returns true to indicate the queued DICOM query request was successfully deleted."),
                   @ApiResponse(code = 403, message = "The user doesn't have permission to delete queued DICOM query requests."),
                   @ApiResponse(code = 404, message = "The queued DICOM query request wasn't found."),
                   @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "query/queue/{id}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.DELETE, restrictTo = Authorizer)
    public boolean queryQueueDelete(@ApiParam(value = "ID of the queued query request to delete", required = true) @PathVariable final long id) throws NotFoundException, InsufficientPrivilegesException {
        final UserI user = getSessionUser();
        try {
            final QueuedPacsRequest request = _queuedRequestService.get(id);
            if (!Roles.isSiteAdmin(user) && !StringUtils.equals(request.getUsername(), user.getUsername())) {
                throw new InsufficientPrivilegesException(user.getUsername(), Long.toString(id));
            }
            _queuedRequestService.delete(id);
            return true;
        } catch (org.nrg.framework.exceptions.NotFoundException e) {
            throw new NotFoundException("No queued PACS request with ID " + id + " found");
        }
    }

    @ApiOperation(value = "Get list of the series in a list of studies.", notes = "The get series function returns a list of the series in the listed studies.", response = String.class, responseContainer = "Map")
    @ApiResponses({@ApiResponse(code = 200, message = "A queued DICOM query request."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the series."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "query/series", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST, restrictTo = Authorizer)
    public Map<String, PacsSearchResults<Series>> getSeries(@ApiParam("A PACS series search request") @RequestBody final PacsSeriesSearchRequest request) throws NoContentException, PacsNotQueryableException {
        if (request.getStudyInstanceUids().isEmpty()) {
            throw new NoContentException("No study instance UIDs specified for query on PACS " + request.getPacsId());
        }

        final UserI user = getSessionUser();
        final Pacs  pacs = getPacsEntityService().retrieve(request.getPacsId());
        try {
            return _pacsService.getSeriesByStudyUid(user, pacs, request.getStudyInstanceUids());
        } catch (PacsNotQueryableException e) {
            log.error("An error occurred trying to retrieve series for user {} from PACS {} for study instance UIDs {}", user.getUsername(), request.getPacsId(), String.join(", ", request.getStudyInstanceUids()), e);
            throw e;
        }
        /*
        TODO: get queuing and polling working so that this search can be run asynchronously and pushed when completed.
        TODO: This method should return ResponseEntity<UUID> from the following code:
        try {
            final UUID uuid = _pacsService.getSeriesByStudyUids(user, pacs, studyInstanceUids);
            return ResponseEntity.status(HttpStatus.CREATED).location(new URI(_siteConfigPreferences.getSiteUrl() + "/xapi/dqr/seriesInfo/pacs/" + pacsId + "/studies/" + uuid)).body(uuid);
        } catch (PacsNotQueryableException e) {
            throw new NoContentException("The PACS " + pacs.getId() + " is not queryable");
        }
        */
    }

    @ApiIgnore("Requires queuing and polling working so that searches can be run asynchronously and pushed when completed.")
    @ApiOperation(value = "Get list of the series in a list of studies.", notes = "The get series function returns a list of the series in the listed studies.", response = String.class, responseContainer = "Map")
    @ApiResponses({@ApiResponse(code = 200, message = "A queued DICOM query request."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the series."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "query/series/{searchId}", produces = MediaType.APPLICATION_JSON_VALUE, restrictTo = Authorizer)
    public ResponseEntity<Map<String, PacsSearchResults<Series>>> getSeries(@ApiParam(value = "ID of the search request", required = true) @PathVariable final UUID searchId) throws NotFoundException, URISyntaxException {
        return _pacsService.getSearchStatus(searchId) ? ResponseEntity.ok(_pacsService.getSearchResults(searchId)) : ResponseEntity.status(HttpStatus.CREATED).location(new URI(_siteConfigPreferences.getSiteUrl() + "/xapi/dqr/query/series/" + searchId)).build();
    }

    /*
    @ApiOperation(value = "Searches for patients on the specified PACS.")
    @ApiResponses({@ApiResponse(code = 200, message = "Series successfully requested."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to import data from the specified PACS."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "query/patients", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE}, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST, restrictTo = Authorizer)
    @AuthDelegate(DqrUserXapiAuthorization.class)
    public PacsSearchResults<Patient> searchForPatients(final @ApiParam("Import request.") @RequestBody PacsSearchCriteria criteria) throws PacsNotFoundException, NoContentException, PacsNotQueryableException {
        final Pacs                       pacs     = getQueryablePacs(criteria.getPacsId());
        final PacsSearchResults<Patient> patients = _pacsService.getPatientsByExample(getSessionUser(), pacs, criteria);
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
    @XapiRequestMapping(value = "query/patients", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authorizer)
    @AuthDelegate(DqrUserXapiAuthorization.class)
    public Patient searchForPatient(final @ApiParam("ID of the PACS entry from which data should be imported.") @PathVariable long id, final @ApiParam("Import request.") @PathVariable String patientId) throws PacsNotFoundException, PacsNotQueryableException, NotFoundException {
        return _pacsService.getPatientById(getSessionUser(), getPacs(id), patientId).orElseThrow(() -> new NotFoundException("No patient was found with the ID " + patientId + " on the PACS " + id));
    }

    @ApiOperation(value = "Searches for studies on the specified PACS.")
    @ApiResponses({@ApiResponse(code = 200, message = "Series successfully requested."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to import data from the specified PACS."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "query/studies", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE}, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST, restrictTo = Authorizer)
    @AuthDelegate(DqrUserXapiAuthorization.class)
    public Collection<Study> searchForStudies(final @ApiParam("ID of the PACS entry from which data should be imported.") @PathVariable long id, final @ApiParam("Import request.") @RequestBody PacsSearchCriteria criteria) throws PacsNotFoundException, NoContentException, PacsNotQueryableException {
        final PacsSearchResults<Study> studies = _pacsService.getStudiesByExample(getSessionUser(), getQueryablePacs(id), criteria);
        if (studies.getResults().isEmpty()) {
            throw new NoContentException("No studies were found that met the specified criteria");
        }
        return studies.getResults();
    }

    @ApiOperation(value = "Searches for a particular study on the specified PACS.")
    @ApiResponses({@ApiResponse(code = 200, message = "Series successfully requested."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to import data from the specified PACS."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "query/studies", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authorizer)
    @AuthDelegate(DqrUserXapiAuthorization.class)
    public Study searchForStudy(final @ApiParam("ID of the PACS entry from which data should be imported.") @PathVariable long id, final @ApiParam("Import request.") @PathVariable String studyId) throws PacsNotFoundException, PacsNotQueryableException, NotFoundException {
        return _pacsService.getStudyById(getSessionUser(), getQueryablePacs(id), studyId).orElseThrow(() -> new NotFoundException("No study was found with the ID " + studyId + " on the PACS " + id));
    }

    @ApiOperation(value = "Searches for a particular study on the specified PACS.", response = Series.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "Series successfully requested."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to import data from the specified PACS."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "query/studies", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authorizer)
    @AuthDelegate(DqrUserXapiAuthorization.class)
    public Collection<Series> searchForStudySeries(final @ApiParam("ID of the PACS entry from which data should be imported.") @PathVariable long id, final @ApiParam("Import request.") @PathVariable String studyId) throws PacsNotFoundException, NoContentException, PacsNotQueryableException {
        final PacsSearchResults<Series> series = _pacsService.getSeriesByStudy(getSessionUser(), getQueryablePacs(id), Study.builder().studyId(studyId).build());
        if (series.getResults().isEmpty()) {
            throw new NoContentException("No series found for study with the ID " + studyId + " on the PACS " + id);
        }
        return series.getResults();
    }
    */

    @ApiOperation(value = "Uses the uploaded csv to generate JSON (with the format the new importer wants) containing information about what would be imported if the user decides to continue.", response = String.class)
    @ApiResponses({@ApiResponse(code = 200, message = "CSV successfully uploaded and processed."), @ApiResponse(code = 400, message = "Uploaded file must be a CSV."), @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."), @ApiResponse(code = 403, message = "Not authorized to upload a CSV."), @ApiResponse(code = 500, message = "Unexpected error")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "import", consumes = MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST, restrictTo = Authorizer)
    public ResponseEntity<List<FindRow>> importFromPacs(@ApiParam(value = "Multipart file object being uploaded") @RequestParam(value = "csv_to_store") MultipartFile csv,
                                                        @ApiParam("Pacs to query.") @RequestParam final Long pacsId,
                                                        @ApiParam("Get all studies on PACS when a row has no search criteria.") @RequestParam(defaultValue = "false") final boolean allowRowThatGetsAllStudiesOnPacs) throws Exception {
        final File temp = File.createTempFile("xnat", "csv");
        try (final InputStream input = csv.getInputStream(); final OutputStream output = new FileOutputStream(temp)) {
            IOUtils.copy(input, output);
        }
        final List<FindRow>              rows    = _pacsService.extractNewImportRequestFromCsv(getSessionUser(), temp, pacsId, allowRowThatGetsAllStudiesOnPacs);
        final ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (rows.stream().filter(Objects::nonNull).map(FindRow::getRelabelMap).filter(Objects::nonNull).allMatch(Map::isEmpty)) {
            builder.header(HttpHeaders.WARNING, "The generated JSON has no anon script.");
        }
        return builder.body(rows);
    }

    @ApiOperation(value = "Issues the PACS import requests specified in the JSON and performs the specified remapping on the data when it comes in.", response = Boolean.class)
    @ApiResponses({@ApiResponse(code = 200, message = "PACS requests successfully issued."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "import", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, restrictTo = Authorizer)
    public List<QueuedPacsRequest> importFromPacs(@RequestBody final PacsImportRequest request) throws Exception {
        final UserI user = getSessionUser();
        //You cannot import into a project that does not have DQR enabled.
        final String projectId = request.getProjectId();
        if (!_preferences.getAllowAllProjectsToUseDqr() && !_dqrProjectSettingsService.isDqrEnabledForProject(projectId) || !Permissions.canEditProject(user, projectId) && !Roles.checkRole(user, "Administrator") && !Groups.hasAllDataAccess(user)) {
            throw new InsufficientPrivilegesException(user.getUsername(), projectId);
        }
        return _pacsService.importFromPacs(getSessionUser(), request);
    }

    @ApiOperation(value = "Sends selected scans to PACS.", response = String.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Scans sent to PACS."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "export", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, restrictTo = Authorizer)
    public Map<String, Object> export(@ApiParam("Parameters for the export operation") @RequestParam final PacsExportRequest request) throws Exception {
        return export(request.getPacsId(), request.getSessionId(), request.getScans());
    }

    @ApiOperation(value = "Sends selected scans to PACS.", response = String.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Scans sent to PACS."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "export", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE, restrictTo = Authorizer)
    public Map<String, Object> export(@ApiParam("Id of PACS to send to.") @RequestParam final long pacsId,
                                      @ApiParam("XNAT session to send.") @RequestParam final String session,
                                      @ApiParam("Array of scans in the session to send.") @RequestParam final List<String> scansToExport) throws Exception {
        final Pacs _pacs = getPacsEntityService().retrieve(pacsId);
        if (_pacs == null) {
            throw new PacsNotFoundException(pacsId);
        }

        if (StringUtils.isBlank(session)) {
            throw new RuntimeException("You must specify a session ID for this operation.");
        }

        final UserI                user         = getSessionUser();
        final XnatImagesessiondata imageSession = XnatImagesessiondata.getXnatImagesessiondatasById(session, user, false);
        if (imageSession == null) {
            throw new RuntimeException("Couldn't find a session corresponding to the submitted session ID: " + session);
        }
        if (!Permissions.canRead(user, imageSession)) {
            throw new InsufficientPrivilegesException(user.getUsername(), imageSession.getId());
        }

        final Map<String, Object> dataToSend = new HashMap<>();
        dataToSend.put("session", session);
        try {
            if (scansToExport == null) {
                throw new RuntimeException("No scan IDs found to export, returning.");
            } else {
                ArrayList<String> scans = new ArrayList<>();
                if (_pacs.isStorable()) {
                    for (String scanId : scansToExport) {
                        final XnatImagescandata scan = imageSession.getScanById(scanId);
                        scans.add(scanId);
                        new Thread(() -> _pacsService.exportSeries(user, _pacs, scan)).start();
                        log.info("Exported series {} from session {}", scanId, imageSession.getId());
                    }
                    final EventDetails eventDetails;
                    eventDetails = EventUtils.newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.TYPE.PROCESS, "EXPORT_TO_PACS_REQUEST");
                    eventDetails.setComment("Pacs: " + pacsId);
                    PersistentWorkflowI wrk = PersistentWorkflowUtils.buildOpenWorkflow(user, XnatMrsessiondata.SCHEMA_ELEMENT_NAME, session, imageSession.getProject(), eventDetails);
                    assert wrk != null;
                    PersistentWorkflowUtils.complete(wrk, wrk.buildEvent());
                } else {
                    throw new PacsNotStorableException(pacsId);
                }
                dataToSend.put("scans", scans);

                log.debug("User {} exported {} scans from session {}", user.getLogin(), scansToExport.size(), imageSession.getId());
            }
        } catch (Exception exception) {
            throw new InitializationException(exception);
        }

        return dataToSend;
    }

    @ApiIgnore("This doesn't seem to be used any more.")
    @ApiOperation(value = "Get list of all ORM strategies.", notes = "Returns list of the names of all the ORM strategy bean IDs.", response = String.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "A list of ORM strategy bean IDs."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the list of ORM strategy bean IDs."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "strategies", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public Set<String> getOrmStrategies() {
        return _ormStrategies.keySet();
    }

    private final PacsService                _pacsService;
    private final ExecutedPacsRequestService _executedRequestService;
    private final QueuedPacsRequestService   _queuedRequestService;
    private final DqrProjectSettingsService  _dqrProjectSettingsService;
    private final DqrPreferences             _preferences;
    private final Map<String, OrmStrategy>   _ormStrategies;
    private final SiteConfigPreferences      _siteConfigPreferences;
}
