package org.nrg.xnatx.dqr.rest;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.nrg.xapi.exceptions.DataFormatException;
import org.nrg.xapi.exceptions.NotFoundException;
import org.nrg.xapi.rest.AbstractXapiRestController;
import org.nrg.xdat.security.services.RoleHolder;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.exceptions.PacsNotFoundException;
import org.nrg.xnatx.dqr.exceptions.PacsNotQueryableException;
import org.nrg.xnatx.dqr.exceptions.PacsNotStorableException;
import org.nrg.xnatx.dqr.services.PacsEntityService;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Getter(AccessLevel.PROTECTED)
@Accessors(prefix = "_")
public abstract class AbstractDqrRestController extends AbstractXapiRestController {
    protected AbstractDqrRestController(final PacsEntityService pacsEntityService, final NamedParameterJdbcTemplate template, final UserManagementServiceI userManagementService, final RoleHolder roleHolder) {
        super(userManagementService, roleHolder);
        _pacsEntityService = pacsEntityService;
        _template = template;
    }

    protected Pacs getPacs(final long pacsId) throws PacsNotFoundException {
        try {
            return _pacsEntityService.get(pacsId);
        } catch (org.nrg.framework.exceptions.NotFoundException e) {
            throw new PacsNotFoundException(pacsId);
        }
    }

    protected Pacs getQueryablePacs(final long pacsId) throws PacsNotFoundException, PacsNotQueryableException {
        final Pacs pacs = getPacs(pacsId);
        if (pacs.isQueryable()) {
            return pacs;
        }
        throw new PacsNotQueryableException(pacsId);
    }

    protected Pacs getStorablePacs(final long pacsId) throws PacsNotFoundException, PacsNotStorableException {
        final Pacs pacs = getPacs(pacsId);
        if (pacs.isStorable()) {
            return pacs;
        }
        throw new PacsNotStorableException(pacsId);
    }

    protected long validate(final String experimentId, final String scanId) throws NotFoundException {
        try {
            return _template.queryForObject(QUERY_IMAGE_SESSION_AND_SCAN, new MapSqlParameterSource("experimentId", experimentId).addValue("scanId", scanId), Long.class);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException(String.format(MESSAGE_SESSION_SCAN_NOT_FOUND, experimentId, scanId));
        }
    }

    protected void validate(final String experimentId) throws NotFoundException {
        if (!_template.queryForObject(QUERY_IMAGE_SESSION_EXISTS, new MapSqlParameterSource("experimentId", experimentId), Boolean.class)) {
            throw new NotFoundException(String.format(MESSAGE_SESSION_NOT_FOUND, experimentId));
        }
    }

    protected void validate(final long id, final Pacs pacs) throws DataFormatException, PacsNotFoundException {
        if (pacs.getId() == 0) {
            pacs.setId(id);
        } else if (id != pacs.getId()) {
            throw new DataFormatException(String.format(MESSAGE_PACS_ID_MISMATCH, id, pacs.getId()));
        }
        validate(id);
    }

    protected void validate(final long id) throws PacsNotFoundException {
        if (!_template.queryForObject(QUERY_PACS_EXISTS, new MapSqlParameterSource("id", id), Boolean.class)) {
            throw new PacsNotFoundException(id);
        }
    }

    private static final String QUERY_PACS_EXISTS              = "SELECT EXISTS(SELECT id FROM xhbm_pacs WHERE id = :id)";
    private static final String QUERY_IMAGE_SESSION_EXISTS     = "SELECT EXISTS(SELECT id FROM xnat_imagesessiondata WHERE id = :experimentId)";
    private static final String QUERY_IMAGE_SESSION_AND_SCAN   = "SELECT s.xnat_imagescandata_id FROM xnat_imagesessiondata i LEFT JOIN xnat_imagescandata s ON i.id = s.image_session_id WHERE i.id = :experimentId AND s.id = :scanId";
    private static final String MESSAGE_PACS_ID_MISMATCH       = "The ID for the update call \"%d\" does not match the PACS entity ID \"%d\"";
    private static final String MESSAGE_SESSION_NOT_FOUND      = "No image session with ID \"%s\" exists on this system";
    private static final String MESSAGE_SESSION_SCAN_NOT_FOUND = "No image session \"%s\" with scan ID \"%s\" exists on this system";

    private final PacsEntityService          _pacsEntityService;
    private final NamedParameterJdbcTemplate _template;
}
