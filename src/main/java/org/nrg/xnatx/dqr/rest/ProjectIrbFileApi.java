package org.nrg.xnatx.dqr.rest;

import static org.nrg.xdat.security.helpers.AccessLevel.Delete;
import static org.nrg.xdat.security.helpers.AccessLevel.Edit;

import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.nrg.framework.annotations.XapiRestController;
import org.nrg.mail.services.MailService;
import org.nrg.xapi.exceptions.InitializationException;
import org.nrg.xapi.exceptions.NotFoundException;
import org.nrg.xapi.exceptions.ResourceAlreadyExistsException;
import org.nrg.xapi.rest.AbstractXapiRestController;
import org.nrg.xapi.rest.Project;
import org.nrg.xapi.rest.XapiRequestMapping;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.services.RoleHolder;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.security.UserI;
import org.nrg.xnatx.dqr.domain.entities.ProjectIrbInfo;
import org.nrg.xnatx.dqr.services.ProjectIrbInfoEntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import springfox.documentation.annotations.ApiIgnore;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Paths;
import java.util.List;

/**
 * The project IRB file functionality is not fully supported as of the 1.0 release of the XNAT DQR plugin.
 */
@ApiIgnore
@Api(value = "XNAT Project IRB File API", hidden = true)
@XapiRestController
@Slf4j
@RequestMapping("/irb")
public class ProjectIrbFileApi extends AbstractXapiRestController {
    @Autowired
    public ProjectIrbFileApi(final UserManagementServiceI userManagementService, final RoleHolder roleHolder, final ProjectIrbInfoEntityService projectIrbInfoEntityService, final SiteConfigPreferences siteConfigPreferences, final MailService mailService) {
        super(userManagementService, roleHolder);

        _projectIrbInfoEntityService = projectIrbInfoEntityService;
        _siteConfigPreferences = siteConfigPreferences;
        _mailService = mailService;
    }

    @ApiOperation(value = "Get stored IRB number for project.", response = String.class)
    @ApiResponses({@ApiResponse(code = 200, message = "An IRB number."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the project's IRB number."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "projects/{projectId}/irbNumber", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Delete)
    public String getIrbNumber(@PathVariable("projectId") @Project final String projectId) throws NotFoundException {
        return _projectIrbInfoEntityService.findIrbNumberForProject(projectId);
    }

    @ApiOperation(value = "Get stored IRB file for project.", response = Object.class)
    @ApiResponses({@ApiResponse(code = 200, message = "An IRB file."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the project's IRB file."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "projects/{projectId}/irbFile/{fileName}", method = RequestMethod.GET, restrictTo = Delete)
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
    @XapiRequestMapping(value = "projects/{projectId}/irbFilename", produces = MediaType.TEXT_PLAIN_VALUE, method = RequestMethod.GET, restrictTo = Delete)
    @ResponseBody
    public List<String> getIrbFilenames(@PathVariable("projectId") @Project final String projectId) throws NotFoundException {
        return _projectIrbInfoEntityService.findIrbFileNamesForProject(projectId);
    }

    @ApiOperation(value = "Update IRB number for project.", response = Boolean.class)
    @ApiResponses({@ApiResponse(code = 200, message = "IRB number updated."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to modify the project's IRB number."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "projects/{projectId}/irbNumber", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.PUT, restrictTo = Delete)
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
    @XapiRequestMapping(value = "projects/{projectId}/irbFile", method = RequestMethod.PUT, restrictTo = Edit)
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
    @XapiRequestMapping(value = "projects/{projectId}/irbFile", method = RequestMethod.DELETE, restrictTo = Delete)
    @ResponseBody
    public boolean deleteIrbFile(@PathVariable("projectId") @Project final String projectId) throws NotFoundException {
        final ProjectIrbInfo info = _projectIrbInfoEntityService.findIrbInfoForProject(projectId);
        if (info == null) {
            throw new NotFoundException("No IRB file found for project ID " + projectId);
        }
        _projectIrbInfoEntityService.delete(info);
        return true;
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

    private final SiteConfigPreferences       _siteConfigPreferences;
    private final ProjectIrbInfoEntityService _projectIrbInfoEntityService;
    private final MailService                 _mailService;
}
