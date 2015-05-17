package sk.eea.liferay.cvtilr.hook;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.struts.BaseStrutsAction;
import com.liferay.portal.kernel.struts.StrutsAction;

/**
 * @author Marian Soban
 */
public class CustomVerifyEmailAddressAction extends BaseStrutsAction {

    private static final Log LOG = LogFactoryUtil.getLog(CustomVerifyEmailAddressAction.class);

    @Override
    public String execute(StrutsAction originalStrutsAction, HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        LOG.debug("Before call of original action");
        String result = originalStrutsAction.execute(request, response);
        LOG.debug("After call of original action, result=" + result);
        return result;
    }
}
