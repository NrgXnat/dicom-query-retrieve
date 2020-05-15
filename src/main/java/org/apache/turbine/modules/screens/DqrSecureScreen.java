/*
 * dicom-query-retrieve: org.apache.turbine.modules.screens.DqrSecureScreen
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.apache.turbine.modules.screens;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.turbine.modules.actions.DqrSecureAction;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.dcm.scp.DicomSCPInstance;
import org.nrg.dcm.scp.DicomSCPManager;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xnatx.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.services.PacsEntityService;

public abstract class DqrSecureScreen extends SecureScreen {
    protected void storeProjectAndQueryablePacs(final RunData data, final Context context) {
        final List<Pacs> pacsList = getPacsEntityService().findAllQueryable();
        if (pacsList.isEmpty()) {
            data.setScreenTemplate("PacsSessionFinderNoPacsFound.vm");
        } else {
            context.put("pacsList", pacsList);
        }
        DqrSecureAction.removeDqrSessionVariables(data);
        context.put("projectId", TurbineUtils.GetPassedParameter("project", data));
    }

    protected void storeScpsAndEnabledScps(final Context context) {
        final Pair<List<DicomSCPInstance>, List<DicomSCPInstance>> pair = getScpsAndEnabledScps();
        context.put("scps", pair.getLeft());
        context.put("enabledScps", pair.getRight());
    }

    protected Pair<List<DicomSCPInstance>, List<DicomSCPInstance>> getScpsAndEnabledScps() {
        final List<DicomSCPInstance> scps        = new ArrayList<>(getDicomSCPManager().getDicomSCPInstances().values());
        final List<DicomSCPInstance> enabledScps = scps.stream().filter(DicomSCPInstance::isEnabled).collect(Collectors.toList());
        return Pair.of(scps, enabledScps);
    }

    protected Map<String, OrmStrategy> getOrmStrategyMap() {
        if (_strategies == null) {
            _strategies = XDAT.getContextService().getBeansOfType(OrmStrategy.class);
        }
        return _strategies;
    }

    protected PacsEntityService getPacsEntityService() {
        if (_pacsEntityService == null) {
            _pacsEntityService = XDAT.getContextService().getBean(PacsEntityService.class);
        }
        return _pacsEntityService;
    }

    protected DicomSCPManager getDicomSCPManager() {
        if (_dicomSCPManager == null) {
            _dicomSCPManager = XDAT.getContextService().getBean(DicomSCPManager.class);
        }
        return _dicomSCPManager;
    }

    private static PacsEntityService        _pacsEntityService;
    private static DicomSCPManager          _dicomSCPManager;
    private static Map<String, OrmStrategy> _strategies;
}
