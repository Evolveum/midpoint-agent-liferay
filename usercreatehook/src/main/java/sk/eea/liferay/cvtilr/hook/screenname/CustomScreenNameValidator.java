package sk.eea.liferay.cvtilr.hook.screenname;

import com.liferay.portal.kernel.util.CharPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.security.auth.ScreenNameValidator;

/**
 * @author Brian Wing Shun Chan
 */

public class CustomScreenNameValidator implements ScreenNameValidator {

    public static final String CYRUS = "cyrus";

    public static final String POSTFIX = "postfix";

    public boolean validate(long companyId, String screenName) {
        if (Validator.isEmailAddress(screenName) || screenName.equalsIgnoreCase(CYRUS)
                || screenName.equalsIgnoreCase(POSTFIX) || (screenName.indexOf(CharPool.SLASH) != -1)
                || (screenName.indexOf(CharPool.UNDERLINE) != -1)) {

            return false;
        } else {
            return true;
        }
    }

}
