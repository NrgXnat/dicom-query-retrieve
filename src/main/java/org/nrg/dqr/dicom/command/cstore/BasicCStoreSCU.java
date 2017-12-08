/*
 * BasicCStoreSCU
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.dqr.dicom.command.cstore;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.net.*;
import org.dcm4che2.net.NetworkConnectionBuilder.TlsType;
import org.nrg.dcm.DicomSender;
import org.nrg.dicomtools.utilities.DicomUtils;
import org.nrg.dcm.io.TransferCapabilityExtractor;
import org.nrg.dqr.dicom.command.cecho.CEchoSCU;
import org.nrg.dqr.dicom.command.cecho.dcm4che.tool.Dcm4cheToolCEchoSCU;
import org.nrg.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatResource;
import org.nrg.xdat.om.base.BaseXnatExperimentdata.UnknownPrimaryProjectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.File;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class BasicCStoreSCU implements CStoreSCU {

    private static final String PROTOCOL_TLS = "TLS";
    private static final String DICOM_RESOURCE_FORMAT = "DICOM";

    private static final Logger _log = LoggerFactory.getLogger(BasicCStoreSCU.class);

    private final DicomConnectionProperties _dicomConnectionProperties;

    private final CEchoSCU _cechoSCU;

    public BasicCStoreSCU(final DicomConnectionProperties dicomConnectionProperties) {
        _dicomConnectionProperties = dicomConnectionProperties;
        _cechoSCU = new Dcm4cheToolCEchoSCU(dicomConnectionProperties);
    }

    @Override
    public CStoreResults cstoreSeries(final XnatImagescandata series) throws CStoreFailureException {
        final List<File> dicomFiles = getDicomFilesForSeries(series);
        return cStoreFiles(dicomFiles);
    }

    CStoreResults cStoreFiles(List<File> dicomFiles) {
        _cechoSCU.cecho();
        final DicomSender dicomSender = buildSender(dicomFiles);
        final CStoreResults results = new CStoreResults();
        for (final File file : dicomFiles) {
            try {
                final DicomObject dicomObject = DicomUtils.read(file);
                final String result = dicomSender.send(dicomObject, DicomUtils.getTransferSyntaxUID(dicomObject));
                if (null == result) {
                    if (_log.isDebugEnabled()) {
                        _log.debug("Successfully sent DICOM object from file {}", file.getAbsolutePath());
                    }
                    results.addSuccess(new CStoreResults.CStoreSuccess(file.getAbsolutePath()));
                } else {
                    // this is officially a warning, not an error
                    // but as I'm not sure what types of warnings we expect to get,
                    // I'd rather fail fast for now.
                    if (_log.isDebugEnabled()) {
                        _log.debug("Failed sending DICOM object from file {}:\n{}", file.getAbsolutePath(), dicomObject.toString());
                    } else if (_log.isWarnEnabled()) {
                        _log.warn("Failed sending DICOM object from file {}", file.getAbsolutePath());
                    }
                    results.addFailure(new CStoreResults.CStoreFailure(file.getAbsolutePath(), result));
                    throw new CStoreFailureException(results);
                }
            } catch (Exception e) {
                // Once we know more, we may want to soldier on if a single file fails.
                // For now, I'm going to fail fast.
                if (_log.isWarnEnabled()) {
                    _log.warn("Failed sending DICOM object from file " + file.getAbsolutePath(), e);
                }
                results.addFailure(new CStoreResults.CStoreFailure(file.getAbsolutePath(), e.getMessage()));
                throw new CStoreFailureException(e, results);
            }
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
        return buildSender(TransferCapabilityExtractor.getTransferCapabilities(files, TransferCapability.SCU));
    }

    private DicomSender buildSender(final TransferCapability[] tcs) {
        return buildSender(tcs, null, false, null);
    }

    private DicomSender buildSender(final TransferCapability[] tcs, final TlsType tlsType,
                                    final boolean needsClientAuth, final TrustManager[] trustManagers) {
        final NetworkConnection connection;
        if (null != tlsType) {
            connection = new NetworkConnectionBuilder().setTls(tlsType).setTlsNeedClientAuth(needsClientAuth).build();
        } else {
            connection = new NetworkConnection();
        }

        final NetworkApplicationEntity localAE = new NetworkApplicationEntityBuilder()
                .setAETitle(_dicomConnectionProperties.getLocalAeTitle()).setTransferCapability(tcs)
                .setNetworkConnection(connection).build();

        final NetworkConnectionBuilder builder = new NetworkConnectionBuilder();
        builder.setHostname(_dicomConnectionProperties.getRemoteHost()).setPort(
                _dicomConnectionProperties.getRemoteStoragePort());
        if (null != tlsType) {
            builder.setTls(tlsType).setTlsNeedClientAuth(needsClientAuth);
        }

        final NetworkApplicationEntity remoteAE = new NetworkApplicationEntityBuilder()
                .setNetworkConnection(builder.build()).setAETitle(_dicomConnectionProperties.getRemoteAeTitle()).build();

        final DicomSender sender = new DicomSender(localAE, remoteAE);
        if (null != tlsType) {
            try {
                final SSLContext context = SSLContext.getInstance(PROTOCOL_TLS);
                context.init(null, trustManagers, null);
                sender.setSSLContext(context);
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new RuntimeException(e); // programming error
            }
        }
        return sender;
    }
}
