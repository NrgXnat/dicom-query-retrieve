package org.nrg.xnatx.dqr.utils;

import lombok.AllArgsConstructor;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

@Value
@AllArgsConstructor
public class AeTitle implements Serializable {
    private static final long serialVersionUID = -3480526692304977042L;

    public static final AeTitle EMPTY = new AeTitle(null, 0);

    public AeTitle(final String aeTitleAndPort) {
        if (StringUtils.contains(aeTitleAndPort, ":")) {
            final String[] parts = aeTitleAndPort.split(":");
            aeTitle = parts[0];
            port = Integer.parseInt(parts[1]);
        } else {
            aeTitle = aeTitleAndPort;
            port = 0;
        }
    }

    @Override
    public String toString() {
        return port == 0 ? aeTitle : aeTitle + ":" + port;
    }

    String aeTitle;
    int    port;
}
