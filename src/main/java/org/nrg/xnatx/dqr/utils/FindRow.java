package org.nrg.xnatx.dqr.utils;

import java.util.Collection;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.nrg.xnatx.dqr.domain.Study;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;

@Getter
@Accessors(prefix = "_")
@Builder
@AllArgsConstructor
public class FindRow {
    private final PacsSearchCriteria  _criteria;
    private final Map<String, String> _relabelMap;
    private final Collection<Study>   _studies;
}
