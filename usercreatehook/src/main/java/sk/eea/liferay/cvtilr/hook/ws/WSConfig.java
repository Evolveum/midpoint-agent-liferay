package sk.eea.liferay.cvtilr.hook.ws;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.PropsUtil;


/**
 * @author Marian Soban
 */
public class WSConfig {

    private static final Log LOG = LogFactoryUtil.getLog(WSConfig.class);

    public static final String PROPERTY_WS_ENDPOINT_URL = "ws.endpoint.url";
    public static final String PROPERTY_WS_USER = "ws.user";
    public static final String PROPERTY_WS_PASSWORD = "ws.password";
    public static final String PROPERTY_MIDPOINT_PASSWORD_MIN_UNIQUE_CHARS = "midpoint.password.min.unique.chars";
    public static final String PROPERTY_MIDPOINT_PASSWORD_MIN_LENGHT = "midpoint.password.min.length";
    public static final String PROPERTY_EMAIL_DOMAINS_ALLWAYS_PERMITTED = "email.domains.allways.permitted";
    public static final String PROPERTY_SCREENNAME_RESERVED = "screenname.reserved";

    private static final String DEFAULT_ENDPOINT_URL = "http://localhost:8080/midpoint/model/model-1";
    private static final String DEFAULT_USER = "administrator";
    private static final String DEFAULT_PASSWORD = "5ecr3t";

    private static final int DEFAULT_PASSWORD_MIN_UNIQUE_CHARS = 3;
    private static final int DEFAULT_PASSWORD_MIN_LENGTH = 8;

    public static String getEndPointUrl() {
        String result = PropsUtil.get(PROPERTY_WS_ENDPOINT_URL);
        if (StringUtils.isBlank(result)) {
            result = DEFAULT_ENDPOINT_URL;
            LOG.info("Property '" + PROPERTY_WS_ENDPOINT_URL
                    + "' not found in hook's portal.properties - using default value '" + result + "'");
        } else {
            LOG.debug("Property '" + PROPERTY_WS_ENDPOINT_URL + "'  value: '" + result + "'");
        }
        return result;
    }

    public static String getUser() {
        String result = PropsUtil.get(PROPERTY_WS_USER);
        if (StringUtils.isEmpty(result)) {
            result = DEFAULT_USER;
            LOG.info("Property '" + PROPERTY_WS_USER
                    + "' not found in hook's portal.properties - using default value '" + result + "'");
        } else {
            LOG.debug("Property '" + PROPERTY_WS_USER + "'  value: '" + result + "'");
        }
        return result;
    }

    public static String getPassword() {
        String result = PropsUtil.get(PROPERTY_WS_PASSWORD);
        if (StringUtils.isEmpty(result)) {
            result = DEFAULT_PASSWORD;
            LOG.info("Property '" + PROPERTY_WS_PASSWORD
                    + "' not found in hook's portal.properties - using default value");
        }
        return result;
    }

    public static int getMidpointPasswordMinUniqueChars() {
        int result = -1;
        try {
            String resultStr = PropsUtil.get(PROPERTY_MIDPOINT_PASSWORD_MIN_UNIQUE_CHARS);
            if (!StringUtils.isEmpty(resultStr)) {
            	result = Integer.parseInt(resultStr);
            }
        } catch (Exception e) {
            LOG.warn(e);
        }
        if (result < 1) {
            result = DEFAULT_PASSWORD_MIN_UNIQUE_CHARS;
            LOG.info("Property '" + PROPERTY_MIDPOINT_PASSWORD_MIN_UNIQUE_CHARS
                    + "' not found or not valid in hook's portal.properties - using default value '" + result + "'");
        } else {
            LOG.debug("Property '" + PROPERTY_MIDPOINT_PASSWORD_MIN_UNIQUE_CHARS + "'  value: '" + result + "'");
        }
        return result;
    }

    public static int getMidpointPasswordMinLenght() {
        int result = -1;
        try {
            String resultStr = PropsUtil.get(PROPERTY_MIDPOINT_PASSWORD_MIN_LENGHT);
            if (!StringUtils.isEmpty(resultStr)) {
            	result = Integer.parseInt(resultStr);
            }
        } catch (Exception e) {
            LOG.warn(e);
        }
        if (result < 1) {
            result = DEFAULT_PASSWORD_MIN_LENGTH;
            LOG.info("Property '" + PROPERTY_MIDPOINT_PASSWORD_MIN_LENGHT
                    + "' not found or not valid in hook's portal.properties - using default value '" + result + "'");
        } else {
            LOG.debug("Property '" + PROPERTY_MIDPOINT_PASSWORD_MIN_LENGHT + "'  value: '" + result + "'");
        }
        return result;
    }


    public static List<String> getAllwaysPermittedEmailDomains() {
        List<String> result = new ArrayList<String>();
        String value = PropsUtil.get(PROPERTY_EMAIL_DOMAINS_ALLWAYS_PERMITTED);
        if (!StringUtils.isBlank(value)) {
            StringTokenizer st = new StringTokenizer(value, ",");
            while (st.hasMoreTokens()) {
                String domain = st.nextToken().trim().toLowerCase();
                result.add(domain);
            }
        }
        LOG.debug("Property '" + PROPERTY_EMAIL_DOMAINS_ALLWAYS_PERMITTED + "'  value: '" + value
                + "'. Used domains from list: '" + result + "'");
        return result;
    }

    public static List<String> getReservedScreennames() {
        List<String> result = new ArrayList<String>();
        String value = PropsUtil.get(PROPERTY_SCREENNAME_RESERVED);
        if (!StringUtils.isBlank(value)) {
            StringTokenizer st = new StringTokenizer(value, ",");
            while (st.hasMoreTokens()) {
                String reservedScreenname = st.nextToken().trim().toLowerCase();
                result.add(reservedScreenname);
            }
        }
        LOG.debug("Property '" + PROPERTY_SCREENNAME_RESERVED + "'  value: '" + value
                + "'. Used reserved screennames from list: '" + result + "'");
        return result;
    }
}
