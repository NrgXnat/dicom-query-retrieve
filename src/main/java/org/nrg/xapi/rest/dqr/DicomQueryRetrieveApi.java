package org.nrg.xapi.rest.dqr;

import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.nrg.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.dqr.domain.Series;
import org.nrg.dqr.domain.entities.*;
import org.nrg.dqr.dto.PacsSearchResults;
import org.nrg.dqr.preferences.DqrPreferences;
import org.nrg.dqr.services.*;
import org.nrg.dqr.util.*;
import org.nrg.framework.annotations.XapiRestController;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.prefs.exceptions.InvalidPreferenceName;
import org.nrg.xapi.authorization.DqrUserXapiAuthorization;
import org.nrg.xapi.rest.*;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatMrsessiondata;
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
import org.nrg.xnat.restlet.extensions.PacsNotFoundException;
import org.nrg.xnat.restlet.extensions.PacsNotStorableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLConnection;
import java.util.*;

import static org.nrg.xdat.security.helpers.AccessLevel.*;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

/**
 * Created by mike on 1/19/18.
 */
@Api(description = "Dicom Query Retrieve API")
@XapiRestController
@Slf4j
@RequestMapping(value = "/dqr")
public class DicomQueryRetrieveApi extends AbstractXapiRestController {
    @Autowired
    protected DicomQueryRetrieveApi(DqrPreferences prefs, UserManagementServiceI userManagementService, RoleHolder roleHolder, ExecutedPacsRequestService requestService, QueuedPacsRequestService queuedRequestService, PacsService pacsService, PacsEntityService pacsEntityService, PacsPingService pacsPingService, ProjectIrbInfoEntityService projectIrbInfoEntityService, DqrAdminSettingsForProjectService adminSettingsForProjectService, PacsAvailabilityEntityService pacsAvailabilityEntityService) {
        super(userManagementService, roleHolder);
        _preferences = prefs;
        _executedRequestService = requestService;
        _queuedRequestService = queuedRequestService;
        _pacsService = pacsService;
        _pacsEntityService = pacsEntityService;
        _pacsPingService = pacsPingService;
        _projectIrbInfoEntityService = projectIrbInfoEntityService;
        _adminSettingsForProjectService = adminSettingsForProjectService;
        _pacsAvailabilityEntityService = pacsAvailabilityEntityService;
    }



    @ApiOperation(value = "Get list of all DICOM query requests.", notes = "The DICOM query history function returns a list of all DICOM queries that have ever been made on the XNAT system with brief information about each.", response = ExecutedPacsRequest.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "A list of DICOM query requests."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the list of DICOM query requests."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "query/history/all", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public ResponseEntity<List<ExecutedPacsRequest>> queryHistoryGet() {
        return new ResponseEntity<>(_executedRequestService.getAll(), HttpStatus.OK);
    }



    @ApiOperation(value = "Get count of all DICOM query requests.", notes = "The DICOM query history count function returns a count of all DICOM queries that have ever been made on the XNAT system.", response = Integer.class)
    @ApiResponses({@ApiResponse(code = 200, message = "A count of DICOM query requests."),
            @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
            @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the count of DICOM query requests."),
            @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "query/history/all/count", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public ResponseEntity<Integer> queryHistoryCountGet() {
        return new ResponseEntity<>(_executedRequestService.getAll().size(), HttpStatus.OK);
    }



    @ApiOperation(value = "Get DICOM query request history entries for the current user.", notes = "The DICOM query history function returns a list of all DICOM queries that have ever been made on the XNAT system for the current user with brief information about each.", response = ExecutedPacsRequest.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "A list of DICOM query requests."),
            @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
            @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the list of DICOM query requests."),
            @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "query/history/user", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authorizer)
    public ResponseEntity<List<ExecutedPacsRequest>> userQueryHistoryGet(
            @ApiParam("Sort order") @RequestParam(name = "sort", defaultValue = "desc") final String sortOrder
    ) {
        final UserI user = getSessionUser();
        List<ExecutedPacsRequest> allRequests = _executedRequestService.getAllForUser(user);
        if (!sortOrder.equalsIgnoreCase("asc")) {
            Collections.reverse(allRequests);
        }
        return new ResponseEntity<>(allRequests, HttpStatus.OK);
    }



    @ApiOperation(value = "Get count of DICOM query request history entries for the current user.", notes = "The DICOM query history count function returns a count of all DICOM queries that have ever been made on the XNAT system for the current user.", response = Integer.class)
    @ApiResponses({@ApiResponse(code = 200, message = "A count of DICOM query requests."),
            @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
            @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the count of DICOM query requests."),
            @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "query/history/user/count", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authorizer)
    public ResponseEntity<Integer> userQueryHistoryCountGet() {
        final UserI user = getSessionUser();
        return new ResponseEntity<>(_executedRequestService.getAllForUser(user).size(), HttpStatus.OK);
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
    public ResponseEntity<List<ExecutedPacsRequest>> queryHistoryGetRange(
            @ApiParam("Sort order") @RequestParam(name = "sort", defaultValue = "desc") final String sortOrder,
            @ApiParam("Range start") @RequestParam(name = "start", defaultValue = "1") final int rangeStart,
            @ApiParam("Range end") @RequestParam(name = "end", defaultValue = "100") final int rangeEnd) {
        List<ExecutedPacsRequest> allRequests = _executedRequestService.getAll();
        int start = Math.max(1, rangeStart);
        int end = Math.min(rangeEnd, allRequests.size());
        // if (start > end) {
        //     return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        // }
        if (start > allRequests.size()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        if (!sortOrder.equalsIgnoreCase("asc")) {
            Collections.reverse(allRequests);
        }
        List<ExecutedPacsRequest> requestsPage = allRequests.subList(start - 1, end);
        return new ResponseEntity<>(requestsPage, HttpStatus.OK);
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
    public ResponseEntity<List<ExecutedPacsRequest>> queryHistoryGetPaged(
            @ApiParam("Sort order") @RequestParam(name = "sort", defaultValue = "desc") final String sortOrder,
            @ApiParam("Page index") @RequestParam(name = "page", defaultValue = "0") final int pageIndex,
            @ApiParam("Page size") @RequestParam(name = "size", defaultValue = "100") final int pageSize) {
        List<ExecutedPacsRequest> allRequests = _executedRequestService.getAll();
        int start = pageIndex * pageSize;
        int end = Math.min((start + pageSize), allRequests.size());
        // if (start > end) {
        //    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        // }
        if (start >= allRequests.size()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        if (!sortOrder.equalsIgnoreCase("asc")) {
            Collections.reverse(allRequests);
        }
        List<ExecutedPacsRequest> requestsPage = allRequests.subList(start, end);
        return new ResponseEntity<>(requestsPage, HttpStatus.OK);
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
    public ResponseEntity<List<ExecutedPacsRequest>> userQueryHistoryGetRange(
            @ApiParam("Sort order") @RequestParam(name = "sort", defaultValue = "desc") final String sortOrder,
            @ApiParam("Range start") @RequestParam(name = "start", defaultValue = "1") final int rangeStart,
            @ApiParam("Range end") @RequestParam(name = "end", defaultValue = "100") final int rangeEnd) {
        final UserI user = getSessionUser();
        List<ExecutedPacsRequest> allRequests = _executedRequestService.getAllForUser(user);
        int start = Math.max(1, rangeStart);
        int end = Math.min(rangeEnd, allRequests.size());
        // if (start > end) {
        //     return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        // }
        // if (start > allRequests.size()) {
        //    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        // }
        if (!sortOrder.equalsIgnoreCase("asc")) {
            Collections.reverse(allRequests);
        }
        List<ExecutedPacsRequest> requestsPage = allRequests.subList(start - 1, end);
        return new ResponseEntity<>(requestsPage, HttpStatus.OK);
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
    public ResponseEntity<List<ExecutedPacsRequest>> userQueryHistoryGetPaged(
            @ApiParam("Sort order") @RequestParam(name = "sort", defaultValue = "desc") final String sortOrder,
            @ApiParam("Page index") @RequestParam(name = "page", defaultValue = "0") final int pageIndex,
            @ApiParam("Page size") @RequestParam(name = "size", defaultValue = "100") final int pageSize) {
        final UserI user = getSessionUser();
        List<ExecutedPacsRequest> allRequests = _executedRequestService.getAllForUser(user);
        int start = pageIndex * pageSize;
        int end = Math.min((start + pageSize), allRequests.size());
        // if (start > end) {
        //     return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        // }
        if (start >= allRequests.size()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        if (!sortOrder.equalsIgnoreCase("asc")) {
            Collections.reverse(allRequests);
        }
        List<ExecutedPacsRequest> requestsPage = allRequests.subList(start, end);
        return new ResponseEntity<>(requestsPage, HttpStatus.OK);
    }


    @ApiOperation(value = "Get DICOM query request by ID.", notes = "The DICOM query history function returns information about the DICOM query with a given ID.", response = ExecutedPacsRequest.class)
    @ApiResponses({@ApiResponse(code = 200, message = "A DICOM query request."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the DICOM query request."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "query/history/request/{id}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authorizer)
    public ResponseEntity<ExecutedPacsRequest> queryHistoryGet(@ApiParam(value = "ID of the query request to fetch", required = true) @PathVariable("id") final String id) {
        try {
            final UserI user = getSessionUser();
            if (Roles.isSiteAdmin(user)) {
                return new ResponseEntity<>(_executedRequestService.get(Long.parseLong(id)), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(_executedRequestService.getByIdForUser(Long.parseLong(id), user), HttpStatus.OK);
            }
        } catch (NotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }



    @ApiOperation(value = "Get list of queued DICOM query requests for all users.", notes = "The DICOM query queue function returns a list of all DICOM queries that are currently queued on the XNAT system with brief information about each.", response = QueuedPacsRequest.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "A list of queued DICOM query requests."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the list of queued DICOM query requests."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "query/queue/all", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public ResponseEntity<List<QueuedPacsRequest>> queryQueueGet() {
        return new ResponseEntity<>(_queuedRequestService.getAll(), HttpStatus.OK);
    }



    @ApiOperation(value = "Get count of queued DICOM query requests for all users.", notes = "The DICOM query queue count function returns a count of all DICOM queries that are currently queued on the XNAT system.", response = Integer.class)
    @ApiResponses({@ApiResponse(code = 200, message = "A count of queued DICOM query requests."),
            @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
            @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the count of queued DICOM query requests."),
            @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "query/queue/all/count", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public ResponseEntity<Integer> queryQueueCountGet() {
        return new ResponseEntity<>(_queuedRequestService.getAll().size(), HttpStatus.OK);
    }



    @ApiOperation(value = "Get list of queued DICOM query requests for the current user.", notes = "The DICOM query queue function returns a list of all DICOM queries that are currently queued on the XNAT system for the current user with brief information about each.", response = QueuedPacsRequest.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "A list of queued DICOM query requests."),
            @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
            @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the list of queued DICOM query requests."),
            @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "query/queue/user", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authorizer)
    public ResponseEntity<List<QueuedPacsRequest>> queryUserQueueGet() {
        final UserI user = getSessionUser();
        return new ResponseEntity<>(_queuedRequestService.getAllForUser(user), HttpStatus.OK);
    }



    @ApiOperation(value = "Get count of queued DICOM query requests for the current user.", notes = "The DICOM query queue count function returns a count of all DICOM queries that are currently queued on the XNAT system for the current user.", response = Integer.class)
    @ApiResponses({@ApiResponse(code = 200, message = "A count of queued DICOM query requests."),
            @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
            @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the count of queued DICOM query requests."),
            @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "query/queue/user/count", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authorizer)
    public ResponseEntity<Integer> queryUserQueueCountGet() {
        final UserI user = getSessionUser();
        return new ResponseEntity<>(_queuedRequestService.getAllForUser(user).size(), HttpStatus.OK);
    }



    @ApiOperation(value = "Get list of queued DICOM query requests with order information for all users.", notes = "The DICOM query queue function returns a list of all DICOM queries that are currently queued on the XNAT system with brief information about each (including order information).", response = QueuedPacsRequest.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "A list of queued DICOM query requests."),
            @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
            @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the list of queued DICOM query requests."),
            @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "query/queue/all/ordered", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public ResponseEntity<List<Map<String, Object>>> queryQueueWithOrderGet(
            @ApiParam("Sort order") @RequestParam(name = "sort", defaultValue = "asc") final String sortOrder
    ) {
        List<Map<String, Object>> allRequests = _queuedRequestService.getAllWithOrder();
        if (sortOrder.equalsIgnoreCase("desc")) {
            Collections.reverse(allRequests);
        }
        return new ResponseEntity<>(allRequests, HttpStatus.OK);
    }



    @ApiOperation(value = "Get list of queued DICOM query requests for the current user with order information.", notes = "The DICOM query queue function returns a list of all DICOM queries that are currently queued on the XNAT system for the current user with brief information about each (including order information).", response = QueuedPacsRequest.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "A list of queued DICOM query requests."),
            @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
            @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the list of queued DICOM query requests."),
            @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "query/queue/user/ordered", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authorizer)
    public ResponseEntity<List<Map<String, Object>>> queryUserQueueWithOrderGet(
            @ApiParam("Sort order") @RequestParam(name = "sort", defaultValue = "asc") final String sortOrder
    ) {
        final UserI user = getSessionUser();
        List<Map<String, Object>> allRequests = _queuedRequestService.getAllWithOrderForUser(user);
        if (sortOrder.equalsIgnoreCase("desc")) {
            Collections.reverse(allRequests);
        }
        return new ResponseEntity<>(allRequests, HttpStatus.OK);
    }



    @ApiOperation(value = "Get paged list of queued DICOM query requests for all users.", notes = "The DICOM query queue function returns a list of all DICOM queries that are currently queued on the XNAT system with brief information about each.", response = QueuedPacsRequest.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "A list of queued DICOM query requests."),
            @ApiResponse(code = 204, message = "No results. Invalid page range."),
            @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
            @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the list of queued DICOM query requests."),
            @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "query/queue/all/paged", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public ResponseEntity<List<QueuedPacsRequest>> queryQueuePagedGet(
            @ApiParam("Page index") @RequestParam(name = "page", defaultValue = "0") final int pageIndex,
            @ApiParam("Page size") @RequestParam(name = "size", defaultValue = "100") final int pageSize) {
        List<QueuedPacsRequest> allRequests = _queuedRequestService.getAll();
        int start = pageIndex * pageSize;
        int end = Math.min((start + pageSize), allRequests.size());
        // if (start > end) {
        //    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        // }
        if (start >= allRequests.size()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        Collections.reverse(allRequests);
        List<QueuedPacsRequest> requestsPage = allRequests.subList(start, end);
        return new ResponseEntity<>(requestsPage, HttpStatus.OK);
    }



    @ApiOperation(value = "Get paged list of queued DICOM query requests for the current user.", notes = "The DICOM query queue function returns a list of all DICOM queries that are currently queued on the XNAT system for the current user with brief information about each.", response = QueuedPacsRequest.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "A list of queued DICOM query requests."),
            @ApiResponse(code = 204, message = "No results. Invalid page range."),
            @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
            @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the list of queued DICOM query requests."),
            @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "query/queue/user/paged", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authorizer)
    public ResponseEntity<List<QueuedPacsRequest>> queryUserQueuePagedGet(
            @ApiParam("Page index") @RequestParam(name = "page", defaultValue = "0") final int pageIndex,
            @ApiParam("Page size") @RequestParam(name = "size", defaultValue = "100") final int pageSize) {
        final UserI user = getSessionUser();
        List<QueuedPacsRequest> allRequests = _queuedRequestService.getAllForUser(user);
        int start = pageIndex * pageSize;
        int end = Math.min((start + pageSize), allRequests.size());
        // if (start > end) {
        //    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        // }
        if (start >= allRequests.size()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        Collections.reverse(allRequests);
        List<QueuedPacsRequest> requestsPage = allRequests.subList(start, end);
        return new ResponseEntity<>(requestsPage, HttpStatus.OK);
    }



    @ApiOperation(value = "Get paged list of queued DICOM query requests with order information for all users.", notes = "The DICOM query queue function returns a list of all DICOM queries that are currently queued on the XNAT system with brief information about each (including order information).", response = QueuedPacsRequest.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "A list of queued DICOM query requests."),
            @ApiResponse(code = 204, message = "No results. Invalid page range."),
            @ApiResponse(code = 400, message = "Request could not be completed as submitted."),
            @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
            @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the list of queued DICOM query requests."),
            @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "query/queue/all/ordered/paged", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public ResponseEntity<List<Map<String, Object>>> queryQueueGetPagedWithOrder(
            @ApiParam("Sort order") @RequestParam(name = "sort", defaultValue = "asc") final String sortOrder,
            @ApiParam("Page index") @RequestParam(name = "page", defaultValue = "0") final int pageIndex,
            @ApiParam("Page size") @RequestParam(name = "size", defaultValue = "100") final int pageSize) {
        List<Map<String, Object>> allRequests = _queuedRequestService.getAllWithOrder();
        int start = pageIndex * pageSize;
        int end = Math.min((start + pageSize), allRequests.size());
        // if (start > end) {
        //    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        // }
        if (start > allRequests.size()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        if (sortOrder.equalsIgnoreCase("desc")) {
            Collections.reverse(allRequests);
        }
        List<Map<String, Object>> requestsPage = allRequests.subList(start, end);
        return new ResponseEntity<>(requestsPage, HttpStatus.OK);
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
    public ResponseEntity<List<Map<String, Object>>> queryUserQueueGetPagedWithOrder(
            @ApiParam("Sort order") @RequestParam(name = "sort", defaultValue = "asc") final String sortOrder,
            @ApiParam("Page index") @RequestParam(name = "page", defaultValue = "0") final int pageIndex,
            @ApiParam("Page size") @RequestParam(name = "size", defaultValue = "100") final int pageSize) {
        final UserI user = getSessionUser();
        List<Map<String, Object>> allRequests = _queuedRequestService.getAllWithOrderForUser(user);
        int start = pageIndex * pageSize;
        int end = Math.min((start + pageSize), allRequests.size());
        // if (start > end) {
        //     return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        // }
        if (start >= allRequests.size()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        if (sortOrder.equalsIgnoreCase("desc")) {
            Collections.reverse(allRequests);
        }
        List<Map<String, Object>> requestsPage = allRequests.subList(start, end);
        return new ResponseEntity<>(requestsPage, HttpStatus.OK);
    }



    @ApiOperation(value = "Get data for queued DICOM query request by ID.", notes = "The DICOM query queue function returns information about the queued DICOM query with a given ID.", response = QueuedPacsRequest.class)
    @ApiResponses({@ApiResponse(code = 200, message = "A queued DICOM query request."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the DICOM query request."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "query/queue/request/{id}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authorizer)
    public ResponseEntity<QueuedPacsRequest> queryQueueGet(@ApiParam(value = "ID of the queued query request to fetch", required = true) @PathVariable("id") final String id) {
        try {
            final UserI user = getSessionUser();
            if (Roles.isSiteAdmin(user)) {
                return new ResponseEntity<>(_queuedRequestService.get(Long.parseLong(id)), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(_queuedRequestService.getByIdForUser(Long.parseLong(id), user), HttpStatus.OK);
            }
        } catch (NotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }



    @ApiOperation(value = "Deletes the queued DICOM query request with given ID.", notes = "Returns true if the queued DICOM query request was successfully deleted. Returns false otherwise.", response = Boolean.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Returns true to indicate the queued DICOM query request was successfully deleted."),
                   @ApiResponse(code = 403, message = "The user doesn't have permission to delete queued DICOM query requests."),
                   @ApiResponse(code = 404, message = "The queued DICOM query request wasn't found."),
                   @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "query/queue/request/{id}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.DELETE, restrictTo = Authorizer)
    public ResponseEntity<Boolean> queryQueueDelete(@ApiParam(value = "ID of the queued query request to delete", required = true) @PathVariable("id") final String id) {
        try {
            final UserI       user = getSessionUser();
            QueuedPacsRequest req  = _queuedRequestService.get(Long.parseLong(id));
            if (req == null) {
                return new ResponseEntity<>(false, HttpStatus.NOT_FOUND);
            }
            if (Roles.isSiteAdmin(user)) {
                _queuedRequestService.delete(Long.parseLong(id));
                return new ResponseEntity<>(true, HttpStatus.OK);
            } else {
                if (StringUtils.equals(req.getUsername(), user.getUsername())) {
                    _queuedRequestService.delete(Long.parseLong(id));
                    return new ResponseEntity<>(true, HttpStatus.OK);
                } else {
                    return new ResponseEntity<>(HttpStatus.FORBIDDEN);
                }
            }
        } catch (NotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }



    @ApiOperation(value = "Deletes the queued DICOM query requests with given IDs.", notes = "Returns true if the queued DICOM query request was successfully deleted. Returns false otherwise.", response = Boolean.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Returns true to indicate the queued DICOM query requests were successfully deleted."),
            @ApiResponse(code = 403, message = "The user doesn't have permission to delete queued DICOM query requests."),
            @ApiResponse(code = 404, message = "The queued DICOM query requests weren't found."),
            @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "query/queue/deleteRequests", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST, restrictTo = Authorizer)
    public ResponseEntity<Boolean> queryQueueDeleteMultiple(@RequestBody final String[] idsToDelete) {
        try {
            final UserI       user = getSessionUser();
            for(String idToDelete :idsToDelete) {
                QueuedPacsRequest req = _queuedRequestService.get(Long.parseLong(idToDelete));
                if (req == null) {
                    return new ResponseEntity<>(false, HttpStatus.NOT_FOUND);
                }
                if (Roles.isSiteAdmin(user)) {
                    _queuedRequestService.delete(Long.parseLong(idToDelete));
                } else {
                    if (StringUtils.equals(req.getUsername(), user.getUsername())) {
                        _queuedRequestService.delete(Long.parseLong(idToDelete));
                    } else {
                        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
                    }
                }
            }
        } catch (NotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(true, HttpStatus.OK);
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
//        if (!csv.getContentType().contains("csv")) {
//            throw new ServletRequestBindingException("Incorrect file format. Spreadsheet file must be of type: application/csv");
//        }

        final File temp = File.createTempFile("xnat", "csv");
        try (final FileOutputStream fos = new FileOutputStream(temp)) {
            fos.write(csv.getBytes());
        }
        final List<CsvRow> rows = _pacsService.extractImportRequestFromCsv(getSessionUser(), temp, pacsId, allowRowThatGetsAllStudiesOnPacs);

        boolean anonScriptFound = false;
        for (CsvRow row : rows) {
            if (row != null && StringUtils.isNotBlank(row.getAnonScript())) {
                anonScriptFound = true;
            }
        }
        if (anonScriptFound) {
            return new ResponseEntity<>(rows, HttpStatus.OK);
        } else {
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.WARNING, "The generated JSON has no anon script.");

            return ResponseEntity.ok()
                                 .headers(headers)
                                 .body(rows);
        }
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
        final List<FindRow> rows = _pacsService.extractNewImportRequestFromCsv(getSessionUser(), temp, pacsId, allowRowThatGetsAllStudiesOnPacs);

        boolean anonScriptFound = false;
        for (FindRow row : rows) {
            Map<String,String> relabelMap = row.getRelabelMap();
            if (row != null && relabelMap!=null && relabelMap.size()>0) {
                anonScriptFound = true;
            }
        }
        if (anonScriptFound) {
            return new ResponseEntity<>(rows, HttpStatus.OK);
        } else {
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.WARNING, "The generated JSON has no anon script.");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(rows);
        }
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
    @XapiRequestMapping(value = "csvimport/importFromJson",
                        method = RequestMethod.POST,
                        consumes = MediaType.APPLICATION_JSON_VALUE,
                        produces = MediaType.APPLICATION_JSON_VALUE,
                        restrictTo = Authorizer)
    public ResponseEntity<Boolean> importFromPacs(@RequestBody final CsvRow[] rows,
                                                  @ApiParam("Pacs to query.") @RequestParam(name = "pacsId") final Long pacsId,
                                                  @ApiParam("XNAT SCP receiver to send to (Must be formatted as AE_TITLE:PORT).") @RequestParam(name = "ae") final String ae,
                                                  @ApiParam("XNAT project to send to.") @RequestParam(name = "project") final String project,
                                                  @ApiParam("Force the import to happen even if requested remapping won't take place.") @RequestParam(name = "importEvenIfCustomProcessingIsOff", required = false) final boolean importEvenIfCustomProcessingIsOff) throws Exception {
        UserI                      user             = getSessionUser();
        if (!_preferences.getAllowAllProjectsToUseDqr() && !_adminSettingsForProjectService.isDqrEnabledForProject(project)) {
            //You cannot import into a project that does not have DQR enabled.
            return new ResponseEntity<>(false, HttpStatus.FORBIDDEN);
        } else if (!Permissions.canEditProject(user, project) && !Roles.checkRole(user, "Administrator") && !Groups.hasAllDataAccess(user)) {
            return new ResponseEntity<>(false, HttpStatus.FORBIDDEN);
        } else {
            HttpHeaders headers = new HttpHeaders();
            if (!_pacsService.processSpreadsheetImportFromRows(getSessionUser(), Arrays.asList(rows), ae, project, pacsId, importEvenIfCustomProcessingIsOff)) {
                headers.add(HttpHeaders.WARNING, "This PACS is not currently available, but your request is queued and will be serviced when the PACS is available.");
            } else {
                headers.add(HttpHeaders.WARNING, "Query Submitted.");
            }
            return ResponseEntity.ok()
                                 .headers(headers)
                                 .body(true);
        }
    }



    @ApiOperation(value = "Issues the PACS import requests specified in the JSON and performs the specified remapping on the data when it comes in.", response = Boolean.class)
    @ApiResponses({@ApiResponse(code = 200, message = "PACS requests successfully issued."),
            @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
            @ApiResponse(code = 500, message = "Unexpected error")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "csvimport/generalImportFromJson",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE,
            restrictTo = Authorizer)
    public ResponseEntity<Boolean> importFromPacsGeneral(@RequestBody final Map<String, StudyImportInformation> studiesToImport,
                                                        @ApiParam("Pacs to query.") @RequestParam(name = "pacsId") final Long pacsId,
                                                        @ApiParam("XNAT SCP receiver to send to (Must be formatted as AE_TITLE:PORT).") @RequestParam(name = "ae") final String ae,
                                                        @ApiParam("XNAT project to send to.") @RequestParam(name = "project") final String project,
                                                        @ApiParam("Force the import to happen even if requested remapping won't take place.") @RequestParam(name = "importEvenIfCustomProcessingIsOff", required = false) final boolean importEvenIfCustomProcessingIsOff) throws Exception {
        UserI                      user             = getSessionUser();
        if (!_preferences.getAllowAllProjectsToUseDqr() && !_adminSettingsForProjectService.isDqrEnabledForProject(project)) {
            //You cannot import into a project that does not have DQR enabled.
            return new ResponseEntity<>(false, HttpStatus.FORBIDDEN);
        } else if (!Permissions.canEditProject(user, project) && !Roles.checkRole(user, "Administrator") && !Groups.hasAllDataAccess(user)) {
            return new ResponseEntity<>(false, HttpStatus.FORBIDDEN);
        } else {
            HttpHeaders headers = new HttpHeaders();
            if (!_pacsService.processSpreadsheetImport(studiesToImport, getSessionUser(), ae, project, pacsId, importEvenIfCustomProcessingIsOff)) {
                headers.add(HttpHeaders.WARNING, "This PACS is not currently available, but your request is queued and will be serviced when the PACS is available.");
            } else {
                headers.add(HttpHeaders.WARNING, "Query Submitted.");
            }
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(true);
        }
    }



    @ApiOperation(value = "Sends selected scans to PACS.", response = String.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Scans sent to PACS."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "send/toPacs",
                        method = RequestMethod.PUT,
                        consumes = MediaType.APPLICATION_JSON_VALUE,
                        produces = MediaType.APPLICATION_JSON_VALUE,
                        restrictTo = Authorizer)
    public ResponseEntity<Map<String, Object>> sendToPacs(@ApiParam("Id of PACS to send to.") @RequestParam(name = "pacsId") final String pacs,
                                                          @ApiParam("XNAT session to send.") @RequestParam(name = "session") final String session,
                                                          @ApiParam("Array of scans in the session to send.") @RequestParam(name = "scansToExport") final String[] scansToExport) throws Exception {
        final UserI         user       = getSessionUser();
        Map<String, Object> dataToSend = new HashMap<>();

        final long        pacsId            = Long.valueOf(pacs);
        PacsEntityService pacsEntityService = XDAT.getContextService().getBean(PacsEntityService.class);
        final Pacs        _pacs             = pacsEntityService.retrieve(pacsId);

        if (_pacs == null) {
            throw new PacsNotFoundException();
        }

        if (StringUtils.isBlank(session)) {
            throw new RuntimeException("You must specify a session ID for this operation.");
        }
        XnatExperimentdata   temp          = XnatExperimentdata.getXnatExperimentdatasById(session, user, false);
        XnatImagesessiondata sessionObject = null;
        if (temp instanceof XnatImagesessiondata) {
            sessionObject = (XnatImagesessiondata) temp;
        }
        if (sessionObject == null) {
            throw new RuntimeException("Couldn't find a session corresponding to the submitted session ID: " + session);
        }
        if(!Permissions.canRead(user, sessionObject)){
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        dataToSend.put("session", session);
        try {
            if (scansToExport == null) {
                throw new RuntimeException("No scan IDs found to export, returning.");
            } else {
                ArrayList<String> scans = new ArrayList<>();
                if (_pacs.isStorable()) {
                    for (String scanId : scansToExport) {
                        final XnatImagescandata scan = sessionObject.getScanById(scanId);
                        scans.add(scanId);
                        new Thread() {
                            public void run() {
                                _pacsService.exportSeries(user, _pacs, scan);
                            }
                        }.start();
                        log.info("Exported series {} from session {}", scanId, sessionObject.getId());
                    }
                    final EventDetails eventDetails;
                    eventDetails = EventUtils.newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.TYPE.PROCESS, "EXPORT_TO_PACS_REQUEST");
                    eventDetails.setComment("Pacs: " + pacs);
                    PersistentWorkflowI wrk = PersistentWorkflowUtils.buildOpenWorkflow(user, XnatMrsessiondata.SCHEMA_ELEMENT_NAME, session, temp.getProject(), eventDetails);
                    assert wrk != null;
                    PersistentWorkflowUtils.complete(wrk, wrk.buildEvent());
                } else {
                    throw new PacsNotStorableException();
                }
                dataToSend.put("scans", scans);

                log.debug("User {} exported {} scans from session {}", user.getLogin(), scansToExport.length, sessionObject.getId());
            }
        } catch (Exception exception) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(dataToSend, HttpStatus.OK);
    }



    @ApiOperation(value = "Ping a PACS.", notes = "The ping PACS function returns whether the PACS was responsive.", response = PacsPing.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Whether the PACS was responsive."),
                   @ApiResponse(code = 401, message = "Must be authenticated to ping PACS."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to ping PACS."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "pacsStatus/ping/{id}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authorizer)
    @AuthDelegate(DqrUserXapiAuthorization.class)
    public ResponseEntity<PacsPing> pingPacs(@ApiParam(value = "ID of the pacs to ping", required = true) @PathVariable("id") final String id) {
        final Long pacsId     = Long.valueOf(id);
        final Pacs pacs       = _pacsEntityService.retrieve(pacsId);
        Date       time       = new Date();
        boolean    canConnect = _pacsService.canConnect(getSessionUser(), pacs);
        PacsPing   ping       = new PacsPing();
        ping.setPacsId(pacsId);
        ping.setSuccessful(canConnect);
        ping.setPingTime(time);
        _pacsPingService.create(ping);
        return new ResponseEntity<>(ping, HttpStatus.OK);
    }



    @ApiOperation(value = "Get information about the last time the PACS was pinged.", notes = "The last ping for PACS function returns information about the last time the PACS with supplied ID was pinged.", response = PacsPing.class)
    @ApiResponses({@ApiResponse(code = 200, message = "A PACS ping."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the PACS ping."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "pacsStatus/lastPing/{id}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public ResponseEntity<PacsPing> lastPingForPacs(@ApiParam(value = "ID of the PACS", required = true) @PathVariable("id") final String id) {
        final Long pacsId = Long.valueOf(id);
        return new ResponseEntity<>(_pacsPingService.getLatestPing(pacsId), HttpStatus.OK);
    }



    @ApiOperation(value = "Get information about the times the PACS was pinged.", notes = "The all pings for PACS function returns information about all the times the PACS with supplied ID was pinged.", response = PacsPing.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "A list of PACS pings."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the PACS pings."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "pacsStatus/allPings/{id}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public ResponseEntity<List<PacsPing>> allPingsForPacs(@ApiParam(value = "ID of the PACS", required = true) @PathVariable("id") final String id) {
        final Long pacsId = Long.valueOf(id);
        return new ResponseEntity<>(_pacsPingService.getPings(pacsId), HttpStatus.OK);
    }



    @ApiOperation(value = "Get list of all ormStrategies.", notes = "Returns list of the names of all the OrmStrategies beans.", response = String.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "A list of ormStrategies."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access ormStrategies."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "ormStrategies", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public ResponseEntity<List<String>> getOrmStrategies() {
        Map<String, OrmStrategy> strategyMap = XDAT.getContextService().getBeansOfType(OrmStrategy.class);
        List<String>             strategies  = new ArrayList<>();
        for (Map.Entry<String, OrmStrategy> strategy : strategyMap.entrySet()) {
            strategies.add(strategy.getKey());
        }
        return new ResponseEntity<>(strategies, HttpStatus.OK);
    }



    @ApiOperation(value = "Returns the full map of DQR settings for this XNAT application.", notes = "Complex objects may be returned as encapsulated JSON strings.", response = String.class, responseContainer = "Map")
    @ApiResponses({@ApiResponse(code = 200, message = "DQR settings successfully retrieved."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Insufficient privileges to retrieve the requested setting."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public ResponseEntity<Map<String, Object>> getAllDqrPreferences() {
        log.info("User {} requested the system DQR settings.", getSessionUser().getUsername());
        final Map<String, Object> map = new HashMap<>(_preferences);
        return new ResponseEntity<>(map, HttpStatus.OK);
    }



    @ApiOperation(value = "Sets a map of DQR properties.", notes = "Sets the DQR properties specified in the map.")
    @ApiResponses({@ApiResponse(code = 200, message = "Automation properties successfully set."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to set automation properties."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.APPLICATION_JSON_VALUE}, method = RequestMethod.POST, restrictTo = Admin)
    public ResponseEntity<Void> setBatchDqrPreferences(@ApiParam(value = "The map of DQR preferences to be set.", required = true) @RequestBody final Map<String, String> properties) {
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

        return new ResponseEntity<>(HttpStatus.OK);
    }



    @ApiOperation(value = "Returns whether project is a project that has been configured to use Dqr.", response = Boolean.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Whether the project is a Dqr project."),
            @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
            @ApiResponse(code = 403, message = "You do not have sufficient permissions to check whether the project is a Dqr project."),
            @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "isDqrProject/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Read)
    public ResponseEntity<Boolean> isDqrProject(@PathVariable("projectId") @Project final String projectId) {
        return new ResponseEntity<>(_preferences.getAllowAllProjectsToUseDqr() || _adminSettingsForProjectService.isDqrEnabledForProject(projectId), HttpStatus.OK);
    }



    @ApiOperation(value = "Get stored IRB number for project.", response = String.class)
    @ApiResponses({@ApiResponse(code = 200, message = "An IRB number."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the project's IRB number."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "projectSettings/{projectId}/irbNumber", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Delete)
    public ResponseEntity<String> getIrbNumber(@PathVariable("projectId") @Project final String projectId) {
        return new ResponseEntity<>(_projectIrbInfoEntityService.findIrbNumberForProject(projectId), HttpStatus.OK);
    }



    @ApiOperation(value = "Get stored IRB file for project.", response = Object.class)
    @ApiResponses({@ApiResponse(code = 200, message = "An IRB file."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the project's IRB file."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "projectSettings/{projectId}/irbFile/{passedFileName}", method = RequestMethod.GET, restrictTo = Delete)
    @ResponseBody
    public ResponseEntity<ByteArrayResource> getIrbFile(@PathVariable("projectId") @Project final String projectId) throws IOException {
        //Filename is included in the URL to avoid confusing some browsers (even though it's unused).

        final ProjectIrbInfo info = _projectIrbInfoEntityService.findIrbInfoForProject(projectId);
        if (info == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        final byte[] bytes    = info.getIrbFile();
        final String fileName = info.getIrbFileName();

        final String mimeType;
        if (StringUtils.endsWith(fileName, ".pdf")) {
            mimeType = MediaType.APPLICATION_PDF_VALUE;
        } else {
            try (final InputStream is = new BufferedInputStream(new ByteArrayInputStream(bytes))) {
                mimeType = URLConnection.guessContentTypeFromStream(is);
            }
        }

        return ResponseEntity.ok()
                             .header(HttpHeaders.CONTENT_TYPE, mimeType)
                             .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                             .body(new ByteArrayResource(bytes));
    }



    @ApiOperation(value = "Get stored IRB filename for project.", response = String.class)
    @ApiResponses({@ApiResponse(code = 200, message = "An IRB filename."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the project's IRB filename."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "projectSettings/{projectId}/irbFilename", produces = MediaType.TEXT_PLAIN_VALUE, method = RequestMethod.GET, restrictTo = Delete)
    @ResponseBody
    public ResponseEntity<String> getIrbFilename(@PathVariable("projectId") @Project final String projectId) {
        ProjectIrbInfo info = _projectIrbInfoEntityService.findIrbInfoForProject(projectId);
        if (info == null) {
            return new ResponseEntity<>("", HttpStatus.OK);
        } else {
            return new ResponseEntity<>(info.getIrbFileName(), HttpStatus.OK);
        }
    }



    @ApiOperation(value = "Update IRB number for project.", response = Boolean.class)
    @ApiResponses({@ApiResponse(code = 200, message = "IRB number updated."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to modify the project's IRB number."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "projectSettings/{projectId}/irbNumber", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.PUT, restrictTo = Delete)
    public ResponseEntity<Boolean> putIrbNumber(@PathVariable("projectId") @Project final String projectId,
                                                @ApiParam("IRB number for this project.") @RequestParam(name = "irbNumber") final String irbNumber) {
        ProjectIrbInfo info = _projectIrbInfoEntityService.findIrbInfoForProject(projectId);
        if (info != null) {
            info.setIrbNumber(irbNumber);
            _projectIrbInfoEntityService.update(info);
            if (info.getIrbFile() != null) {
                notifyAdminOfCompleteIrbInfo(projectId, info, getSessionUser());
            }
        } else {
            //Create new IRB info object
            final ProjectIrbInfo projectIrbInfo = new ProjectIrbInfo();
            projectIrbInfo.setProjectId(projectId);
            projectIrbInfo.setIrbNumber(irbNumber);
            _projectIrbInfoEntityService.create(projectIrbInfo);
        }
        return new ResponseEntity<>(true, HttpStatus.OK);
    }



    @ApiOperation(value = "Update IRB file for project.", response = Boolean.class)
    @ApiResponses({@ApiResponse(code = 200, message = "IRB file updated."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to modify the project's IRB file."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "projectSettings/{projectId}/irbFile", method = RequestMethod.PUT, restrictTo = Delete)
    public ResponseEntity<Boolean> putIrbFile(@ApiParam(value = "Multipart file object being uploaded") @RequestParam(value = "irbFile") MultipartFile irbFile,
                                              @PathVariable("projectId") @Project final String projectId) {
        try {
            String         fileName = irbFile.getOriginalFilename();
            byte[]         bytes    = irbFile.getBytes();
            ProjectIrbInfo info     = _projectIrbInfoEntityService.findIrbInfoForProject(projectId);
            if (info != null) {
                info.setIrbFileName(fileName);
                info.setIrbFile(bytes);
                _projectIrbInfoEntityService.update(info);
                if (info.getIrbNumber() != null) {
                    notifyAdminOfCompleteIrbInfo(projectId, info, getSessionUser());
                }
            } else {
                //Create new IRB info object
                final ProjectIrbInfo projectIrbInfo = new ProjectIrbInfo();
                projectIrbInfo.setProjectId(projectId);
                projectIrbInfo.setIrbFileName(fileName);
                projectIrbInfo.setIrbFile(bytes);
                _projectIrbInfoEntityService.create(projectIrbInfo);
            }
        } catch (IOException e) {
            log.error("IO exception when updating IRB file.", e);
            return new ResponseEntity<>(false, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(true, HttpStatus.OK);
    }



    @ApiOperation(value = "Deletes the stored IRB file for project.", response = Boolean.class)
    @ApiResponses({@ApiResponse(code = 200, message = "IRB file for the project was successfully removed."),
            @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
            @ApiResponse(code = 403, message = "Insufficient privileges to delete the IRB file for the project."),
            @ApiResponse(code = 404, message = "The requested IRB file for the project wasn't found."),
            @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @XapiRequestMapping(value = "projectSettings/{projectId}/irbFile", method = RequestMethod.DELETE, restrictTo = Delete)
    @ResponseBody
    public ResponseEntity<Boolean> deleteIrbFile(@PathVariable("projectId") @Project final String projectId) throws IOException {
        final ProjectIrbInfo info = _projectIrbInfoEntityService.findIrbInfoForProject(projectId);
        if (info == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        _projectIrbInfoEntityService.delete(info);

        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    private void notifyAdminOfCompleteIrbInfo(final String projectId, final ProjectIrbInfo info, final UserI user) {
        try {
            String adminEmail = XDAT.getSiteConfigPreferences().getAdminEmail();
            XDAT.getMailService().sendMessage(adminEmail, new String[]{XDAT.getSiteConfigPreferences().getAdminEmail()}, new String[]{},
                                              String.format("[ %s ] Project IRB Info Stored for %s", TurbineUtils.GetSystemName(), projectId),
                                              String.format("IRB info (containing IRB number %s) has been stored for project %s by user %s. You can review this info by going to the project's Project Settings page. If this IRB info is acceptable, you can add the project to the list of projects that are permitted to use DQR in Plugin Settings.", info.getIrbNumber(), projectId, user.getUsername()));
        } catch (final Exception e) {
            log.error(String.format("User %s saved IRB info for project %s but there was an error notifying the admin.", user.getUsername(),
                                    projectId));
        }
    }



    @ApiOperation(value = "Creates a new Dqr configuration for a project from the submitted attributes.", notes = "Returns the newly created Dqr configuration for a project with the submitted attributes.", response = DqrAdminSettingsForProject.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Returns the newly created Dqr configuration for the project."),
                   @ApiResponse(code = 403, message = "Insufficient privileges to create the Dqr configuration for the project."),
                   @ApiResponse(code = 404, message = "The requested Dqr configuration for the project wasn't found."),
                   @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @XapiRequestMapping(value = "adminProjectSettings", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST, restrictTo = Admin)
    @ResponseBody
    public ResponseEntity<DqrAdminSettingsForProject> createDqrAdminSettingsForProject(@RequestBody final DqrAdminSettingsForProject settings) {
        if (StringUtils.isBlank(settings.getProjectId())) {
            log.error("User {} tried to configure Dqr settings for a project without specifying a project.", getSessionUser().getUsername());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        else{
            DqrAdminSettingsForProject existingSettings = _adminSettingsForProjectService.findSettingsByProject(settings.getProjectId());
            if(existingSettings!=null) {
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
                if (isDirty) {
                    _adminSettingsForProjectService.update(existingSettings);
                    return new ResponseEntity<>(existingSettings, HttpStatus.OK);
                }
                return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
            }
            else{
                DqrAdminSettingsForProject created = _adminSettingsForProjectService.create(settings);
                return new ResponseEntity<>(created, HttpStatus.OK);
            }
        }
    }



    @ApiOperation(value = "Deletes the requested Dqr configuration for the project.", response = Boolean.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Dqr configuration for the project was successfully removed."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Insufficient privileges to delete the Dqr configuration for the project."),
                   @ApiResponse(code = 404, message = "The requested Dqr configuration for the project wasn't found."),
                   @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @XapiRequestMapping(value = "adminProjectSettings/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.DELETE, restrictTo = Admin)
    @ResponseBody
    public ResponseEntity<Boolean> deleteDqrAdminSettingsForProject(@PathVariable("projectId") final String projectId) {
        DqrAdminSettingsForProject existingSettings = _adminSettingsForProjectService.findSettingsByProject(projectId);
        if (existingSettings==null) {
            return new ResponseEntity<>(false, HttpStatus.NOT_FOUND);
        }
        try {
            _adminSettingsForProjectService.delete(existingSettings.getId());
            return new ResponseEntity<>(true, HttpStatus.OK);
        } catch (Throwable t) {
            log.error("An error occurred deleting the Dqr configuration for the project {}", projectId, t);
            return new ResponseEntity<>(false, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }



    @ApiOperation(value = "Get list of Dqr configurations.", notes = "The Dqr configurations function returns a list of all Dqr configurations in the XNAT system.", response = DqrAdminSettingsForProject.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "Returns a list of all of the currently configured Dqr configurations."),
                   @ApiResponse(code = 500, message = "An unexpected or unknown error occurred")})
    @XapiRequestMapping(value = "adminProjectSettings", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    @ResponseBody
    public ResponseEntity<List<DqrAdminSettingsForProject>> getAllDqrAdminSettingsForProjects() {
        return new ResponseEntity<>(_adminSettingsForProjectService.getAll(), HttpStatus.OK);
    }



    @ApiOperation(value = "Get Dqr configuration for the specified project.", notes = "The get Dqr configuration function returns the Dqr configuration for the specified project.", response = DqrAdminSettingsForProject.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Returns Dqr configuration for the project."),
                   @ApiResponse(code = 500, message = "An unexpected or unknown error occurred")})
    @XapiRequestMapping(value = "adminProjectSettings/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    @ResponseBody
    public ResponseEntity<DqrAdminSettingsForProject> getDqrAdminSettingForProject(@PathVariable("projectId") final String projectId) {
        return new ResponseEntity<>(_adminSettingsForProjectService.findSettingsByProject(projectId), HttpStatus.OK);
    }



    @ApiOperation(value = "Creates a new PACS availability interval from the submitted attributes.", notes = "Returns the newly created PACS availability interval with the submitted attributes.", response = PacsAvailability.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Returns the newly created PACS availability interval."),
            @ApiResponse(code = 403, message = "Insufficient privileges to create the PACS availability interval."),
            @ApiResponse(code = 404, message = "The requested PACS availability interval wasn't found."),
            @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @XapiRequestMapping(value = "pacsAvailability/window", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST, restrictTo = Admin)
    @ResponseBody
    public ResponseEntity<PacsAvailability> createPacsAvailabilityInterval(@RequestBody final PacsAvailability settings) throws Exception {
        if (settings.getDayOfWeek()==0 || StringUtils.isBlank(settings.getAvailabilityStart()) || StringUtils.isBlank(settings.getAvailabilityEnd())) {
            log.error("User {} tried to create a PACS availability interval but did not supply the day of week, start time, and end time.", getSessionUser().getUsername());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        _pacsAvailabilityEntityService.checkOverlap(settings, true);
        if(settings.getUtilizationPercent()==0 || settings.getThreads()==0){
            settings.setUtilizationPercent(0);
            settings.setThreads(0);
        }
        PacsAvailability created = _pacsAvailabilityEntityService.create(settings);
        return new ResponseEntity<>(created, HttpStatus.OK);
    }



    @ApiOperation(value = "Checks whether a new PACS availability interval would overlap with any existing intervals.", notes = "Returns whether the posted PACS availability interval would overlap with any existing intervals.", response = Boolean.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Returns whether there is overlap with an existing interval."),
            @ApiResponse(code = 400, message = "Interval not fully specified."),
            @ApiResponse(code = 403, message = "Insufficient privileges to check interval overlap."),
            @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @XapiRequestMapping(value = "pacsAvailability/conflictsExisting", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST, restrictTo = Admin)
    @ResponseBody
    public ResponseEntity<Boolean> checkPacsAvailabilityInterval(@RequestBody final PacsAvailability settings) throws Exception {
        if (settings.getDayOfWeek()==0 || StringUtils.isBlank(settings.getAvailabilityStart()) || StringUtils.isBlank(settings.getAvailabilityEnd())) {
            log.error("User {} check overlap for a PACS availability interval but did not supply the day of week, start time, and end time.", getSessionUser().getUsername());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(_pacsAvailabilityEntityService.checkOverlap(settings, false), HttpStatus.OK);
    }



    @ApiOperation(value = "Updates the requested PACS availability interval using the submitted attributes.", notes = "Returns the updated PACS availability interval.", response = PacsAvailability.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Returns the updated PACS availability interval."),
            @ApiResponse(code = 304, message = "The requested PACS availability interval is the same as the submitted PACS availability interval."),
            @ApiResponse(code = 403, message = "Insufficient privileges to edit the requested PACS availability interval."),
            @ApiResponse(code = 404, message = "The requested PACS availability interval wasn't found."),
            @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @XapiRequestMapping(value = "pacsAvailability/window/{pacsAvailabilityId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.PUT, restrictTo = Admin)
    @ResponseBody
    public ResponseEntity<PacsAvailability> updatePacsAvailabilityInterval(@PathVariable("pacsAvailabilityId") final String pacsAvailabilityId, @RequestBody final PacsAvailability settings) throws Exception {
        PacsAvailability existingSettings = _pacsAvailabilityEntityService.get(Long.parseLong(pacsAvailabilityId));
        if (existingSettings == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        boolean isDirty = false;
        // Only update fields that are actually included in the submitted data and differ from the original source.
        if (settings.getDayOfWeek()!=0 && settings.getDayOfWeek()!=existingSettings.getDayOfWeek()) {
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
        if(settings.getUtilizationPercent()==0 || settings.getThreads()==0){
            settings.setUtilizationPercent(0);
            settings.setThreads(0);
        }
        if (settings.getUtilizationPercent()!=existingSettings.getUtilizationPercent()) {
            existingSettings.setUtilizationPercent(settings.getUtilizationPercent());
            isDirty = true;
        }
        if (settings.getThreads()!=existingSettings.getThreads()) {
            existingSettings.setThreads(settings.getThreads());
            isDirty = true;
        }
        if (settings.getPacsId()!=existingSettings.getPacsId()) {
            existingSettings.setPacsId(settings.getPacsId());
            isDirty = true;
        }

        if (isDirty) {
            _pacsAvailabilityEntityService.checkOverlap(settings, true, existingSettings.getId());
            _pacsAvailabilityEntityService.update(existingSettings);
            return new ResponseEntity<>(existingSettings, HttpStatus.OK);
        }

        return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
    }



    @ApiOperation(value = "Deletes the requested PACS availability interval.", response = Boolean.class)
    @ApiResponses({@ApiResponse(code = 200, message = "PACS availability interval was successfully removed."),
            @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
            @ApiResponse(code = 403, message = "Insufficient privileges to delete the PACS availability interval."),
            @ApiResponse(code = 404, message = "The requested PACS availability interval wasn't found."),
            @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @XapiRequestMapping(value = "pacsAvailability/window/{pacsAvailabilityId}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.DELETE, restrictTo = Admin)
    @ResponseBody
    public ResponseEntity<Boolean> deletePacsAvailabilityInterval(@PathVariable("pacsAvailabilityId") final String pacsAvailabilityId) throws Exception {
        PacsAvailability existingSettings = _pacsAvailabilityEntityService.get(Long.parseLong(pacsAvailabilityId));
        if (existingSettings == null) {
            return new ResponseEntity<>(false, HttpStatus.NOT_FOUND);
        }
        try{
            _pacsAvailabilityEntityService.delete(existingSettings.getId());
            return new ResponseEntity<>(true, HttpStatus.OK);
        }
        catch(Throwable t){
            log.error("An error occurred deleting the PACS availability interval with id " + pacsAvailabilityId, t);
            return new ResponseEntity<>(false, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }



    @ApiOperation(value = "Get PACS availability interval with the specified ID.", notes = "The get PACS availability interval function returns the PACS availability intervals with the specified ID.", response = PacsAvailability.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Returns PACS availability interval."),
            @ApiResponse(code = 500, message = "An unexpected or unknown error occurred")})
    @XapiRequestMapping(value = "pacsAvailability/window/{pacsAvailabilityId}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    @ResponseBody
    public ResponseEntity<PacsAvailability> getPacsAvailabilityInterval(@PathVariable("pacsAvailabilityId") final String pacsAvailabilityId) throws Exception {
        return new ResponseEntity<>(_pacsAvailabilityEntityService.get(Long.parseLong(pacsAvailabilityId)), HttpStatus.OK);
    }



    @ApiOperation(value = "Get PACS availability intervals for the specified PACS.", notes = "The get PACS availability intervals function returns the PACS availability intervals for the specified PACS.", response = PacsAvailability.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "Returns PACS availability intervals for the PACS."),
            @ApiResponse(code = 500, message = "An unexpected or unknown error occurred")})
    @XapiRequestMapping(value = "pacsAvailability/windows/{pacsId}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    @ResponseBody
    public ResponseEntity<List<PacsAvailability>> getPacsAvailabilityIntervals(@PathVariable("pacsId") final String pacsId) {
        return new ResponseEntity<>(_pacsAvailabilityEntityService.findSettingsByPacs(Long.parseLong(pacsId)), HttpStatus.OK);
    }



    @ApiOperation(value = "Get PACS availability intervals by day for the specified PACS.", notes = "The get PACS availability intervals by day function returns the PACS availability intervals for the specified PACS.", response = PacsAvailability.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "Returns PACS availability intervals by day for the PACS."),
            @ApiResponse(code = 500, message = "An unexpected or unknown error occurred")})
    @XapiRequestMapping(value = "pacsAvailability/windowsByDay/{pacsId}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    @ResponseBody
    public ResponseEntity<Map<Integer, List<PacsAvailability>>> getPacsAvailabilityIntervalsByDay(@PathVariable("pacsId") final String pacsId) {
        return new ResponseEntity<>(_pacsAvailabilityEntityService.findSettingsByPacsGroupedByDay(Long.parseLong(pacsId)), HttpStatus.OK);
    }



    @ApiOperation(value = "Get list of the series in a list of studies.", notes = "The get series function returns a list of the series in the listed studies.", response = String.class, responseContainer = "Map")
    @ApiResponses({@ApiResponse(code = 200, message = "A queued DICOM query request."),
            @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
            @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the series."),
            @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @AuthDelegate(DqrUserXapiAuthorization.class)
    @XapiRequestMapping(value = "seriesInfo/pacs/{pacsId}/studies", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST, restrictTo = Authorizer)
    public ResponseEntity<Map<String,PacsSearchResults<String, Series>>> getSeries(@ApiParam(value = "ID of the pacs to query", required = true) @PathVariable("pacsId") final String pacsId,
                                                  @ApiParam("List of studies to get series for.") @RequestBody final String studyUids) {
        Map<String,PacsSearchResults<String, Series>> seriesMap = new HashMap<>();
        final UserI user = getSessionUser();
        final String[] studiesArray = StringUtils.trimToEmpty(studyUids).split("\\s*,\\s*");
        if(studiesArray!=null && pacsId!=null){
            final Long pacsIdLong = Long.valueOf(pacsId);
            final Pacs pacs       = _pacsEntityService.retrieve(pacsIdLong);
            String studiesString = "";
//            for(String studyUid:studiesArray) {
//                studiesString+="\\"+studyUid;
//            }
//            studiesString = studiesString.substring(1);
//            PacsSearchResults<String, Series> results = _pacsService.getSeriesByStudyUid(user, pacs, studiesString);
//            if(results!=null){
//                seriesMap.put(studiesString,results);
//            }
            for(String studyUid:studiesArray) {
                studiesString=studyUid;
                PacsSearchResults<String, Series> results = _pacsService.getSeriesByStudyUid(user, pacs, studiesString);
                if(results!=null){
                    seriesMap.put(studiesString,results);
                }
            }
        }
        return new ResponseEntity<>(seriesMap, HttpStatus.OK);
    }

    private final PacsService                       _pacsService;
    private final PacsEntityService                 _pacsEntityService;
    private final ProjectIrbInfoEntityService       _projectIrbInfoEntityService;
    private final PacsPingService                   _pacsPingService;
    private final ExecutedPacsRequestService        _executedRequestService;
    private final QueuedPacsRequestService          _queuedRequestService;
    private final DqrAdminSettingsForProjectService _adminSettingsForProjectService;
    private final DqrPreferences                    _preferences;
    private final PacsAvailabilityEntityService     _pacsAvailabilityEntityService;
}
