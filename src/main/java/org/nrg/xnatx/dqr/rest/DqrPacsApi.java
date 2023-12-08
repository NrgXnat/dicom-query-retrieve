/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.rest.DqrPacsApi
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.nrg.framework.annotations.XapiRestController;
import org.nrg.xapi.exceptions.DataFormatException;
import org.nrg.xapi.exceptions.NotFoundException;
import org.nrg.xapi.exceptions.NotModifiedException;
import org.nrg.xapi.rest.AuthDelegate;
import org.nrg.xapi.rest.XapiRequestMapping;
import org.nrg.xdat.security.services.RoleHolder;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.domain.entities.PacsAvailability;
import org.nrg.xnatx.dqr.domain.entities.PacsPing;
import org.nrg.xnatx.dqr.dto.DicomWebPingRequest;
import org.nrg.xnatx.dqr.dto.DicomWebPingResult;
import org.nrg.xnatx.dqr.dto.PacsSettings;
import org.nrg.xnatx.dqr.exceptions.InvalidDicomWebPingRequestException;
import org.nrg.xnatx.dqr.exceptions.PacsConnectionException;
import org.nrg.xnatx.dqr.exceptions.PacsNotFoundException;
import org.nrg.xnatx.dqr.security.DqrUserXapiAuthorization;
import org.nrg.xnatx.dqr.services.DicomQueryRetrieveService;
import org.nrg.xnatx.dqr.services.PacsAvailabilityService;
import org.nrg.xnatx.dqr.services.PacsDicomWebService;
import org.nrg.xnatx.dqr.services.PacsPingService;
import org.nrg.xnatx.dqr.services.PacsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.DayOfWeek;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.nrg.xdat.security.helpers.AccessLevel.Admin;
import static org.nrg.xdat.security.helpers.AccessLevel.Authenticated;
import static org.nrg.xdat.security.helpers.AccessLevel.Authorizer;

@Api("XNAT PACS Management API")
@XapiRestController
@Slf4j
@RequestMapping(value = "/pacs")
public class DqrPacsApi extends AbstractDqrRestController {
    @Autowired
    public DqrPacsApi(
            final UserManagementServiceI userManagementService,
            final RoleHolder roleHolder,
            final DicomQueryRetrieveService dqrService,
            final PacsAvailabilityService pacsAvailabilityService,
            final PacsPingService pacsPingService,
            final PacsService pacsService,
            final NamedParameterJdbcTemplate template,
            final PacsDicomWebService pacsDicomWebService) {
        super(pacsService, template, userManagementService, roleHolder);
        _dqrService = dqrService;
        _pacsAvailabilityService = pacsAvailabilityService;
        _pacsPingService = pacsPingService;
        _pacsDicomWebService = pacsDicomWebService;
    }

    @ApiOperation(value = "Get list of all existing PACS systems.", notes = "This API call accepts two optional query-string parameters that can limit the scope of the PACS returned.", response = Pacs.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "A list of PACS configured in this system."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the list of PACS."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authenticated)
    public List<Pacs> getAllPacs(final @ApiParam("Indicates whether the results should include only PACS that allow store (C-PUT) operations.") @RequestParam(defaultValue = "false") boolean storable,
                                 final @ApiParam("Indicates whether the results should include only PACS that allow query (C-FIND) operations.") @RequestParam(defaultValue = "false") boolean queryable) {
        return getPacsService().findAll(storable, queryable);
    }

    @ApiOperation(value = "Creates a new PACS entry.", response = Pacs.class)
    @ApiResponses({@ApiResponse(code = 200, message = "New PACS entry successfully created."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to create a new PACS."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE}, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST, restrictTo = Admin)
    public Pacs createPacs(final @ApiParam("Attributes for the new PACS entry.") @RequestBody PacsSettings settings) {
        return getPacsService().create(new Pacs(settings));
    }

    @ApiOperation(value = "Retrieves an existing PACS entry.", response = Pacs.class)
    @ApiResponses({@ApiResponse(code = 200, message = "PACS entry successfully retrieved."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to retrieve the PACS entry."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "{pacsId}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authenticated)
    public Pacs retrievePacs(final @ApiParam("ID of the PACS entry to be retrieved.") @PathVariable long pacsId) throws PacsNotFoundException {
        return getPacs(pacsId);
    }

    @ApiOperation(value = "Updates an existing PACS entry.")
    @ApiResponses({@ApiResponse(code = 200, message = "PACS entry successfully updated."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to update the PACS entry."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "{pacsId}", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE}, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.PUT, restrictTo = Admin)
    public Pacs updatePacs(final @ApiParam("ID of the PACS entry to be updated.") @PathVariable long pacsId, final @ApiParam("Attributes for the updated PACS entry.") @RequestBody PacsSettings settings) throws DataFormatException, PacsNotFoundException {
        final Pacs pacs = getPacsService().retrieve(pacsId);
        pacs.copySettings(settings);
        validate(pacsId, pacs);
        getPacsService().update(pacs);
        return getPacsService().retrieve(pacsId);
    }

    @ApiOperation(value = "Deletes an existing PACS entry.")
    @ApiResponses({@ApiResponse(code = 200, message = "PACS entry successfully deleted."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to delete the PACS entry."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "{pacsId}", method = RequestMethod.DELETE, restrictTo = Admin)
    public void deletePacs(final @ApiParam("ID of the PACS entry to be deleted.") @PathVariable long pacsId) throws PacsNotFoundException {
        validate(pacsId);
        getPacsService().delete(pacsId);
    }

    @ApiOperation(value = "Ping a PACS.", notes = "The ping PACS function returns whether the PACS was responsive.", response = PacsPing.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Whether the PACS was responsive."),
                   @ApiResponse(code = 401, message = "Must be authenticated to ping PACS."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to ping PACS."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "{pacsId}/status", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Authorizer)
    @AuthDelegate(DqrUserXapiAuthorization.class)
    public PacsPing pingPacs(@ApiParam(value = "ID of the pacs to ping", required = true) @PathVariable final long pacsId) {
        final PacsPing ping = new PacsPing();
        ping.setPacsId(pacsId);
        ping.setSuccessful(_dqrService.canConnect(getSessionUser(), getPacsService().retrieve(pacsId)));
        ping.setPingTime(new Date());
        _pacsPingService.create(ping);
        return ping;
    }

    @ApiOperation(value = "Get information about the last time the PACS was pinged.", notes = "The last ping for PACS function returns information about the last time the PACS with supplied ID was pinged.", response = PacsPing.class)
    @ApiResponses({@ApiResponse(code = 200, message = "A PACS ping."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the PACS ping."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "{pacsId}/status/last", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public PacsPing lastPingForPacs(@ApiParam(value = "ID of the PACS", required = true) @PathVariable final long pacsId) {
        return _pacsPingService.getLatestPing(pacsId);
    }

    @ApiOperation(value = "Get information about the times the PACS was pinged.", notes = "The all pings for PACS function returns information about all the times the PACS with supplied ID was pinged.", response = PacsPing.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "A list of PACS pings."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "You do not have sufficient permissions to access the PACS pings."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "{pacsId}/status/all", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public List<PacsPing> allPingsForPacs(@ApiParam(value = "ID of the PACS", required = true) @PathVariable final long pacsId) {
        return _pacsPingService.getPings(pacsId);
    }

    @ApiOperation(value = "Creates a new PACS availability interval from the submitted attributes.", notes = "Returns the newly created PACS availability interval with the submitted attributes.", response = PacsAvailability.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Returns the newly created PACS availability interval."),
                   @ApiResponse(code = 403, message = "Insufficient privileges to create the PACS availability interval."),
                   @ApiResponse(code = 404, message = "The requested PACS availability interval wasn't found."),
                   @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @XapiRequestMapping(value = "{pacsId}/availability", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST, restrictTo = Admin)
    @ResponseBody
    public PacsAvailability createPacsAvailabilityInterval(@PathVariable final long pacsId, @RequestBody final PacsAvailability settings) throws DataFormatException {
        if (!validatePacsAvailabilityInterval(pacsId, settings)) {
            throw new DataFormatException("User " + getSessionUser().getUsername() + " tried to create an availability interval for PACS " + pacsId + " but the interval was invalid (probable overlap with existing interval).");
        }
        if (settings.getUtilizationPercent() == 0 || settings.getThreads() == 0) {
            settings.setUtilizationPercent(0);
            settings.setThreads(0);
        }
        return _pacsAvailabilityService.create(settings);
    }

    @ApiOperation(value = "Validates a new PACS availability interval, including checking whether it would overlap with any existing intervals.", notes = "Returns true if the posted PACS availability interval is valid and false if not (e.g. if it would overlap with any existing intervals).", response = Boolean.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Returns true if the availability interval is valid."),
                   @ApiResponse(code = 400, message = "Interval not fully specified."),
                   @ApiResponse(code = 403, message = "Insufficient privileges to check interval overlap."),
                   @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @XapiRequestMapping(value = "{pacsId}/availability/validate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST, restrictTo = Admin)
    @ResponseBody
    public boolean validatePacsAvailabilityInterval(@PathVariable final long pacsId, @RequestBody final PacsAvailability settings) throws DataFormatException {
        if (settings.getDayOfWeek().getValue() == 0 || StringUtils.isBlank(settings.getAvailabilityStart()) || StringUtils.isBlank(settings.getAvailabilityEnd())) {
            throw new DataFormatException("User " + getSessionUser().getUsername() + " tried to check overlap for a PACS availability interval but did not supply the day of week, start time, and end time.");
        }
        settings.setPacsId(pacsId);
        return !_pacsAvailabilityService.checkOverlap(settings, false);
    }

    @ApiOperation(value = "Updates the requested PACS availability interval using the submitted attributes.", notes = "Returns the updated PACS availability interval.", response = PacsAvailability.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Returns the updated PACS availability interval."),
                   @ApiResponse(code = 304, message = "The requested PACS availability interval is the same as the submitted PACS availability interval."),
                   @ApiResponse(code = 403, message = "Insufficient privileges to edit the requested PACS availability interval."),
                   @ApiResponse(code = 404, message = "The requested PACS availability interval wasn't found."),
                   @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @XapiRequestMapping(value = "{pacsId}/availability/{availabilityId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.PUT, restrictTo = Admin)
    @ResponseBody
    public PacsAvailability updatePacsAvailabilityInterval(@PathVariable final long pacsId, @PathVariable final long availabilityId, @RequestBody final PacsAvailability settings) throws Exception {
        final PacsAvailability existingSettings = getPacsAvailability(pacsId, availabilityId);

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
            throw new NotModifiedException("No changes were made to the PACS availability settings " + availabilityId);
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
    @XapiRequestMapping(value = "{pacsId}/availability/{availabilityId}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.DELETE, restrictTo = Admin)
    @ResponseBody
    public boolean deletePacsAvailabilityInterval(@PathVariable final long pacsId, @PathVariable final long availabilityId) throws NotFoundException {
        final PacsAvailability existingSettings = getPacsAvailability(pacsId, availabilityId);
        _pacsAvailabilityService.delete(existingSettings.getId());
        return true;
    }

    @ApiOperation(value = "Get PACS availability interval with the specified ID.", notes = "The get PACS availability interval function returns the PACS availability intervals with the specified ID.", response = PacsAvailability.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Returns PACS availability interval."),
                   @ApiResponse(code = 500, message = "An unexpected or unknown error occurred")})
    @XapiRequestMapping(value = "{pacsId}/availability/{availabilityId}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    @ResponseBody
    public PacsAvailability getPacsAvailability(@PathVariable final long pacsId, @PathVariable final long availabilityId) throws NotFoundException {
        try {
            final PacsAvailability existingSettings = _pacsAvailabilityService.get(availabilityId);
            if (existingSettings == null || pacsId != existingSettings.getPacsId()) {
                throw new org.nrg.framework.exceptions.NotFoundException("");
            }
            return existingSettings;
        } catch (org.nrg.framework.exceptions.NotFoundException e) {
            throw new NotFoundException("No PACS availability entry with ID " + availabilityId + " exists for PACS " + pacsId);
        }
    }

    @ApiOperation(value = "Get PACS availability intervals for the specified PACS.", notes = "The PACS availability intervals for the specified PACS are grouped by day of the week.", response = PacsAvailability.class, responseContainer = "Map")
    @ApiResponses({@ApiResponse(code = 200, message = "Returns PACS availability intervals by day for the PACS."),
                   @ApiResponse(code = 500, message = "An unexpected or unknown error occurred")})
    @XapiRequestMapping(value = "{pacsId}/availability", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    @ResponseBody
    public Map<DayOfWeek, List<PacsAvailability>> getPacsAvailabilityIntervalsByDay(@PathVariable final long pacsId) {
        return _pacsAvailabilityService.findAllByPacsIdGroupedByDayOfWeek(pacsId);
    }

    @ApiOperation(value = "Check the dicomWeb status.", response = DicomWebPingResult.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Whether the DicomWeb was responsive."),
            @ApiResponse(code = 401, message = "Pacs needs to be authenticated."),
            @ApiResponse(code = 404, message = "HTTP host connection error or URL not found."),
            @ApiResponse(code = 403, message = "You do not have sufficient permissions to check the status DicomWeb."),
            @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "/dicom-web/status", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST, restrictTo = Admin)
    public DicomWebPingResult dicomWebPing(@RequestBody DicomWebPingRequest pingRequest) throws InvalidDicomWebPingRequestException {
        return _pacsDicomWebService.ping(pingRequest);
    }

    private final DicomQueryRetrieveService _dqrService;
    private final PacsPingService           _pacsPingService;
    private final PacsAvailabilityService _pacsAvailabilityService;
    private final PacsDicomWebService _pacsDicomWebService;
}
