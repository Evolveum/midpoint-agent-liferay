package sk.eea.liferay.cvtilr.hook.exception;

import com.liferay.portal.kernel.exception.PortalException;

/**
 * @author Marian Soban
 */
public class WSException extends PortalException {

    private static final long serialVersionUID = -740111521372313378L;

    public WSException() {
        super();
    }

    public WSException(String msg) {
        super(msg);
    }

    public WSException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public WSException(Throwable cause) {
        super(cause);
    }
}
