package org.apache.turbine.modules.screens;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.turbine.modules.actions.DqrSecureAction;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.dcm.scp.DicomSCPInstance;
import org.nrg.dcm.scp.DicomSCPManager;
import org.nrg.xnatx.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;
import org.nrg.xnatx.dqr.services.DqrProjectSettingsService;
import org.nrg.xnatx.dqr.services.PacsEntityService;
import org.nrg.xnatx.dqr.services.PacsService;
import org.nrg.mail.services.MailService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xnatx.dqr.exceptions.PacsNotFoundException;

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
        final List<DicomSCPInstance> scps = new ArrayList<>(getDicomSCPManager().getDicomSCPInstances().values());
        final List<DicomSCPInstance> enabledScps = Lists.newArrayList(Iterables.filter(scps, new Predicate<DicomSCPInstance>() {
            @Override
            public boolean apply(final DicomSCPInstance scp) {
                return scp.isEnabled();
            }
        }));
        return Pair.of(scps, enabledScps);
    }

    protected Map<String, OrmStrategy> getOrmStrategyMap() {
        if (_strategies == null) {
            _strategies = XDAT.getContextService().getBeansOfType(OrmStrategy.class);
        }
        return _strategies;
    }

    protected PacsService getPacsService() {
        if (_pacsService == null) {
            _pacsService = XDAT.getContextService().getBean(PacsService.class);
        }
        return _pacsService;
    }

    protected PacsEntityService getPacsEntityService() {
        if (_pacsEntityService == null) {
            _pacsEntityService = XDAT.getContextService().getBean(PacsEntityService.class);
        }
        return _pacsEntityService;
    }

    protected DqrPreferences getDqrPreferences() {
        if (_dqrPreferences == null) {
            _dqrPreferences = getDqrPreferences();
        }
        return _dqrPreferences;
    }

    protected DqrProjectSettingsService getDqrAdminSettings() {
        if (_dqrAdminSettings == null) {
            _dqrAdminSettings = XDAT.getContextService().getBean(DqrProjectSettingsService.class);
        }
        return _dqrAdminSettings;
    }

    protected DicomSCPManager getDicomSCPManager() {
        if (_dicomSCPManager == null) {
            _dicomSCPManager = XDAT.getContextService().getBean(DicomSCPManager.class);
        }
        return _dicomSCPManager;
    }

    protected SiteConfigPreferences getSiteConfigPreferences() {
        if (_siteConfigPreferences == null) {
            _siteConfigPreferences = XDAT.getContextService().getBean(SiteConfigPreferences.class);
        }
        return _siteConfigPreferences;
    }

    protected MailService getMailService() {
        if (_mailService == null) {
            _mailService = XDAT.getContextService().getBean(MailService.class);
        }
        return _mailService;
    }

    private static long getPassedPacsId(final RunData data) throws PacsNotFoundException {
        try {
            return Long.parseLong((String) TurbineUtils.GetPassedParameter("pacsId", data));
        } catch (NumberFormatException e) {
            throw new PacsNotFoundException(0);
        }
    }

    private static PacsService                       _pacsService;
    private static PacsEntityService                 _pacsEntityService;
    private static DqrPreferences            _dqrPreferences;
    private static DqrProjectSettingsService _dqrAdminSettings;
    private static DicomSCPManager           _dicomSCPManager;
    private static SiteConfigPreferences             _siteConfigPreferences;
    private static MailService                       _mailService;
    private static Map<String, OrmStrategy>          _strategies;
}
