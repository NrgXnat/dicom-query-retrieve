package org.nrg.xapi.rest.dqr;

import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.h2.util.StringUtils;
import org.nrg.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.dqr.domain.entities.Pacs;
import org.nrg.dqr.domain.entities.PacsPing;
import org.nrg.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.dqr.services.*;
import org.nrg.dqr.util.CsvRow;
import org.nrg.framework.annotations.XapiRestController;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.xapi.rest.AbstractXapiRestController;
import org.nrg.xapi.rest.XapiRequestMapping;
import org.nrg.xdat.security.helpers.Roles;
import org.nrg.xdat.security.services.RoleHolder;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.restlet.extensions.PacsNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.nrg.xdat.security.helpers.AccessLevel.Admin;
import static org.nrg.xdat.security.helpers.AccessLevel.Authenticated;
import static org.nrg.xdat.security.helpers.AccessLevel.User;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

/**
 * Created by mike on 1/19/18.
 */
@Api(description = "Dicom Query Retrieve API")
@XapiRestController
@Slf4j
@RequestMapping(value = "/dqr")
public class DicomQueryRetrieveApi extends AbstractXapiRestController {

    protected DicomQueryRetrieveApi(UserManagementServiceI userManagementService, RoleHolder roleHolder, ExecutedPacsRequestService requestService, QueuedPacsRequestService queuedRequestService, PacsService pacsService, PacsEntityService pacsEntityService, PacsPingService pacsPingService) {
        super(userManagementService, roleHolder);
        _executedRequestService = requestService;
        _queuedRequestService = queuedRequestService;
        _pacsService = pacsService;
        _pacsEntityService = pacsEntityService;
        _pacsPingService = pacsPingService;
    }

    @ApiOperation(value = "Get list of all DICOM query requests.", notes = "The DICOM query history function returns a list of all DICOM queries that have ever been made on the XNAT system with brief information about each.", response = ExecutedPacsRequest.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "A list of DICOM query requests."),
            @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
            @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the list of DICOM query requests."),
            @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "query/history", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authenticated)
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
    @XapiRequestMapping(value = "query/history/request/{id}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authenticated)
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
    @XapiRequestMapping(value = "query/queue", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authenticated)
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
    @XapiRequestMapping(value = "query/queue/request/{id}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authenticated)
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
    @XapiRequestMapping(value = "query/queue/request/{id}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.DELETE, restrictTo = Authenticated)
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

    @ApiOperation(value = "Uses the uploaded csv to generate JSON containing information about what would be imported if the user decides to continue.", response = String.class)
    @ApiResponses({@ApiResponse(code = 200, message = "CSV successfully uploaded and processed."), @ApiResponse(code = 400, message = "Uploaded file must be a CSV."), @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."), @ApiResponse(code = 403, message = "Not authorized to upload a CSV."), @ApiResponse(code = 404, message = "No PACS with the specified ID is configured on this system."), @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = "csvimport/uploadCsv", consumes = MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST, restrictTo = Authenticated)
    public ResponseEntity<List<CsvRow>> uploadImportCsv(@ApiParam(value = "Multipart file object being uploaded") @RequestParam(value = "csv_to_store", required = true) MultipartFile csv,
                                                        @ApiParam("Pacs to query.") @RequestParam(name = "pacsId", required = true) final Long pacsId, @ApiParam("Get all studies on PACS when a row has no search criteria.") @RequestParam(name = "allowRowThatGetsAllStudiesOnPacs", required = false) final boolean allowRowThatGetsAllStudiesOnPacs) throws Exception {
        if (!csv.getContentType().contains("csv")) {
            String error = "No valid files were uploaded. Spreadsheet file must be of type: application/csv";
            log.error(error);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        File temp = File.createTempFile("xnat", "csv");
        FileOutputStream fos = new FileOutputStream(temp);
        fos.write(csv.getBytes());
        fos.close();
        List<CsvRow> rows = null;
        try {
            rows = _pacsService.extractImportRequestFromCsv(getSessionUser(), temp, pacsId, allowRowThatGetsAllStudiesOnPacs);
        }
        catch(PacsNotFoundException e){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(rows, HttpStatus.OK);
    }

    @ApiOperation(value = "Issues the PACS import requests specified in the JSON and performs the specified remapping on the data when it comes in.", response = Boolean.class)
    @ApiResponses({@ApiResponse(code = 200, message = "PACS requests successfully issued."),
            @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
            @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = "csvimport/importFromJson",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE,
            restrictTo = Authenticated)
    public ResponseEntity<Boolean> importFromPacs(@RequestBody final CsvRow[] rows,
                                                  @ApiParam("Pacs to query.") @RequestParam(name = "pacsId", required = true) final Long pacsId,
                                                  @ApiParam("XNAT SCP receiver to send to (Must be formatted as AE_TITLE:PORT).") @RequestParam(name = "ae", required = true) final String ae,
                                                  @ApiParam("XNAT project to send to.") @RequestParam(name = "project", required = true) final String project,
                                                  @ApiParam("Force the import to happen even if requested remapping won't take place.") @RequestParam(name = "importEvenIfCustomProcessingIsOff", required = false) final boolean importEvenIfCustomProcessingIsOff) throws Exception {
        _pacsService.processSpreadsheetImportFromRows(getSessionUser(), Arrays.asList(rows), ae, project, pacsId, importEvenIfCustomProcessingIsOff);
        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    @ApiOperation(value = "Ping a PACS.", notes = "The ping PACS function returns whether the PACS was responsive.", response = PacsPing.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Whether the PACS was responsive."),
            @ApiResponse(code = 401, message = "Must be authenticated to ping PACS."),
            @ApiResponse(code = 403, message = "You do not have sufficient permissions to ping PACS."),
            @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "pacsStatus/ping/{id}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
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

    PacsService _pacsService;
    PacsEntityService _pacsEntityService;
    PacsPingService _pacsPingService;
    ExecutedPacsRequestService _executedRequestService;
    QueuedPacsRequestService _queuedRequestService;
}
