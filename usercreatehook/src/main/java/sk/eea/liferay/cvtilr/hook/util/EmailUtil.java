package sk.eea.liferay.cvtilr.hook.util;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

/**
 * @author Marian Soban
 */
public class EmailUtil {
    public static final String EMAIL_DELIMITER = "@";

    private EmailUtil() {
    }

    /**
     * Gets domain of email address. Returns <i>null</i> if email address is invalid.
     */
    public static String getEmailDomain(String email) {
        String result = null;
        if (isValidEmailAddress(email)) {
            email = email.toLowerCase().trim();
            result = email.split(EMAIL_DELIMITER)[1];
        }
        return result;
    }

    public static boolean isValidEmailAddress(String email) {
        boolean result = true;
        if (email == null) {
            result = false;
        } else {
            try {
                InternetAddress emailAddr = new InternetAddress(email);
                emailAddr.validate();
            } catch (AddressException ex) {
                result = false;
            }
        }
        return result;
    }
}
