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
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.nrg.framework.annotations.XapiRestController;
import org.nrg.mail.services.MailService;
import org.nrg.prefs.exceptions.InvalidPreferenceName;
import org.nrg.xapi.exceptions.*;
import org.nrg.xapi.rest.AbstractXapiRestController;
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
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.event.EventDetails;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.security.UserI;
import org.nrg.xnatx.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.xnatx.dqr.domain.Series;
import org.nrg.xnatx.dqr.domain.entities.*;
import org.nrg.xnatx.dqr.dto.PacsSearchResults;
import org.nrg.xnatx.dqr.exceptions.PacsNotFoundException;
import org.nrg.xnatx.dqr.exceptions.PacsNotQueryableException;
import org.nrg.xnatx.dqr.exceptions.PacsNotStorableException;
import org.nrg.xnatx.dqr.messaging.PacsSearchRequest;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;
import org.nrg.xnatx.dqr.security.DqrUserXapiAuthorization;
import org.nrg.xnatx.dqr.services.*;
import org.nrg.xnatx.dqr.utils.CsvRow;
import org.nrg.xnatx.dqr.utils.FindRow;
import org.nrg.xnatx.dqr.utils.StudyImportInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.util.*;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;

/**
 * Created by mike on 1/19/18.
 */
@Api("Dicom Query Retrieve API")
@XapiRestController
@Slf4j
@RequestMapping(value = "/dqr")
public class DicomQueryRetrieveApi extends AbstractXapiRestController {

    @Autowired
    public DicomQueryRetrieveApi(final DqrPreferences preferences, final UserManagementServiceI userManagementService, final RoleHolder roleHolder, final ExecutedPacsRequestService requestService, final QueuedPacsRequestService queuedRequestService, final PacsService pacsService, final PacsEntityService pacsEntityService, final PacsPingService pacsPingService, final ProjectIrbInfoEntityService projectIrbInfoEntityService, final DqrProjectSettingsService dqrProjectSettingsService, final PacsAvailabilityService pacsAvailabilityService, final Map<String, OrmStrategy> ormStrategies, final SiteConfigPreferences siteConfigPreferences, final MailService mailService) {
        super(userManagementService, roleHolder);
        _preferences = preferences;
        _executedRequestService = requestService;
        _queuedRequestService = queuedRequestService;
        _pacsService = pacsService;
        _pacsEntityService = pacsEntityService;
        _pacsPingService = pacsPingService;
        _projectIrbInfoEntityService = projectIrbInfoEntityService;
        _dqrProjectSettingsService = dqrProjectSettingsService;
        _pacsAvailabilityService = pacsAvailabilityService;
        _ormStrategies = ormStrategies;
        _siteConfigPreferences = siteConfigPreferences;
        _mailService = mailService;
    }

    @ApiOperation(value = "Get list of all DICOM query requests.", notes = "The DICOM query history function returns a list of all DICOM queries that have ever been made on the XNAT system with brief information about each.", response = ExecutedPacsRequest.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "A list of DICOM query requests."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the list of DICOM query requests."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "query/history/all", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public List<ExecutedPacsRequest> queryHistoryGet() {
        return _executedRequestService.getAll();
    }

    @ApiOperation(value = "Get count of all DICOM query requests.", notes = "The DICOM query history count function returns a count of all DICOM queries that have ever been made on the XNAT system.", response = Integer.class)
    @ApiResponses({@ApiResponse(code = 200, message = "A count of DICOM query requests."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the count of DICOM query requests."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "query/history/all/count", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public long queryHistoryCountGet() {
        return _executedRequestService.getCount();
    }

    @ApiOperation(value = "Get DICOM query request history entries for the current user.", notes = "The DICOM query history function returns a list of all DICOM queries that have ever been made on the XNAT system for the current user with brief information about each.", response = ExecutedPacsRequest.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "A list of DICOM query requests."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the list of DICOM query requests."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "query/history/user", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authorizer)
    public List<ExecutedPacsRequest> userQueryHistoryGet(@ApiParam("Sort order") @RequestParam(name = "sort", defaultValue = "desc") final String sortOrder) {
        final List<ExecutedPacsRequest> allRequests = _executedRequestService.getAllForUser(getSessionUser());
        if (!sortOrder.equalsIgnoreCase("asc")) {
            Collections.reverse(allRequests);
        }
        return allRequests;
    }

    @ApiOperation(value = "Get count of DICOM query request history entries for the current user.", notes = "The DICOM query history count function returns a count of all DICOM queries that have ever been made on the XNAT system for the current user.", response = Integer.class)
    @ApiResponses({@ApiResponse(code = 200, message = "A count of DICOM query requests."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the count of DICOM query requests."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "query/history/user/count", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authorizer)
    public long userQueryHistoryCountGet() {
        return _executedRequestService.getAllForUserCount(getSessionUser());
    }

    @ApiOperation(
        value = "Get list of DICOM query request history entries for all users within a specified range.",
        notes = "The DICOM query history function returns a list of all DICOM queries that have ever been made on the XNAT system with brief information about each.",
        response = ExecutedPacsRequest.class, responseContainer = "List"
    )
    @ApiResponses({@ApiResponse(code = 200, message = "A list of DICOM query requests."),
                   @ApiResponse(code = 204, message = "No results. Invalid range."),
                   @ApiResponse(code = 400, message = "Request could not be completed as submitted."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the list of DICOM query requests."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "query/history/all/range", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public List<ExecutedPacsRequest> queryHistoryGetRange(@ApiParam("Sort order") @RequestParam(name = "sort", defaultValue = "desc") final String sortOrder,
                                                          @ApiParam("Range start") @RequestParam(name = "start", defaultValue = "1") final int rangeStart,
                                                          @ApiParam("Range end") @RequestParam(name = "end", defaultValue = "100") final int rangeEnd) {
        return getRangedList(_executedRequestService.getAll(), rangeStart, rangeEnd, sortOrder);
    }

    @ApiOperation(
        value = "Get paged list of DICOM query request history entries for all users.",
        notes = "The DICOM query history function returns a list of all DICOM queries that have ever been made on the XNAT system with brief information about each.",
        response = ExecutedPacsRequest.class, responseContainer = "List"
    )
    @ApiResponses({@ApiResponse(code = 200, message = "A list of DICOM query requests."),
                   @ApiResponse(code = 204, message = "No results. Invalid page range."),
                   @ApiResponse(code = 400, message = "Request could not be completed as submitted."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the list of DICOM query requests."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "query/history/all/paged", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public List<ExecutedPacsRequest> queryHistoryGetPaged(@ApiParam("Sort order") @RequestParam(name = "sort", defaultValue = "desc") final String sortOrder,
                                                          @ApiParam("Page index") @RequestParam(name = "page", defaultValue = "0") final int pageIndex,
                                                          @ApiParam("Page size") @RequestParam(name = "size", defaultValue = "100") final int pageSize) throws NoContentException {
        return getPagedList(_executedRequestService.getAll(), pageIndex, pageSize, sortOrder);
    }

    @ApiOperation(
        value = "Get list of DICOM query request history entries for the current user within a specified range.",
        notes = "The DICOM query history function returns a list of all DICOM queries that have ever been made on the XNAT system for the current user with brief information about each.",
        response = ExecutedPacsRequest.class, responseContainer = "List"
    )
    @ApiResponses({@ApiResponse(code = 200, message = "A list of DICOM query requests."),
                   @ApiResponse(code = 204, message = "No results. Invalid range."),
                   @ApiResponse(code = 400, message = "Request could not be completed as submitted."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the list of DICOM query requests."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "query/history/user/range", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authorizer)
    public List<ExecutedPacsRequest> userQueryHistoryGetRange(@ApiParam("Sort order") @RequestParam(name = "sort", defaultValue = "desc") final String sortOrder,
                                                              @ApiParam("Range start") @RequestParam(name = "start", defaultValue = "1") final int rangeStart,
                                                              @ApiParam("Range end") @RequestParam(name = "end", defaultValue = "100") final int rangeEnd) {
        return getRangedList(_executedRequestService.getAllForUser(getSessionUser()), rangeStart, rangeEnd, sortOrder);
    }

    @ApiOperation(value = "Get paged list of DICOM query request history for the current user.", notes = "The DICOM query history function returns a list of all DICOM queries that have ever been made on the XNAT system for the current user with brief information about each.", response = ExecutedPacsRequest.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "A list of DICOM query requests."),
                   @ApiResponse(code = 204, message = "No results. Invalid page range."),
                   @ApiResponse(code = 400, message = "Request could not be completed as submitted."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the list of DICOM query requests."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "query/history/user/paged", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authorizer)
    public List<ExecutedPacsRequest> userQueryHistoryGetPaged(@ApiParam("Sort order") @RequestParam(name = "sort", defaultValue = "desc") final String sortOrder,
                                                              @ApiParam("Page index") @RequestParam(name = "page", defaultValue = "0") final int pageIndex,
                                                              @ApiParam("Page size") @RequestParam(name = "size", defaultValue = "100") final int pageSize) throws NoContentException {
        return getPagedList(_executedRequestService.getAllForUser(getSessionUser()), pageIndex, pageSize, sortOrder);
    }

    @ApiOperation(value = "Get DICOM query request by ID.", notes = "The DICOM query history function returns information about the DICOM query with a given ID.", response = ExecutedPacsRequest.class)
    @ApiResponses({@ApiResponse(code = 200, message = "A DICOM query request."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the DICOM query request."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "query/history/request/{id}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authorizer)
    public ExecutedPacsRequest queryHistoryGet(@ApiParam(value = "ID of the query request to fetch", required = true) @PathVariable final long id) throws NotFoundException {
        try {
            final UserI user = getSessionUser();
            return Roles.isSiteAdmin(user) ? _executedRequestService.get(id) : _executedRequestService.getByIdForUser(id, user);
        } catch (org.nrg.framework.exceptions.NotFoundException e) {
            throw new NotFoundException("No executed PACS request with ID " + id + " found");
        }
    }

    @ApiOperation(value = "Get list of queued DICOM query requests for all users.", notes = "The DICOM query queue function returns a list of all DICOM queries that are currently queued on the XNAT system with brief information about each.", response = QueuedPacsRequest.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "A list of queued DICOM query requests."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the list of queued DICOM query requests."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "query/queue/all", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public List<QueuedPacsRequest> queryQueueGet() {
        return _queuedRequestService.getAll();
    }

    @ApiOperation(value = "Get count of queued DICOM query requests for all users.", notes = "The DICOM query queue count function returns a count of all DICOM queries that are currently queued on the XNAT system.", response = Integer.class)
    @ApiResponses({@ApiResponse(code = 200, message = "A count of queued DICOM query requests."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the count of queued DICOM query requests."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "query/queue/all/count", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public long queryQueueCountGet() {
        return _queuedRequestService.getCount();
    }

    @ApiOperation(value = "Get list of queued DICOM query requests for the current user.", notes = "The DICOM query queue function returns a list of all DICOM queries that are currently queued on the XNAT system for the current user with brief information about each.", response = QueuedPacsRequest.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "A list of queued DICOM query requests."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the list of queued DICOM query requests."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "query/queue/user", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authorizer)
    public List<QueuedPacsRequest> queryUserQueueGet() {
        return _queuedRequestService.getAllForUser(getSessionUser());
    }

    @ApiOperation(value = "Get count of queued DICOM query requests for the current user.", notes = "The DICOM query queue count function returns a count of all DICOM queries that are currently queued on the XNAT system for the current user.", response = Integer.class)
    @ApiResponses({@ApiResponse(code = 200, message = "A count of queued DICOM query requests."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the count of queued DICOM query requests."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "query/queue/user/count", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authorizer)
    public long queryUserQueueCountGet() {
        return _queuedRequestService.getAllForUserCount(getSessionUser());
    }

    @ApiOperation(value = "Get list of queued DICOM query requests with order information for all users.", notes = "The DICOM query queue function returns a list of all DICOM queries that are currently queued on the XNAT system with brief information about each (including order information).", response = QueuedPacsRequest.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "A list of queued DICOM query requests."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the list of queued DICOM query requests."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "query/queue/all/ordered", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public List<Map<String, Object>> queryQueueWithOrderGet(@ApiParam("Sort order") @RequestParam(name = "sort", defaultValue = "asc") final String sortOrder) {
        final List<Map<String, Object>> allRequests = _queuedRequestService.getAllWithOrder();
        if (sortOrder.equalsIgnoreCase("desc")) {
            Collections.reverse(allRequests);
        }
        return allRequests;
    }

    @ApiOperation(value = "Get list of queued DICOM query requests for the current user with order information.", notes = "The DICOM query queue function returns a list of all DICOM queries that are currently queued on the XNAT system for the current user with brief information about each (including order information).", response = QueuedPacsRequest.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "A list of queued DICOM query requests."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the list of queued DICOM query requests."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "query/queue/user/ordered", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authorizer)
    public List<Map<String, Object>> queryUserQueueWithOrderGet(@ApiParam("Sort order") @RequestParam(name = "sort", defaultValue = "asc") final String sortOrder) {
        final List<Map<String, Object>> allRequests = _queuedRequestService.getAllWithOrderForUser(getSessionUser());
        if (sortOrder.equalsIgnoreCase("desc")) {
            Collections.reverse(allRequests);
        }
        return allRequests;
    }

    @ApiOperation(value = "Get paged list of queued DICOM query requests for all users.", notes = "The DICOM query queue function returns a list of all DICOM queries that are currently queued on the XNAT system with brief information about each.", response = QueuedPacsRequest.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "A list of queued DICOM query requests."),
                   @ApiResponse(code = 204, message = "No results. Invalid page range."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the list of queued DICOM query requests."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "query/queue/all/paged", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public List<QueuedPacsRequest> queryQueuePagedGet(@ApiParam("Sort order") @RequestParam(name = "sort", defaultValue = "desc") final String sortOrder,
                                                      @ApiParam("Page index") @RequestParam(name = "page", defaultValue = "0") final int pageIndex,
                                                      @ApiParam("Page size") @RequestParam(name = "size", defaultValue = "100") final int pageSize) throws NoContentException {
        return getPagedList(_queuedRequestService.getAll(), pageIndex, pageSize, sortOrder);
    }

    @ApiOperation(value = "Get paged list of queued DICOM query requests for the current user.", notes = "The DICOM query queue function returns a list of all DICOM queries that are currently queued on the XNAT system for the current user with brief information about each.", response = QueuedPacsRequest.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "A list of queued DICOM query requests."),
                   @ApiResponse(code = 204, message = "No results. Invalid page range."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the list of queued DICOM query requests."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "query/queue/user/paged", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authorizer)
    public List<QueuedPacsRequest> queryUserQueuePagedGet(@ApiParam("Sort order") @RequestParam(name = "sort", defaultValue = "desc") final String sortOrder,
                                                          @ApiParam("Page index") @RequestParam(name = "page", defaultValue = "0") final int pageIndex,
                                                          @ApiParam("Page size") @RequestParam(name = "size", defaultValue = "100") final int pageSize) throws NoContentException {
        return getPagedList(_queuedRequestService.getAllForUser(getSessionUser()), pageIndex, pageSize, sortOrder);
    }

    @ApiOperation(value = "Get paged list of queued DICOM query requests with order information for all users.", notes = "The DICOM query queue function returns a list of all DICOM queries that are currently queued on the XNAT system with brief information about each (including order information).", response = QueuedPacsRequest.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "A list of queued DICOM query requests."),
                   @ApiResponse(code = 204, message = "No results. Invalid page range."),
                   @ApiResponse(code = 400, message = "Request could not be completed as submitted."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the list of queued DICOM query requests."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "query/queue/all/ordered/paged", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public List<Map<String, Object>> queryQueueGetPagedWithOrder(@ApiParam("Sort order") @RequestParam(name = "sort", defaultValue = "asc") final String sortOrder,
                                                                 @ApiParam("Page index") @RequestParam(name = "page", defaultValue = "0") final int pageIndex,
                                                                 @ApiParam("Page size") @RequestParam(name = "size", defaultValue = "100") final int pageSize) throws NoContentException {
        return getPagedList(_queuedRequestService.getAllWithOrder(), pageIndex, pageSize, sortOrder);
    }

    @ApiOperation(value = "Get paged list of queued DICOM query requests with order information for the current user.", notes = "The DICOM query queue function returns a list of all DICOM queries that are currently queued on the XNAT system for the current user with brief information about each (including order information).", response = QueuedPacsRequest.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "A list of queued DICOM query requests."),
                   @ApiResponse(code = 204, message = "No results. Invalid page range."),
                   @ApiResponse(code = 400, message = "Request could not be completed as submitted."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the list of queued DICOM query requests."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "query/queue/user/ordered/paged", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authorizer)
    public List<Map<String, Object>> queryUserQueueGetPagedWithOrder(@ApiParam("Sort order") @RequestParam(name = "sort", defaultValue = "asc") final String sortOrder,
                                                                     @ApiParam("Page index") @RequestParam(name = "page", defaultValue = "0") final int pageIndex,
                                                                     @ApiParam("Page size") @RequestParam(name = "size", defaultValue = "100") final int pageSize) throws NoContentException {
        return getPagedList(_queuedRequestService.getAllWithOrderForUser(getSessionUser()), pageIndex, pageSize, sortOrder);
    }

    @ApiOperation(value = "Get data for queued DICOM query request by ID.", notes = "The DICOM query queue function returns information about the queued DICOM query with a given ID.", response = QueuedPacsRequest.class)
    @ApiResponses({@ApiResponse(code = 200, message = "A queued DICOM query request."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the DICOM query request."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "query/queue/request/{id}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authorizer)
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
    @XapiRequestMapping(value = "query/queue/request/{id}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.DELETE, restrictTo = Authorizer)
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

    @ApiOperation(value = "Deletes the queued DICOM query requests with given IDs.", notes = "Returns true if the queued DICOM query request was successfully deleted. Returns false otherwise.", response = Boolean.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Returns true to indicate the queued DICOM query requests were successfully deleted."),
                   @ApiResponse(code = 403, message = "The user doesn't have permission to delete queued DICOM query requests."),
                   @ApiResponse(code = 404, message = "The queued DICOM query requests weren't found."),
                   @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "query/queue/deleteRequests", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST, restrictTo = Authorizer)
    public boolean queryQueueDeleteMultiple(@RequestBody final List<Long> idsToDelete) throws NotFoundException, InsufficientPrivilegesException {
        for (final long idToDelete : idsToDelete) {
            queryQueueDelete(idToDelete);
        }
        return true;
    }

    /**
     * @deprecated Use the csvimport/newUploadCsv REST call instead.
     */
    @Deprecated
    @ApiOperation(value = "Uses the uploaded csv to generate JSON containing information about what would be imported if the user decides to continue.", response = String.class)
    @ApiResponses({@ApiResponse(code = 200, message = "CSV successfully uploaded and processed."), @ApiResponse(code = 400, message = "Uploaded file must be a CSV."), @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."), @ApiResponse(code = 403, message = "Not authorized to upload a CSV."), @ApiResponse(code = 500, message = "Unexpected error")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "csvimport/uploadCsv", consumes = MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST, restrictTo = Authorizer)
    public ResponseEntity<List<CsvRow>> uploadImportCsv(@ApiParam(value = "Multipart file object being uploaded") @RequestParam(value = "csv_to_store") MultipartFile csv,
                                                        @ApiParam("Pacs to query.") @RequestParam(name = "pacsId") final Long pacsId, @ApiParam("Get all studies on PACS when a row has no search criteria.") @RequestParam(name = "allowRowThatGetsAllStudiesOnPacs", required = false) final boolean allowRowThatGetsAllStudiesOnPacs) throws Exception {
        final File temp = File.createTempFile("xnat", "csv");
        try (final FileOutputStream fos = new FileOutputStream(temp)) {
            fos.write(csv.getBytes());
        }
        final List<CsvRow>               rows    = _pacsService.extractImportRequestFromCsv(getSessionUser(), temp, pacsId, allowRowThatGetsAllStudiesOnPacs);
        final ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (rows.stream().filter(Objects::nonNull).noneMatch(row -> StringUtils.isNotBlank(row.getAnonScript()))) {
            builder.header(HttpHeaders.WARNING, "The generated JSON has no anon script.");
        }
        return builder.body(rows);
    }

    @ApiOperation(value = "Uses the uploaded csv to generate JSON (with the format the new importer wants) containing information about what would be imported if the user decides to continue.", response = String.class)
    @ApiResponses({@ApiResponse(code = 200, message = "CSV successfully uploaded and processed."), @ApiResponse(code = 400, message = "Uploaded file must be a CSV."), @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."), @ApiResponse(code = 403, message = "Not authorized to upload a CSV."), @ApiResponse(code = 500, message = "Unexpected error")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "csvimport/newUploadCsv", consumes = MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST, restrictTo = Authorizer)
    public ResponseEntity<List<FindRow>> newUploadImportCsv(@ApiParam(value = "Multipart file object being uploaded") @RequestParam(value = "csv_to_store") MultipartFile csv,
                                                            @ApiParam("Pacs to query.") @RequestParam(name = "pacsId") final Long pacsId, @ApiParam("Get all studies on PACS when a row has no search criteria.") @RequestParam(name = "allowRowThatGetsAllStudiesOnPacs", required = false) final boolean allowRowThatGetsAllStudiesOnPacs) throws Exception {
        final File temp = File.createTempFile("xnat", "csv");
        try (final FileOutputStream fos = new FileOutputStream(temp)) {
            fos.write(csv.getBytes());
        }
        final List<FindRow>              rows    = _pacsService.extractNewImportRequestFromCsv(getSessionUser(), temp, pacsId, allowRowThatGetsAllStudiesOnPacs);
        final ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (rows.stream().filter(Objects::nonNull).map(FindRow::getRelabelMap).filter(Objects::nonNull).allMatch(Map::isEmpty)) {
            builder.header(HttpHeaders.WARNING, "The generated JSON has no anon script.");
        }
        return builder.body(rows);
    }

    /**
     * @deprecated Use the csvimport/generalImportFromJson REST call instead.
     */
    @Deprecated
    @ApiOperation(value = "Issues the PACS import requests specified in the JSON and performs the specified remapping on the data when it comes in.", response = Boolean.class)
    @ApiResponses({@ApiResponse(code = 200, message = "PACS requests successfully issued."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "csvimport/importFromJson", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, restrictTo = Authorizer)
    public ResponseEntity<Boolean> importFromPacs(@RequestBody final List<CsvRow> rows,
                                                  @ApiParam("Pacs to query.") @RequestParam(name = "pacsId") final Long pacsId,
                                                  @ApiParam("XNAT SCP receiver to send to (Must be formatted as AE_TITLE:PORT).") @RequestParam(name = "ae") final String ae,
                                                  @ApiParam("XNAT project to send to.") @RequestParam(name = "project") final String project,
                                                  @ApiParam("Force the import to happen even if requested remapping won't take place.") @RequestParam(name = "importEvenIfCustomProcessingIsOff", required = false) final boolean importEvenIfCustomProcessingIsOff) throws Exception {
        final UserI user = getSessionUser();
        if (!_preferences.getAllowAllProjectsToUseDqr() && !_dqrProjectSettingsService.isDqrEnabledForProject(project) || !Permissions.canEditProject(user, project) && !Roles.checkRole(user, "Administrator") && !Groups.hasAllDataAccess(user)) {
            //You cannot import into a project that does not have DQR enabled.
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(false);
        }
        final boolean importFromRows = _pacsService.processSpreadsheetImportFromRows(user, rows, ae, project, pacsId, importEvenIfCustomProcessingIsOff);
        return ResponseEntity.ok().header(HttpHeaders.WARNING, importFromRows ? QUERY_SUBMITTED : PACS_NOT_AVAILABLE).body(true);
    }

    @ApiOperation(value = "Issues the PACS import requests specified in the JSON and performs the specified remapping on the data when it comes in.", response = Boolean.class)
    @ApiResponses({@ApiResponse(code = 200, message = "PACS requests successfully issued."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "csvimport/generalImportFromJson", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, restrictTo = Authorizer)
    public ResponseEntity<Boolean> importFromPacsGeneral(@RequestBody final Map<String, StudyImportInformation> studiesToImport,
                                                         @ApiParam("Pacs to query.") @RequestParam(name = "pacsId") final long pacsId,
                                                         @ApiParam("XNAT SCP receiver to send to (Must be formatted as AE_TITLE:PORT).") @RequestParam(name = "ae") final String ae,
                                                         @ApiParam("XNAT project to send to.") @RequestParam(name = "project") final String project,
                                                         @ApiParam("Force the import to happen even if requested remapping won't take place.") @RequestParam(name = "importEvenIfCustomProcessingIsOff", required = false) final boolean importEvenIfCustomProcessingIsOff) throws Exception {
        final UserI user = getSessionUser();
        //You cannot import into a project that does not have DQR enabled.
        if (!_preferences.getAllowAllProjectsToUseDqr() && !_dqrProjectSettingsService.isDqrEnabledForProject(project) || !Permissions.canEditProject(user, project) && !Roles.checkRole(user, "Administrator") && !Groups.hasAllDataAccess(user)) {
            throw new InsufficientPrivilegesException(user.getUsername(), project);
        }
        final boolean processSpreadsheetImport = _pacsService.processSpreadsheetImport(studiesToImport, getSessionUser(), ae, project, pacsId, importEvenIfCustomProcessingIsOff);
        return ResponseEntity.ok().header(HttpHeaders.WARNING, processSpreadsheetImport ? QUERY_SUBMITTED : PACS_NOT_AVAILABLE).body(true);
    }

    @ApiOperation(value = "Sends selected scans to PACS.", response = String.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Scans sent to PACS."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "send/toPacs", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, restrictTo = Authorizer)
    public Map<String, Object> sendToPacs(@ApiParam("Id of PACS to send to.") @RequestParam(name = "pacsId") final long pacsId,
                                          @ApiParam("XNAT session to send.") @RequestParam(name = "session") final String session,
                                          @ApiParam("Array of scans in the session to send.") @RequestParam(name = "scansToExport") final String[] scansToExport) throws Exception {
        final Pacs _pacs = _pacsEntityService.retrieve(pacsId);
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

                log.debug("User {} exported {} scans from session {}", user.getLogin(), scansToExport.length, imageSession.getId());
            }
        } catch (Exception exception) {
            throw new InitializationException(exception);
        }

        return dataToSend;
    }

    @ApiOperation(value = "Ping a PACS.", notes = "The ping PACS function returns whether the PACS was responsive.", response = PacsPing.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Whether the PACS was responsive."),
                   @ApiResponse(code = 401, message = "Must be authenticated to ping PACS."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to ping PACS."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "pacsStatus/ping/{id}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authorizer)
    @AuthDelegate(DqrUserXapiAuthorization.class)
    public PacsPing pingPacs(@ApiParam(value = "ID of the pacs to ping", required = true) @PathVariable final long id) {
        final PacsPing ping = new PacsPing();
        ping.setPacsId(id);
        ping.setSuccessful(_pacsService.canConnect(getSessionUser(), _pacsEntityService.retrieve(id)));
        ping.setPingTime(new Date());
        _pacsPingService.create(ping);
        return ping;
    }

    @ApiOperation(value = "Get information about the last time the PACS was pinged.", notes = "The last ping for PACS function returns information about the last time the PACS with supplied ID was pinged.", response = PacsPing.class)
    @ApiResponses({@ApiResponse(code = 200, message = "A PACS ping."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the PACS ping."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "pacsStatus/lastPing/{id}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public PacsPing lastPingForPacs(@ApiParam(value = "ID of the PACS", required = true) @PathVariable final long id) {
        return _pacsPingService.getLatestPing(id);
    }

    @ApiOperation(value = "Get information about the times the PACS was pinged.", notes = "The all pings for PACS function returns information about all the times the PACS with supplied ID was pinged.", response = PacsPing.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "A list of PACS pings."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the PACS pings."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "pacsStatus/allPings/{id}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public List<PacsPing> allPingsForPacs(@ApiParam(value = "ID of the PACS", required = true) @PathVariable final long id) {
        return _pacsPingService.getPings(id);
    }

    @ApiOperation(value = "Get list of all ormStrategies.", notes = "Returns list of the names of all the OrmStrategies beans.", response = String.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "A list of ormStrategies."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access ormStrategies."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "ormStrategies", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public Set<String> getOrmStrategies() {
        return _ormStrategies.keySet();
    }

    @ApiOperation(value = "Returns the full map of DQR settings for this XNAT application.", notes = "Complex objects may be returned as encapsulated JSON strings.", response = String.class, responseContainer = "Map")
    @ApiResponses({@ApiResponse(code = 200, message = "DQR settings successfully retrieved."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Insufficient privileges to retrieve the requested setting."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public Map<String, Object> getAllDqrPreferences() {
        log.info("User {} requested the system DQR settings.", getSessionUser().getUsername());
        return new HashMap<>(_preferences);
    }

    @ApiOperation(value = "Sets a map of DQR properties.", notes = "Sets the DQR properties specified in the map.")
    @ApiResponses({@ApiResponse(code = 200, message = "Automation properties successfully set."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to set automation properties."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.APPLICATION_JSON_VALUE}, method = RequestMethod.POST, restrictTo = Admin)
    public void setBatchDqrPreferences(@ApiParam(value = "The map of DQR preferences to be set.", required = true) @RequestBody final Map<String, String> properties) {
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

    @ApiOperation(value = "Returns whether project is a project that has been configured to use Dqr.", response = Boolean.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Whether the project is a Dqr project."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to check whether the project is a Dqr project."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "isDqrProject/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Read)
    public boolean isDqrProject(@PathVariable("projectId") @Project final String projectId) throws NotFoundException {
        return _preferences.getAllowAllProjectsToUseDqr() || _dqrProjectSettingsService.isDqrEnabledForProject(projectId);
    }

    @ApiOperation(value = "Get stored IRB number for project.", response = String.class)
    @ApiResponses({@ApiResponse(code = 200, message = "An IRB number."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the project's IRB number."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "projectSettings/{projectId}/irbNumber", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Delete)
    public String getIrbNumber(@PathVariable("projectId") @Project final String projectId) throws NotFoundException {
        return _projectIrbInfoEntityService.findIrbNumberForProject(projectId);
    }

    @ApiOperation(value = "Get stored IRB file for project.", response = Object.class)
    @ApiResponses({@ApiResponse(code = 200, message = "An IRB file."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the project's IRB file."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "projectSettings/{projectId}/irbFile/{fileName}", method = RequestMethod.GET, restrictTo = Delete)
    @ResponseBody
    public ResponseEntity<ByteArrayResource> getIrbFile(@PathVariable @Project final String projectId, final @PathVariable String fileName) throws IOException, NotFoundException {
        //Filename is included in the URL to avoid confusing some browsers (even though it's unused).
        final ProjectIrbInfo info = _projectIrbInfoEntityService.findIrbInfoForProject(projectId);
        if (info == null) {
            throw new NotFoundException("No IRB file found for project ID " + projectId);
        }

        final File file = Paths.get(info.getProjectIrbFiles().stream().filter(fileStoreInfo -> fileStoreInfo.getLabel().equals(fileName)).findAny().orElseThrow(() -> new NotFoundException(projectId + ": " + fileName)).getStoreUri()).toFile();

        final String mimeType = StringUtils.endsWith(fileName, ".pdf") ? MediaType.APPLICATION_PDF_VALUE : URLConnection.guessContentTypeFromName(fileName);

        return ResponseEntity.ok()
                             .header(HttpHeaders.CONTENT_TYPE, mimeType)
                             .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                             .body(new ByteArrayResource(FileUtils.readFileToByteArray(file)));
    }

    @ApiOperation(value = "Get stored IRB filename for project.", response = String.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "An IRB filename."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the project's IRB filename."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "projectSettings/{projectId}/irbFilename", produces = MediaType.TEXT_PLAIN_VALUE, method = RequestMethod.GET, restrictTo = Delete)
    @ResponseBody
    public List<String> getIrbFilenames(@PathVariable("projectId") @Project final String projectId) throws NotFoundException {
        return _projectIrbInfoEntityService.findIrbFileNamesForProject(projectId);
    }

    @ApiOperation(value = "Update IRB number for project.", response = Boolean.class)
    @ApiResponses({@ApiResponse(code = 200, message = "IRB number updated."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to modify the project's IRB number."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "projectSettings/{projectId}/irbNumber", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.PUT, restrictTo = Delete)
    public boolean putIrbNumber(@PathVariable("projectId") @Project final String projectId,
                                @ApiParam("IRB number for this project.") @RequestParam(name = "irbNumber") final String irbNumber) throws NotFoundException {
        final ProjectIrbInfo info = _projectIrbInfoEntityService.findIrbInfoForProject(projectId);
        if (info != null) {
            info.setIrbNumber(irbNumber);
            _projectIrbInfoEntityService.update(info);
            if (!info.getProjectIrbFiles().isEmpty()) {
                notifyAdminOfCompleteIrbInfo(projectId, info, getSessionUser());
            }
        } else {
            //Create new IRB info object
            final ProjectIrbInfo projectIrbInfo = new ProjectIrbInfo();
            projectIrbInfo.setProjectId(projectId);
            projectIrbInfo.setIrbNumber(irbNumber);
            _projectIrbInfoEntityService.create(projectIrbInfo);
        }
        return true;
    }

    @ApiOperation(value = "Update IRB file for project.", response = Boolean.class)
    @ApiResponses({@ApiResponse(code = 200, message = "IRB file updated."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to modify the project's IRB file."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "projectSettings/{projectId}/irbFile", method = RequestMethod.PUT, restrictTo = Edit)
    public boolean putIrbFile(@ApiParam(value = "Multipart file object being uploaded") @RequestParam final MultipartFile irbFile,
                              @ApiParam(value = "IRB number; required when creating new IRB") @RequestParam(required = false) final String irbNumber,
                              @PathVariable @Project final String projectId) throws InitializationException, ResourceAlreadyExistsException {
        try {
            final String         fileName = irbFile.getOriginalFilename();
            final byte[]         bytes    = irbFile.getBytes();
            final ProjectIrbInfo info     = getProjectIrbInfo(projectId);
            if (info != null) {
                _projectIrbInfoEntityService.addIrbFile(info, fileName, bytes);
                notifyAdminOfCompleteIrbInfo(projectId, info, getSessionUser());
            } else {
                //Create new IRB info object
                _projectIrbInfoEntityService.createNewIrbInfo(projectId, irbNumber, fileName, bytes);
            }
        } catch (IOException e) {
            log.error("IO exception when updating IRB file.", e);
            throw new InitializationException(e);
        }
        return true;
    }

    private ProjectIrbInfo getProjectIrbInfo(final String projectId) {
        try {
            return _projectIrbInfoEntityService.findIrbInfoForProject(projectId);
        } catch (NotFoundException e) {
            return null;
        }
    }

    @ApiOperation(value = "Deletes the stored IRB file for project.", response = Boolean.class)
    @ApiResponses({@ApiResponse(code = 200, message = "IRB file for the project was successfully removed."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Insufficient privileges to delete the IRB file for the project."),
                   @ApiResponse(code = 404, message = "The requested IRB file for the project wasn't found."),
                   @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @XapiRequestMapping(value = "projectSettings/{projectId}/irbFile", method = RequestMethod.DELETE, restrictTo = Delete)
    @ResponseBody
    public boolean deleteIrbFile(@PathVariable("projectId") @Project final String projectId) throws NotFoundException {
        final ProjectIrbInfo info = _projectIrbInfoEntityService.findIrbInfoForProject(projectId);
        if (info == null) {
            throw new NotFoundException("No IRB file found for project ID " + projectId);
        }
        _projectIrbInfoEntityService.delete(info);
        return true;
    }

    @ApiOperation(value = "Creates a new DQR configuration for a project from the submitted attributes.", notes = "Returns the newly created DQR configuration for a project with the submitted attributes.", response = DqrProjectSettings.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Returns the newly created DQR configuration for the project."),
                   @ApiResponse(code = 403, message = "Insufficient privileges to create the DQR configuration for the project."),
                   @ApiResponse(code = 404, message = "The project for the new DQR configuration wasn't found."),
                   @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @XapiRequestMapping(value = "adminProjectSettings", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST, restrictTo = Admin)
    @ResponseBody
    public DqrProjectSettings createDqrProjectSettings(@RequestBody final DqrProjectSettings settings) throws NotFoundException, DataFormatException, NotModifiedException {
        if (StringUtils.isBlank(settings.getProjectId())) {
            throw new DataFormatException("User " + getSessionUser().getUsername() + " tried to configure project DQR settings without specifying a project ID.");
        }

        final DqrProjectSettings existingSettings = _dqrProjectSettingsService.findSettingsByProject(settings.getProjectId());
        if (existingSettings == null) {
            return _dqrProjectSettingsService.create(settings);
        }

        boolean isDirty = false;
        // Only update fields that are actually included in the submitted data and differ from the original source.
        if (StringUtils.isNotBlank(settings.getProjectId()) && !StringUtils.equals(settings.getProjectId(), existingSettings.getProjectId())) {
            existingSettings.setProjectId(settings.getProjectId());
            isDirty = true;
        }
        if (settings.isEnabled() != existingSettings.isEnabled()) {
            existingSettings.setEnabled(settings.isEnabled());
            isDirty = true;
        }
        if (!isDirty) {
            throw new NotModifiedException("No changes were made to the DQR settings for the project " + settings.getProjectId());
        }
        _dqrProjectSettingsService.update(existingSettings);
        return existingSettings;
    }

    @ApiOperation(value = "Deletes the requested Dqr configuration for the project.", response = Boolean.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Dqr configuration for the project was successfully removed."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Insufficient privileges to delete the Dqr configuration for the project."),
                   @ApiResponse(code = 404, message = "The requested Dqr configuration for the project wasn't found."),
                   @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @XapiRequestMapping(value = "adminProjectSettings/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.DELETE, restrictTo = Admin)
    @ResponseBody
    public boolean deleteDqrProjectSettings(@PathVariable final String projectId) throws NotFoundException {
        final DqrProjectSettings existingSettings = _dqrProjectSettingsService.findSettingsByProject(projectId);
        if (existingSettings == null) {
            throw new NotFoundException("No DQR settings were found for the project " + projectId);
        }
        _dqrProjectSettingsService.delete(existingSettings.getId());
        return true;
    }

    @ApiOperation(value = "Get list of Dqr configurations.", notes = "The Dqr configurations function returns a list of all Dqr configurations in the XNAT system.", response = DqrProjectSettings.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "Returns a list of all of the currently configured Dqr configurations."),
                   @ApiResponse(code = 500, message = "An unexpected or unknown error occurred")})
    @XapiRequestMapping(value = "adminProjectSettings", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    @ResponseBody
    public List<DqrProjectSettings> getAllDqrProjectSettings() {
        return _dqrProjectSettingsService.getAll();
    }

    @ApiOperation(value = "Get Dqr configuration for the specified project.", notes = "The get Dqr configuration function returns the Dqr configuration for the specified project.", response = DqrProjectSettings.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Returns Dqr configuration for the project."),
                   @ApiResponse(code = 500, message = "An unexpected or unknown error occurred")})
    @XapiRequestMapping(value = "adminProjectSettings/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    @ResponseBody
    public DqrProjectSettings getDqrProjectSettings(@PathVariable final String projectId) throws NotFoundException {
        return _dqrProjectSettingsService.findSettingsByProject(projectId);
    }

    @ApiOperation(value = "Creates a new PACS availability interval from the submitted attributes.", notes = "Returns the newly created PACS availability interval with the submitted attributes.", response = PacsAvailability.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Returns the newly created PACS availability interval."),
                   @ApiResponse(code = 403, message = "Insufficient privileges to create the PACS availability interval."),
                   @ApiResponse(code = 404, message = "The requested PACS availability interval wasn't found."),
                   @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @XapiRequestMapping(value = "pacsAvailability/window", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST, restrictTo = Admin)
    @ResponseBody
    public PacsAvailability createPacsAvailabilityInterval(@RequestBody final PacsAvailability settings) throws DataFormatException {
        if (settings.getDayOfWeek().getValue() == 0 || StringUtils.isBlank(settings.getAvailabilityStart()) || StringUtils.isBlank(settings.getAvailabilityEnd())) {
            throw new DataFormatException("User " + getSessionUser().getUsername() + " tried to create a PACS availability interval but did not supply the day of week, start time, and end time.");
        }
        _pacsAvailabilityService.checkOverlap(settings, true);
        if (settings.getUtilizationPercent() == 0 || settings.getThreads() == 0) {
            settings.setUtilizationPercent(0);
            settings.setThreads(0);
        }
        return _pacsAvailabilityService.create(settings);
    }

    @ApiOperation(value = "Checks whether a new PACS availability interval would overlap with any existing intervals.", notes = "Returns whether the posted PACS availability interval would overlap with any existing intervals.", response = Boolean.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Returns whether there is overlap with an existing interval."),
                   @ApiResponse(code = 400, message = "Interval not fully specified."),
                   @ApiResponse(code = 403, message = "Insufficient privileges to check interval overlap."),
                   @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @XapiRequestMapping(value = "pacsAvailability/conflictsExisting", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST, restrictTo = Admin)
    @ResponseBody
    public boolean checkPacsAvailabilityInterval(@RequestBody final PacsAvailability settings) throws DataFormatException {
        if (settings.getDayOfWeek().getValue() == 0 || StringUtils.isBlank(settings.getAvailabilityStart()) || StringUtils.isBlank(settings.getAvailabilityEnd())) {
            throw new DataFormatException("User " + getSessionUser().getUsername() + " tried to check overlap for a PACS availability interval but did not supply the day of week, start time, and end time.");
        }
        return _pacsAvailabilityService.checkOverlap(settings, false);
    }

    @ApiOperation(value = "Updates the requested PACS availability interval using the submitted attributes.", notes = "Returns the updated PACS availability interval.", response = PacsAvailability.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Returns the updated PACS availability interval."),
                   @ApiResponse(code = 304, message = "The requested PACS availability interval is the same as the submitted PACS availability interval."),
                   @ApiResponse(code = 403, message = "Insufficient privileges to edit the requested PACS availability interval."),
                   @ApiResponse(code = 404, message = "The requested PACS availability interval wasn't found."),
                   @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @XapiRequestMapping(value = "pacsAvailability/window/{pacsAvailabilityId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.PUT, restrictTo = Admin)
    @ResponseBody
    public PacsAvailability updatePacsAvailabilityInterval(@PathVariable final long pacsAvailabilityId, @RequestBody final PacsAvailability settings) throws Exception {
        final PacsAvailability existingSettings = _pacsAvailabilityService.get(pacsAvailabilityId);
        if (existingSettings == null) {
            throw new NotFoundException("No PACS availability entry exists for ID " + pacsAvailabilityId);
        }

        boolean isDirty = false;
        // Only update fields that are actually included in the submitted data and differ from the original source.
        if (settings.getDayOfWeek().getValue() != 0 && settings.getDayOfWeek() != existingSettings.getDayOfWeek()) {
            existingSettings.setDayOfWeek(settings.getDayOfWeek());
            isDirty = true;
        }
        if (StringUtils.isNotBlank(settings.getAvailabilityStart()) && !StringUtils.equals(settings.getAvailabilityStart(), existingSettings.getAvailabilityStart())) {
            existingSettings.setAvailabilityStart(settings.getAvailabilityStart());
            isDirty = true;
        }
        if (StringUtils.isNotBlank(settings.getAvailabilityEnd()) && !StringUtils.equals(settings.getAvailabilityEnd(), existingSettings.getAvailabilityEnd())) {
            existingSettings.setAvailabilityEnd(settings.getAvailabilityEnd());
            isDirty = true;
        }
        if (settings.getUtilizationPercent() == 0 || settings.getThreads() == 0) {
            settings.setUtilizationPercent(0);
            settings.setThreads(0);
        }
        if (settings.getUtilizationPercent() != existingSettings.getUtilizationPercent()) {
            existingSettings.setUtilizationPercent(settings.getUtilizationPercent());
            isDirty = true;
        }
        if (settings.getThreads() != existingSettings.getThreads()) {
            existingSettings.setThreads(settings.getThreads());
            isDirty = true;
        }
        if (NumberUtils.compare(settings.getPacsId(), existingSettings.getPacsId()) != 0) {
            existingSettings.setPacsId(settings.getPacsId());
            isDirty = true;
        }

        if (!isDirty) {
            throw new NotModifiedException("No changes were made to the PACS availability settings " + pacsAvailabilityId);
        }
        _pacsAvailabilityService.checkOverlap(settings, true, existingSettings.getId());
        _pacsAvailabilityService.update(existingSettings);
        return existingSettings;
    }

    @ApiOperation(value = "Deletes the requested PACS availability interval.", response = Boolean.class)
    @ApiResponses({@ApiResponse(code = 200, message = "PACS availability interval was successfully removed."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Insufficient privileges to delete the PACS availability interval."),
                   @ApiResponse(code = 404, message = "The requested PACS availability interval wasn't found."),
                   @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @XapiRequestMapping(value = "pacsAvailability/window/{pacsAvailabilityId}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.DELETE, restrictTo = Admin)
    @ResponseBody
    public boolean deletePacsAvailabilityInterval(@PathVariable("pacsAvailabilityId") final long pacsAvailabilityId) throws NotFoundException {
        final PacsAvailability existingSettings;
        try {
            existingSettings = _pacsAvailabilityService.get(pacsAvailabilityId);
        } catch (org.nrg.framework.exceptions.NotFoundException e) {
            throw new NotFoundException("No PACS availability entry exists for ID " + pacsAvailabilityId);
        }
        _pacsAvailabilityService.delete(existingSettings.getId());
        return true;
    }

    @ApiOperation(value = "Get PACS availability interval with the specified ID.", notes = "The get PACS availability interval function returns the PACS availability intervals with the specified ID.", response = PacsAvailability.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Returns PACS availability interval."),
                   @ApiResponse(code = 500, message = "An unexpected or unknown error occurred")})
    @XapiRequestMapping(value = "pacsAvailability/window/{pacsAvailabilityId}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    @ResponseBody
    public PacsAvailability getPacsAvailabilityInterval(@PathVariable final long pacsAvailabilityId) throws NotFoundException {
        try {
            return _pacsAvailabilityService.get(pacsAvailabilityId);
        } catch (org.nrg.framework.exceptions.NotFoundException e) {
            throw new NotFoundException("No PACS availability entry exists for ID " + pacsAvailabilityId);
        }
    }

    @ApiOperation(value = "Get PACS availability intervals for the specified PACS.", notes = "The get PACS availability intervals function returns the PACS availability intervals for the specified PACS.", response = PacsAvailability.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "Returns PACS availability intervals for the PACS."),
                   @ApiResponse(code = 500, message = "An unexpected or unknown error occurred")})
    @XapiRequestMapping(value = "pacsAvailability/windows/{pacsId}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    @ResponseBody
    public List<PacsAvailability> getPacsAvailabilityIntervals(@PathVariable final long pacsId) {
        return _pacsAvailabilityService.findAllByPacsId(pacsId);
    }

    @ApiOperation(value = "Get PACS availability intervals by day for the specified PACS.", notes = "The get PACS availability intervals by day function returns the PACS availability intervals for the specified PACS.", response = PacsAvailability.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "Returns PACS availability intervals by day for the PACS."),
                   @ApiResponse(code = 500, message = "An unexpected or unknown error occurred")})
    @XapiRequestMapping(value = "pacsAvailability/windows/{pacsId}/byDay", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    @ResponseBody
    public Map<DayOfWeek, List<PacsAvailability>> getPacsAvailabilityIntervalsByDay(@PathVariable final long pacsId) {
        return _pacsAvailabilityService.findAllByPacsIdGroupedByDayOfWeek(pacsId);
    }

    @ApiOperation(value = "Get list of the series in a list of studies.", notes = "The get series function returns a list of the series in the listed studies.", response = String.class, responseContainer = "Map")
    @ApiResponses({@ApiResponse(code = 200, message = "A queued DICOM query request."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the series."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "seriesInfo/pacs/{pacsId}/studies", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST, restrictTo = Authorizer)
    public Map<String, PacsSearchResults<Series>> getSeries(@ApiParam(value = "ID of the pacs to query", required = true) @PathVariable final long pacsId,
                                                            @ApiParam("List of studies to get series for.") @RequestBody final String ids) throws NoContentException, PacsNotQueryableException {
        if (StringUtils.isBlank(ids)) {
            throw new NoContentException("No study instance UIDs specified for query on PACS " + pacsId);
        }

        final UserI        user              = getSessionUser();
        final Pacs         pacs              = _pacsEntityService.retrieve(pacsId);
        final List<String> studyInstanceUids = Arrays.asList(StringUtils.split(RegExUtils.removeAll(ids, "\n[\\[\\]\"' ]").trim(), ","));
        try {
            return _pacsService.getSeriesByStudyUid(user, pacs, studyInstanceUids);
        } catch (PacsNotQueryableException e) {
            log.error("An error occurred trying to retrieve series for user {} from PACS {} for study instance UIDs {}", user.getUsername(), pacsId, String.join(", ", studyInstanceUids), e);
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

    @ApiOperation(value = "Get list of the series in a list of studies.", notes = "The get series function returns a list of the series in the listed studies.", response = String.class, responseContainer = "Map")
    @ApiResponses({@ApiResponse(code = 200, message = "A queued DICOM query request."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the series."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "seriesInfo/pacs/{pacsId}/studies/{searchId}", produces = MediaType.APPLICATION_JSON_VALUE, restrictTo = Authorizer)
    public ResponseEntity<Map<String, PacsSearchResults<Series>>> getSeries(@ApiParam(value = "ID of the pacs to query", required = true) @PathVariable final long pacsId,
                                                                            @ApiParam(value = "ID of the search request", required = true) @PathVariable final UUID searchId) throws NotFoundException, URISyntaxException {
        if (!_pacsService.getSearchStatus(searchId)) {
            final PacsSearchRequest request = _pacsService.getSearchRequest(searchId);
            if (request.getPacsId() != pacsId) {
                throw new NotFoundException("There is no request ID " + searchId + " associated with the specified PACS instance " + pacsId);
            }
            return ResponseEntity.status(HttpStatus.CREATED).location(new URI(_siteConfigPreferences.getSiteUrl() + "/xapi/dqr/seriesInfo/pacs/" + pacsId + "/studies/" + searchId)).build();
        }

        return ResponseEntity.ok(_pacsService.getSearchResults(searchId));
    }

    private void notifyAdminOfCompleteIrbInfo(final String projectId, final ProjectIrbInfo info, final UserI user) {
        try {
            final String adminEmail = _siteConfigPreferences.getAdminEmail();
            _mailService.sendMessage(adminEmail, adminEmail,
                                     String.format("[ %s ] Project IRB Info Stored for %s", TurbineUtils.GetSystemName(), projectId),
                                     String.format("IRB info (containing IRB number %s) has been stored for project %s by user %s. You can review this info by going to the project's Project Settings page. If this IRB info is acceptable, you can add the project to the list of projects that are permitted to use DQR in Plugin Settings.", info.getIrbNumber(), projectId, user.getUsername()));
        } catch (final Exception e) {
            log.error(String.format("User %s saved IRB info for project %s but there was an error notifying the admin.", user.getUsername(), projectId));
        }
    }

    private <T> List<T> getRangedList(final List<T> list, final int rangeStart, final int rangeEnd, final String sortOrder) {
        return getSortedSubList(list, Math.max(1, rangeStart) - 1, Math.min(rangeEnd, list.size()), sortOrder);
    }

    private <T> List<T> getPagedList(final List<T> list, final int pageIndex, final int pageSize, final String sortOrder) throws NoContentException {
        final int start = pageIndex * pageSize;
        final int size  = list.size();
        if (start >= size) {
            throw new NoContentException("The requested start index is larger than the total number of available requests.");
        }
        return getSortedSubList(list, start, Math.min((start + pageSize), size), sortOrder);
    }

    @Nonnull
    private <T> List<T> getSortedSubList(final List<T> list, final int start, final int end, final String sortOrder) {
        final List<T> subList = list.subList(start, end);
        if (!sortOrder.equalsIgnoreCase("asc")) {
            Collections.reverse(list);
        }
        return subList;
    }

    private static final String  QUERY_SUBMITTED    = "Query Submitted.";
    private static final String  PACS_NOT_AVAILABLE = "This PACS is not currently available, but your request is queued and will be serviced when the PACS is available.";
    private static final Pattern STRIP_JSON         = Pattern.compile("^\\s*\\[?\\s*(?<body>.*)\\s*]?\\s*$");

    private final PacsService                 _pacsService;
    private final PacsEntityService           _pacsEntityService;
    private final ProjectIrbInfoEntityService _projectIrbInfoEntityService;
    private final PacsPingService             _pacsPingService;
    private final ExecutedPacsRequestService  _executedRequestService;
    private final QueuedPacsRequestService    _queuedRequestService;
    private final DqrProjectSettingsService   _dqrProjectSettingsService;
    private final DqrPreferences              _preferences;
    private final PacsAvailabilityService     _pacsAvailabilityService;
    private final Map<String, OrmStrategy>    _ormStrategies;
    private final SiteConfigPreferences       _siteConfigPreferences;
    private final MailService                 _mailService;
}
