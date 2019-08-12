package org.nrg.xapi.authorization;

import org.nrg.dqr.preferences.DqrPreferences;
import org.nrg.prefs.events.PreferenceHandlerMethod;
import org.aspectj.lang.JoinPoint;
import org.nrg.xdat.security.helpers.AccessLevel;
import org.nrg.xdat.security.helpers.Roles;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.event.listeners.methods.AbstractXnatPreferenceHandlerMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Checks whether user can access Dqr features.
 */
@Component
public class DqrUserXapiAuthorization extends AbstractXapiAuthorization implements PreferenceHandlerMethod {
    @Autowired
    public DqrUserXapiAuthorization(final DqrPreferences preferences) {
        _allowAllUsersToUseDqr = preferences.getAllowAllUsersToUseDqr();
    }

    /**
     * Tests whether the current user should be able to access any API calls that specify this authorization delegate. If
     * {@link DqrPreferences#getAllowAllUsersToUseDqr()} is not true, the user must be an admin or have the Dqr role.
     */
    protected boolean checkImpl(final AccessLevel accessLevel, final JoinPoint joinPoint, final UserI user, final HttpServletRequest request) {
        return _allowAllUsersToUseDqr || Roles.isSiteAdmin(user) || Roles.checkRole(user, "Dqr");
    }

    @Override
    protected boolean considerGuests() {
        return false;
    }

    @Override
    public List<String> getToolIds() {
        return _handlerProxy.getToolIds();
    }

    @Override
    public List<String> getHandledPreferences() {
        return _handlerProxy.getHandledPreferences();
    }

    @Override
    public Set<String> findHandledPreferences(final Collection<String> preferences) {
        return _handlerProxy.findHandledPreferences(preferences);
    }

    @Override
    public void handlePreferences(final Map<String, String> values) {
        _handlerProxy.handlePreferences(values);
    }

    @Override
    public void handlePreference(final String preference, final String value) {
        _handlerProxy.handlePreference(preference, value);
    }

    private final PreferenceHandlerMethod _handlerProxy = new AbstractXnatPreferenceHandlerMethod(DqrPreferences.DQR_TOOL_ID, "allowAllUsersToUseDqr") {
        @Override
        protected void handlePreferenceImpl(final String preference, final String value) {
            _allowAllUsersToUseDqr = Boolean.parseBoolean(value);
        }
    };

    private boolean _allowAllUsersToUseDqr;
}
