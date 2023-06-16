package org.nrg.xnatx.dqr.services.impl.hibernate;

import org.apache.commons.lang3.StringUtils;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xdat.model.XnatImagesessiondataI;
import org.nrg.xft.security.UserI;
import org.nrg.xnatx.dqr.domain.daos.SeriesRetrievalRequestDAO;
import org.nrg.xnatx.dqr.domain.entities.ArchivedRequestedSeries;
import org.nrg.xnatx.dqr.domain.entities.PaginatedPacsRequest;
import org.nrg.xnatx.dqr.domain.entities.SeriesRetrievalRequest;
import org.nrg.xnatx.dqr.services.SeriesRetrievalRequestService;
import org.nrg.xnatx.dqr.utils.OptionalString;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Service
@Transactional
public class HibernateSeriesRetrievalRequestService
        extends AbstractHibernateEntityService<SeriesRetrievalRequest, SeriesRetrievalRequestDAO>
        implements SeriesRetrievalRequestService {
    @Override
    public List<SeriesRetrievalRequest> findForArchivedSeries(
            final ArchivedRequestedSeries series,
            final @Nullable String username
    ) {
        return getDao().findByStudySeriesProjectUsername(
                series.getStudyInstanceUid(), series.getSeriesInstanceUid(), series.getXnatProject(), username
        );
    }

    @Override
    public SeriesRetrievalRequest createFromCFindResult(
            final UserI user,
            final String destinationProject,
            final DicomObject cfindResult
    ) {
        final String studyInstanceUid = cfindResult.getString(Tag.StudyInstanceUID);
        if (StringUtils.isBlank(studyInstanceUid)) {
            throw new IllegalArgumentException("cannot request series with empty Study Instance UID");
        }
        final String seriesInstanceUid = cfindResult.getString(Tag.SeriesInstanceUID);
        if (StringUtils.isBlank(seriesInstanceUid)) {
            throw new IllegalArgumentException("cannot request series with empty Series Instance UID");
        }

        final SeriesRetrievalRequest.SeriesRetrievalRequestBuilder builder = SeriesRetrievalRequest.builder()
                .requestingUser(user.getUsername())
                .destinationProject(destinationProject)
                .studyInstanceUid(studyInstanceUid)
                .seriesInstanceUid(seriesInstanceUid);

        optValueUse(cfindResult, Tag.PatientID, builder::patientId);
        optValueUse(cfindResult, Tag.StudyID, builder::studyId);
        optValueUse(cfindResult, Tag.SeriesNumber, builder::seriesNumber);
        optValueUse(cfindResult, Tag.Modality, builder::modality);
        final int nInstances = cfindResult.getInt(Tag.NumberOfSeriesRelatedInstances, -1);
        if (nInstances > 0) {
            builder.expectedInstances(nInstances);
        }

        return create(builder.build());
    }

    public boolean hasBeenRequested(final XnatImagesessiondataI session, final @Nullable UserI user) {
        final String username = null == user ? null : user.getUsername();
        return getDao().hasBeenRequested(session.getUid(), session.getProject(), username);
    }

    public List<SeriesRetrievalRequest> findReverseChronological(final @Nullable UserI user, final PaginatedPacsRequest request) {
        return getDao().findReverseChronological(null == user ? null : user.getUsername(), request);
    }

    /**
     * If the provided DicomObject contains a non-empty String value for the given tag,
     * hand that value to the provided consumer.
     * @param o DicomObject
     * @param tag tag of requested fields
     * @param consumer method to be applied to value if it exists
     */
    private void optValueUse(final DicomObject o, int tag, Consumer<? super String> consumer) {
        OptionalString.of(o.getString(tag)).ifPresent(consumer);
    }
}
