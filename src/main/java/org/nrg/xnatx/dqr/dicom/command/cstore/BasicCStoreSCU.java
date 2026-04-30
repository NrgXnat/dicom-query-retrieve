/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.command.cstore.BasicCStoreSCU
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.command.cstore;

import java.io.File;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.TransferCapability;
import org.nrg.dcm.DicomSender;
import org.nrg.dcm.io.TransferCapabilityExtractor;
import org.nrg.dicomtools.builders.NetworkApplicationEntityBuilder;
import org.nrg.dicomtools.builders.NetworkConnectionBuilder;
import org.nrg.dicomtools.utilities.DicomUtils;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatResource;
import org.nrg.xdat.om.base.BaseXnatExperimentdata.UnknownPrimaryProjectException;
import org.nrg.xnatx.dqr.dicom.command.cecho.CEchoSCU;
import org.nrg.xnatx.dqr.dicom.command.cecho.dcm4che3.Dcm4che3CEchoSCU;
import org.nrg.xnatx.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;

@Slf4j
public class BasicCStoreSCU implements CStoreSCU {
    public BasicCStoreSCU(final DqrPreferences preferences, final DicomConnectionProperties dicomConnectionProperties) {
        _preferences = preferences;
        _dicomConnectionProperties = dicomConnectionProperties;
        _cechoSCU = new Dcm4che3CEchoSCU(preferences, dicomConnectionProperties);
    }

    @Override
    public CStoreResults cstoreSeries(final XnatImagescandata series) throws CStoreFailureException {
        final List<File> dicomFiles = getDicomFilesForSeries(series);
        return cStoreFiles(dicomFiles);
    }

    CStoreResults cStoreFiles(List<File> dicomFiles) {
        _cechoSCU.cecho();
        final DicomSender   dicomSender = buildSender(dicomFiles);
        final CStoreResults results     = new CStoreResults();
        try {
            for (final File file : dicomFiles) {
                try {
                    final Attributes dicomObject = DicomUtils.read(file);
                    dicomSender.send(dicomObject);
                    if (log.isDebugEnabled()) {
                        log.debug("Successfully sent DICOM object from file {}", file.getAbsolutePath());
                    }
                    results.addSuccess(new CStoreResults.CStoreSuccess(file.getAbsolutePath()));
                } catch (Exception e) {
                    log.warn("Failed sending DICOM object from file {}", file.getAbsolutePath(), e);
                    results.addFailure(new CStoreResults.CStoreFailure(file.getAbsolutePath(), e.getMessage()));
                    throw new CStoreFailureException(e, results);
                }
            }
        } finally {
            dicomSender.close();
        }
        return results;
    }

    private List<File> getDicomFilesForSeries(final XnatImagescandata series) {
        for (final XnatAbstractresourceI resource : series.getFile()) {
            if (resource instanceof XnatResource) {
                if (((XnatResource) resource).getFormat().equals(DICOM_RESOURCE_FORMAT)) {
                    try {
                        return ((XnatResource) resource).getCorrespondingFiles(series.getImageSessionData().getArchiveRootPath());
                    } catch (UnknownPrimaryProjectException e) {
                        throw new CStoreFailureException("Could not locate the primary project for the following series:\n" + series);
                    }
                }
            }
        }
        throw new CStoreFailureException("Could not locate DICOM resources for the following series:\n" + series);
    }

    private DicomSender buildSender(final List<File> files) {
        // we need the list of files to determine the transfer capabilities,
        // because apparently some DICOM files have the transfer capability hard-coded in the DICOM tags
        // I didn't take the time to grok this fully, but we'll roll with it
        return buildSender(TransferCapabilityExtractor.getTransferCapabilities(files, "SCU"));
    }

    private DicomSender buildSender(final TransferCapability[] tcs) {
        final Connection localConnection = new NetworkConnectionBuilder().build();

        final String callingAe = StringUtils.defaultIfBlank(_preferences.getDqrCallingAe(), _dicomConnectionProperties.getLocalAeTitle());
        final ApplicationEntity localAE = new NetworkApplicationEntityBuilder()
                .setAETitle(callingAe)
                .setTransferCapability(tcs)
                .setNetworkConnection(localConnection)
                .setAssociationInitiator()
                .build();

        final Connection remoteConnection = new NetworkConnectionBuilder()
                .setHostname(_dicomConnectionProperties.getRemoteHost())
                .setPort(_dicomConnectionProperties.getRemoteStoragePort())
                .build();

        final ApplicationEntity remoteAE = new NetworkApplicationEntityBuilder()
                .setNetworkConnection(remoteConnection)
                .setAETitle(_dicomConnectionProperties.getRemoteAeTitle())
                .build();

        return new DicomSender(localAE, remoteAE);
    }

    private static final String DICOM_RESOURCE_FORMAT = "DICOM";

    private final DicomConnectionProperties _dicomConnectionProperties;
    private final DqrPreferences            _preferences;
    private final CEchoSCU                  _cechoSCU;
}
