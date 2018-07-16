package org.nrg.xapi.rest.dqr;

import io.swagger.annotations.*;
import org.apache.commons.fileupload.FileItem;
import org.h2.util.StringUtils;
import org.nrg.action.ClientException;
import org.nrg.action.ServerException;
import org.nrg.config.exceptions.ConfigServiceException;
import org.nrg.dqr.domain.Study;
import org.nrg.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.dqr.dto.PacsSearchCriteria;
import org.nrg.dqr.services.ExecutedPacsRequestService;
import org.nrg.dqr.services.PacsService;
import org.nrg.dqr.services.QueuedPacsRequestService;
import org.nrg.framework.annotations.XapiRestController;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.xapi.exceptions.NoContentException;
import org.nrg.xapi.rest.AbstractXapiRestController;
import org.nrg.xapi.rest.XapiRequestMapping;
import org.nrg.xdat.security.helpers.Roles;
import org.nrg.xdat.security.services.RoleHolder;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xdat.services.ThemeService;
import org.nrg.xft.XFT;
import org.nrg.xft.schema.Wrappers.XMLWrapper.SAXReader;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xnat.restlet.extensions.PacsNotFoundException;
import org.restlet.data.Status;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXParseException;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.nrg.xdat.security.helpers.AccessLevel.Admin;
import static org.nrg.xdat.security.helpers.AccessLevel.Authenticated;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import lombok.extern.slf4j.Slf4j;
import org.nrg.dqr.util.CsvRow;

/**
 * Created by mike on 1/19/18.
 */
@Api(description = "Import From Spreadsheet API")
@XapiRestController
@RequestMapping(value = "/csvimport")
@Slf4j
public class ImportFromSpreadsheetApi extends AbstractXapiRestController {

    protected ImportFromSpreadsheetApi(UserManagementServiceI userManagementService, RoleHolder roleHolder, PacsService service) {
        super(userManagementService, roleHolder);
        _service = service;
    }


    @ApiOperation(value = "Uses the uploaded csv to generate JSON containing information about what would be imported if the user decides to continue.", response = String.class)
    @ApiResponses({@ApiResponse(code = 200, message = "CSV successfully uploaded and processed."), @ApiResponse(code = 400, message = "Uploaded file must be a CSV."), @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."), @ApiResponse(code = 403, message = "Not authorized to upload a CSV."), @ApiResponse(code = 404, message = "No PACS with the specified ID is configured on this system."), @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = "upload", consumes = MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST, restrictTo = Authenticated)
    public ResponseEntity<List<CsvRow>> uploadImportCsv(@ApiParam(value = "Multipart file object being uploaded") @RequestParam(value = "csv_to_store", required = true) MultipartFile csv,
                                                  @ApiParam("Pacs to query.") @RequestParam(name = "pacsId", required = true) final Long pacsId) throws Exception {
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
             rows = _service.extractImportRequestFromCsv(getSessionUser(), temp, pacsId);
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
    @XapiRequestMapping(method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE,
            restrictTo = Authenticated)
    public ResponseEntity<Boolean> importFromPacs(@RequestBody final CsvRow[] rows,
                                                  @ApiParam("Pacs to query.") @RequestParam(name = "pacsId", required = true) final Long pacsId,
                                                  @ApiParam("XNAT AE to send to.") @RequestParam(name = "ae", required = true) final String ae,
                                                  @ApiParam("XNAT project to send to.") @RequestParam(name = "project", required = true) final String project) throws PacsNotFoundException, ConfigServiceException {
        _service.processSpreadsheetImportFromRows(getSessionUser(), Arrays.asList(rows), ae, project, pacsId);
        return new ResponseEntity<>(true, HttpStatus.OK);
    }


    private PacsService _service;
}
