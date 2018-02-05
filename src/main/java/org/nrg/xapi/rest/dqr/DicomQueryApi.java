package org.nrg.xapi.rest.dqr;

import io.swagger.annotations.*;
import org.h2.util.StringUtils;
import org.nrg.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.dqr.services.ExecutedPacsRequestService;
import org.nrg.dqr.services.QueuedPacsRequestService;
import org.nrg.framework.annotations.XapiRestController;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.xapi.rest.AbstractXapiRestController;
import org.nrg.xapi.rest.XapiRequestMapping;
import org.nrg.xdat.security.helpers.Roles;
import org.nrg.xdat.security.services.RoleHolder;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xft.security.UserI;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

import static org.nrg.xdat.security.helpers.AccessLevel.Admin;
import static org.nrg.xdat.security.helpers.AccessLevel.Authenticated;
import static org.nrg.xdat.security.helpers.AccessLevel.User;

/**
 * Created by mike on 1/19/18.
 */
@Api(description = "Dicom Query API")
@XapiRestController
@RequestMapping(value = "/dqr")
public class DicomQueryApi extends AbstractXapiRestController {

    protected DicomQueryApi(UserManagementServiceI userManagementService, RoleHolder roleHolder, ExecutedPacsRequestService requestService, QueuedPacsRequestService queuedRequestService) {
        super(userManagementService, roleHolder);
        _executedRequestService = requestService;
        _queuedRequestService = queuedRequestService;
    }

    @ApiOperation(value = "Get list of all DICOM query requests.", notes = "The DICOM query history function returns a list of all DICOM queries that have ever been made on the XNAT system with brief information about each.", response = ExecutedPacsRequest.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "A list of DICOM query requests."),
            @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
            @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the list of DICOM query requests."),
            @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "history", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authenticated)
    public ResponseEntity<List<ExecutedPacsRequest>> queryHistoryGet() {
        final UserI user = getSessionUser();
        if(Roles.isSiteAdmin(user)){
            return new ResponseEntity<>(_executedRequestService.getAll(), HttpStatus.OK);
        }
        else{
            return new ResponseEntity<>(_executedRequestService.getAllForUser(user), HttpStatus.OK);
        }
    }

    @ApiOperation(value = "Get DICOM query request by ID.", notes = "The DICOM query history function returns information about the DICOM query with a given ID.", response = ExecutedPacsRequest.class)
    @ApiResponses({@ApiResponse(code = 200, message = "A DICOM query request."),
            @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
            @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the DICOM query request."),
            @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "history/request/{id}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authenticated)
    public ResponseEntity<ExecutedPacsRequest> queryHistoryGet(@ApiParam(value = "ID of the query request to fetch", required = true) @PathVariable("id") final String id) {
        try {
            final UserI user = getSessionUser();
            if(Roles.isSiteAdmin(user)){
                return new ResponseEntity<>(_executedRequestService.get(Long.parseLong(id)), HttpStatus.OK);
            }
            else{
                return new ResponseEntity<>(_executedRequestService.getByIdForUser(Long.parseLong(id),user), HttpStatus.OK);
            }
        } catch (NotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @ApiOperation(value = "Get list of all queued DICOM query requests.", notes = "The DICOM query queue function returns a list of all DICOM queries that are currently queued on the XNAT system with brief information about each.", response = QueuedPacsRequest.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "A list of queued DICOM query requests."),
            @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
            @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the list of queued DICOM query requests."),
            @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "queue", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authenticated)
    public ResponseEntity<List<QueuedPacsRequest>> queryQueueGet() {
        final UserI user = getSessionUser();
        if(Roles.isSiteAdmin(user)){
            return new ResponseEntity<>(_queuedRequestService.getAll(), HttpStatus.OK);
        }
        else{
            return new ResponseEntity<>(_queuedRequestService.getAllForUser(user), HttpStatus.OK);
        }
    }

    @ApiOperation(value = "Get queued DICOM query request by ID.", notes = "The DICOM query queue function returns information about the queued DICOM query with a given ID.", response = QueuedPacsRequest.class)
    @ApiResponses({@ApiResponse(code = 200, message = "A queued DICOM query request."),
            @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
            @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the DICOM query request."),
            @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "queue/request/{id}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authenticated)
    public ResponseEntity<QueuedPacsRequest> queryQueueGet(@ApiParam(value = "ID of the queued query request to fetch", required = true) @PathVariable("id") final String id) {
        try {
            final UserI user = getSessionUser();
            if(Roles.isSiteAdmin(user)){
                return new ResponseEntity<>(_queuedRequestService.get(Long.parseLong(id)), HttpStatus.OK);
            }
            else{
                return new ResponseEntity<>(_queuedRequestService.getByIdForUser(Long.parseLong(id),user), HttpStatus.OK);
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
    @XapiRequestMapping(value = "queue/request/{id}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.DELETE, restrictTo = Authenticated)
    public ResponseEntity<Boolean> queryQueueDelete(@ApiParam(value = "ID of the queued query request to delete", required = true) @PathVariable("id") final String id) throws Exception {
        try {
            final UserI user = getSessionUser();
            QueuedPacsRequest req = _queuedRequestService.get(Long.parseLong(id));
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


    ExecutedPacsRequestService _executedRequestService;
    QueuedPacsRequestService _queuedRequestService;
}
