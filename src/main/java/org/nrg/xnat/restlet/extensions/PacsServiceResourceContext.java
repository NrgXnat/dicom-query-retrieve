package org.nrg.xnat.restlet.extensions;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mike on 1/23/18.
 */
public class PacsServiceResourceContext implements org.apache.velocity.context.Context {

    @Override
    public Object put(final String key, final Object value) {
        return _store.put(key, value);
    }

    @Override
    public Object get(final String key) {
        return _store.get(key);
    }

    @Override
    public boolean containsKey(final Object key) {
        return key != null && key instanceof String && _store.containsKey(key);
    }

    @Override
    public Object[] getKeys() {
        return _store.keySet().toArray();
    }

    @Override
    public Object remove(final Object key) {
        return key != null && key instanceof String ? _store.remove(key) : null;
    }

    private final Map<String, Object> _store = new HashMap<>();

}