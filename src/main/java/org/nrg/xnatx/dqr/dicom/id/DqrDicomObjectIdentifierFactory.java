package org.nrg.xnatx.dqr.dicom.id;

import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.dcm4che2.data.Tag;
import org.nrg.dcm.Extractor;
import org.nrg.dcm.TextExtractor;
import org.nrg.dcm.id.DicomObjectIdentifierFactory;
import org.nrg.dcm.id.DicomProjectIdentifier;
import org.nrg.dcm.id.RoutedStudyDicomProjectIdentifier;
import org.nrg.dcm.id.XnatDefaultDicomObjectIdentifier;
import org.nrg.dcm.scp.DicomSCPInstance;
import org.nrg.xdat.services.StudyRoutingService;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;
import org.nrg.xnatx.dqr.services.StudyIdStudyInstanceUidMappingService;

import java.util.Collections;
import java.util.List;

@Slf4j
public class DqrDicomObjectIdentifierFactory implements DicomObjectIdentifierFactory {
    private final DicomProjectIdentifier defaultProjectIdentifier;
    private final List<Extractor> defaultSubjectExtractors;
    private final List<Extractor> defaultSessionExtractors;
    private final List<Extractor> defaultAttributeExtractors;

    public DqrDicomObjectIdentifierFactory( final StudyRoutingService studyRoutingService, final StudyIdStudyInstanceUidMappingService mappingService, final DqrPreferences preferences) {
        this.defaultProjectIdentifier = new RoutedStudyDicomProjectIdentifier(studyRoutingService);
        this.defaultSubjectExtractors = Collections.singletonList(new TextExtractor(Tag.PatientName));
        this.defaultSessionExtractors = Collections.singletonList(new OverrideStudyIdExtractor(mappingService, preferences));
        this.defaultAttributeExtractors = XnatDefaultDicomObjectIdentifier.getAAExtractors();
    }

    @Override
    public DqrDicomObjectIdentifier create( String name, DicomSCPInstance instance) {
        DqrDicomObjectIdentifier doi = null;

        if( instance.isRoutingExpressionsEnabled()) {
            DicomProjectIdentifier projectIdentifier;
            String projExpression = instance.getProjectRoutingExpression();
            if ( StringUtils.isNotBlank( projExpression)) {
                Extractor extractor = XnatDefaultDicomObjectIdentifier.parseDicomRuleToExtractor(projExpression);
                // TODO: fix this.
                projectIdentifier = defaultProjectIdentifier;
            } else {
                projectIdentifier = defaultProjectIdentifier;
            }

            String subjExpression = instance.getSubjectRoutingExpression();
            ImmutableList.Builder<Extractor> listBuilder = ImmutableList.builder();
            if ( StringUtils.isNotBlank( subjExpression)) {
                listBuilder.add( createExtractor( subjExpression));
            }
            listBuilder.addAll( defaultSubjectExtractors);
            ImmutableList<Extractor> subjectExtractors = listBuilder.build();

            listBuilder = ImmutableList.builder();
            String sessExpression = instance.getSessionRoutingExpression();
            if ( StringUtils.isNotBlank( sessExpression)) {
                listBuilder.add( createExtractor( sessExpression));
            }
            listBuilder.addAll( defaultSessionExtractors);
            ImmutableList<Extractor> sessionExtractors = listBuilder.build();

            listBuilder = ImmutableList.builder();
            String aaExpression = null;
            if ( StringUtils.isNotBlank( aaExpression)) {
                listBuilder.add( createExtractor( aaExpression));
            }
            listBuilder.addAll( defaultAttributeExtractors);
            ImmutableList<Extractor> aaExtractors = listBuilder.build();

            doi = new DqrDicomObjectIdentifier(name, projectIdentifier, subjectExtractors, sessionExtractors, aaExtractors);
        }
        else {
            doi = new DqrDicomObjectIdentifier( name, defaultProjectIdentifier, defaultSubjectExtractors, defaultSessionExtractors, defaultAttributeExtractors);
        }
        return doi;
    }
    protected Extractor createExtractor( String expression) {
        try {
            Extractor extractor = XnatDefaultDicomObjectIdentifier.parseDicomRuleToExtractor( expression);
            if( extractor != null) {
                return extractor;
            }
            else {
                String msg = String.format("Failed to create extractor for routing expression '%s'", expression);
                log.error(msg);
                throw new IllegalArgumentException(msg);
            }
        }
        catch (Exception e) {
            String msg = String.format("Failed to create extractor for routing expression '%s'", expression);
            log.error(msg);
            throw new IllegalArgumentException(msg, e);
        }
    }

    @Override
    public DqrDicomObjectIdentifier create() {
        return create( "empty", DicomSCPInstance.create( "aeTitle", 1, "foo"));
    }

    @Override
    public boolean creates( DicomSCPInstance instance) {
        switch (instance.getIdentifier()) {
            case DqrDicomObjectIdentifier.IDENTITY_TYPE_LABEL:
                return true;
            default:
                return false;
        }
    }

}
