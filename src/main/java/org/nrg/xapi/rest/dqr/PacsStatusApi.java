package org.nrg.xapi.rest.dqr;

import io.swagger.annotations.*;
import org.h2.util.StringUtils;
import org.nrg.dqr.dicom.command.cecho.dcm4che.tool.Dcm4cheToolCEchoSCU;
import org.nrg.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.dqr.domain.entities.Pacs;
import org.nrg.dqr.domain.entities.PacsPing;
import org.nrg.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.dqr.services.*;
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

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.nrg.xdat.security.helpers.AccessLevel.Admin;
import static org.nrg.xdat.security.helpers.AccessLevel.Authenticated;

/**
 * Created by mike on 1/19/18.
 */
@Api(description = "Pacs Status API")
@XapiRestController
@RequestMapping(value = "/pacsStatus")
public class PacsStatusApi extends AbstractXapiRestController {

    PacsService _pacsService;
    PacsEntityService _pacsEntityService;
    PacsPingService _pacsPingService;

    protected PacsStatusApi(UserManagementServiceI userManagementService, RoleHolder roleHolder, PacsService pacsService, PacsEntityService pacsEntityService, PacsPingService pacsPingService) {
        super(userManagementService, roleHolder);
        _pacsService = pacsService;
        _pacsEntityService = pacsEntityService;
        _pacsPingService = pacsPingService;
    }

    @ApiOperation(value = "Ping a PACS.", notes = "The ping PACS function returns whether the PACS was responsive.", response = PacsPing.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Whether the PACS was responsive."),
            @ApiResponse(code = 401, message = "Must be authenticated to ping PACS."),
            @ApiResponse(code = 403, message = "You do not have sufficient permissions to ping PACS."),
            @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "ping/{id}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public ResponseEntity<PacsPing> pingPacs(@ApiParam(value = "ID of the pacs to ping", required = true) @PathVariable("id") final String id) {
        final Long pacsId = Long.valueOf(id);
        final Pacs pacs = _pacsEntityService.retrieve(pacsId);
        Date time = new Date();
        boolean canConnect = _pacsService.canConnect(getSessionUser(),pacs);
        PacsPing ping = new PacsPing();
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
    @XapiRequestMapping(value = "lastPing/{id}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public ResponseEntity<PacsPing> lastPingForPacs(@ApiParam(value = "ID of the PACS", required = true) @PathVariable("id") final String id) {
        final Long pacsId = Long.valueOf(id);
        return new ResponseEntity<>(_pacsPingService.getLatestPing(pacsId), HttpStatus.OK);
    }

    @ApiOperation(value = "Get information about the times the PACS was pinged.", notes = "The all pings for PACS function returns information about all the times the PACS with supplied ID was pinged.", response = PacsPing.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "A list of PACS pings."),
            @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
            @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the PACS pings."),
            @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "allPings/{id}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public ResponseEntity<List<PacsPing>> allPingsForPacs(@ApiParam(value = "ID of the PACS", required = true) @PathVariable("id") final String id) {
        final Long pacsId = Long.valueOf(id);
        return new ResponseEntity<>(_pacsPingService.getPings(pacsId), HttpStatus.OK);
    }

}
