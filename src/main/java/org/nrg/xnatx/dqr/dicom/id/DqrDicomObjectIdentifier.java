package org.nrg.xnatx.dqr.dicom.id;

import org.nrg.dcm.Extractor;
import org.nrg.dcm.id.CompositeDicomObjectIdentifier;
import org.nrg.dcm.id.DicomProjectIdentifier;

import java.util.List;

public class DqrDicomObjectIdentifier extends CompositeDicomObjectIdentifier {

    public final static String IDENTITY_TYPE_LABEL = "dqrDicomObjectIdentifier";

    public DqrDicomObjectIdentifier(final String name,
                                    final DicomProjectIdentifier projectIdentifier,
                                    final List<Extractor> subjectExtractors,
                                    final List<Extractor> sessionExtractors,
                                    final List<Extractor> attributeExtractors) {
        super( name, projectIdentifier, subjectExtractors, sessionExtractors, attributeExtractors);
        setIdentifierTypeLabel( IDENTITY_TYPE_LABEL);
    }

}
