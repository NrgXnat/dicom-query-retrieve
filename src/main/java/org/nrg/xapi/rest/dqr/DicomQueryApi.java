package org.nrg.xapi.rest.dqr;

import io.swagger.annotations.*;
import org.nrg.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.dqr.services.ExecutedPacsRequestService;
import org.nrg.framework.annotations.XapiRestController;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.xapi.rest.AbstractXapiRestController;
import org.nrg.xapi.rest.XapiRequestMapping;
import org.nrg.xdat.security.services.RoleHolder;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

import static org.nrg.xdat.security.helpers.AccessLevel.Admin;

/**
 * Created by mike on 1/19/18.
 */
@Api(description = "Dicom Query API")
@XapiRestController
@RequestMapping(value = "/dqr")
public class DicomQueryApi extends AbstractXapiRestController {

    protected DicomQueryApi(UserManagementServiceI userManagementService, RoleHolder roleHolder, ExecutedPacsRequestService requestService) {
        super(userManagementService, roleHolder);
        _requestService = requestService;
    }

    @ApiOperation(value = "Get list of all DICOM query requests.", notes = "The DICOM query history function returns a list of all DICOM queries that have ever been made on the XNAT system with brief information about each.", response = ExecutedPacsRequest.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "A list of DICOM query requests."),
            @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
            @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the list of DICOM query requests."),
            @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "history", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public ResponseEntity<List<ExecutedPacsRequest>> queryHistoryGet() {
        return new ResponseEntity<>(_requestService.getAll(), HttpStatus.OK);
    }

    @ApiOperation(value = "Get DICOM query request by ID.", notes = "The DICOM query history function returns information about the DICOM query with a given ID.", response = ExecutedPacsRequest.class)
    @ApiResponses({@ApiResponse(code = 200, message = "A DICOM query request."),
            @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
            @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the DICOM query request."),
            @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "history/request/{id}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public ResponseEntity<ExecutedPacsRequest> queryHistoryGet(@ApiParam(value = "ID of the query request to fetch", required = true) @PathVariable("id") final String id) {
        try {
            return new ResponseEntity<>(_requestService.get(Long.parseLong(id)), HttpStatus.OK);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    ExecutedPacsRequestService _requestService;
}
