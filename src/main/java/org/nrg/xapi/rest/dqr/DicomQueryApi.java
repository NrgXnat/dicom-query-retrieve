package org.nrg.xapi.rest.dqr;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.nrg.dqr.domain.entities.PacsRequest;
import org.nrg.dqr.services.PacsRequestService;
import org.nrg.framework.annotations.XapiRestController;
import org.nrg.xapi.rest.AbstractXapiRestController;
import org.nrg.xapi.rest.XapiRequestMapping;
import org.nrg.xdat.security.services.RoleHolder;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    protected DicomQueryApi(UserManagementServiceI userManagementService, RoleHolder roleHolder, PacsRequestService requestService) {
        super(userManagementService, roleHolder);
        _requestService = requestService;
    }

    @ApiOperation(value = "Get list of all PACS requests.", notes = "The DICOM query history function returns a list of all PACS queries that have ever been made on the XNAT system with brief information about each.", response = PacsRequest.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "A list of PACS requests."),
            @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
            @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the list of PACS requests."),
            @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "history", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public ResponseEntity<List<PacsRequest>> queryHistoryGet() {
        return new ResponseEntity<>(_requestService.getAll(), HttpStatus.OK);
    }

    PacsRequestService _requestService;
}
