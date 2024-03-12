package org.nrg.xnatx.dqr.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.nrg.xnatx.dqr.utils.AeTitle;

@Data
@Accessors(prefix = "_")
@EqualsAndHashCode(callSuper = false)
public class PacsException extends DqrException {
    private static final long serialVersionUID = 3019798242092476487L;

    public PacsException(final String message) {
        super(message);
        _pacsId = 0;
        _aeTitle = AeTitle.EMPTY;
        _messageFormat = message;
    }

    public PacsException(final Exception cause) {
        super(cause);
        _pacsId = 0;
        _aeTitle = AeTitle.EMPTY;
        _messageFormat = "";
    }

    public PacsException(final String message, final Throwable cause) {
        super(message, cause);
        _pacsId = 0;
        _aeTitle = AeTitle.EMPTY;
        _messageFormat = message;
    }

    protected PacsException(final long pacsId, final String messageFormat) {
        _pacsId = pacsId;
        _aeTitle = AeTitle.EMPTY;
        _messageFormat = messageFormat;
    }

    protected PacsException(final AeTitle aeTitle, final String messageFormat) {
        _pacsId = 0;
        _aeTitle = aeTitle;
        _messageFormat = messageFormat;
    }

    private final long    _pacsId;
    private final AeTitle _aeTitle;
    private final String  _messageFormat;
}
