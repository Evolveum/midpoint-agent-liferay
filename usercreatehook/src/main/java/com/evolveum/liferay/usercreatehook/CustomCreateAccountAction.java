/*
 * Copyright (c) 2011-2015 EEA s.r.o., Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.liferay.usercreatehook;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import org.apache.commons.lang.StringUtils;

import com.evolveum.liferay.usercreatehook.util.EmailUtil;
import com.evolveum.liferay.usercreatehook.ws.WSConfig;
import com.liferay.portal.ReservedUserScreenNameException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.struts.BaseStrutsPortletAction;
import com.liferay.portal.kernel.struts.StrutsPortletAction;
import com.liferay.portal.kernel.util.Constants;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.CompanyConstants;
import com.liferay.portal.model.EmailAddress;
import com.liferay.portal.model.Organization;
import com.liferay.portal.model.User;
import com.liferay.portal.service.EmailAddressLocalServiceUtil;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;

/**
 * CreateAccountAction hooked/hacked to set also organization when user is created.
 * 
 * @author Marian Soban
 */
public class CustomCreateAccountAction extends BaseStrutsPortletAction {

    private static final Log LOG = LogFactoryUtil.getLog(CustomCreateAccountAction.class);

    public static final String METHOD_GET_REDIRECT_LOCATION = "getRedirectLocation";

    public static final String URL_PARAMETER_LOGIN = "&_58_login="; // 58=login portlet

    @Override
    public void processAction(StrutsPortletAction originalStrutsPortletAction,
            PortletConfig portletConfig,
            ActionRequest actionRequest,
            ActionResponse actionResponse) throws Exception {

        LOG.debug("Before call of original action");
        ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest.getAttribute(WebKeys.THEME_DISPLAY);
        Company company = themeDisplay.getCompany();

        // reserved screennames validation part START
        String screenName = ParamUtil.getString(actionRequest, "screenName");
        Set<String> reservedScreennamesSet = new HashSet<String>();

        List<String> reservedScreennames = WSConfig.getReservedScreennames();
        if (reservedScreennames != null) {
            reservedScreennamesSet.addAll(reservedScreennames);
        }

        if (screenName != null && reservedScreennamesSet.contains(screenName.trim())) {
            SessionErrors.add(actionRequest.getPortletSession(), ReservedUserScreenNameException.class.getName());
            return;
        }
        // reserved screennames validation part END

        // custom email validation part START

        long parentOrganizationId = ParamUtil.getLong(actionRequest, "parentOrganizationId");
        long subOrganizationId = ParamUtil.getLong(actionRequest, "organizationId");
        long organizationId = -1;
        if (parentOrganizationId > 0l || subOrganizationId > 0l) {
            organizationId = subOrganizationId > 0l ? subOrganizationId : parentOrganizationId;
            Set<String> permittedDomainsSet = new HashSet<String>();

            // get addition email addresses of organization
            List<EmailAddress> organizationEmailAddresses = EmailAddressLocalServiceUtil.getEmailAddresses(
                    company.getCompanyId(), Organization.class.getName(), organizationId);

            if (organizationEmailAddresses != null) {
                for (EmailAddress emailAddress : organizationEmailAddresses) {
                    String domain = EmailUtil.getEmailDomain(emailAddress.getAddress());
                    permittedDomainsSet.add(domain);
                }
            }

            List<String> allwaysPermittedDomains = WSConfig.getAllwaysPermittedEmailDomains();
            if (allwaysPermittedDomains != null) {
                permittedDomainsSet.addAll(allwaysPermittedDomains);
            }

            String emailAddress = ParamUtil.getString(actionRequest, "emailAddress");
            String emailDomain = null;
            if (emailAddress != null) {
                emailAddress = emailAddress.trim().toLowerCase();
                emailDomain = EmailUtil.getEmailDomain(emailAddress);
            }

            if (emailDomain == null || !permittedDomainsSet.contains(emailDomain)) {
                SessionErrors.add(actionRequest.getPortletSession(), "notPermittedEmailDomainError");
                return;
            }
        }

        // custom email validation part END

        originalStrutsPortletAction.processAction(originalStrutsPortletAction, portletConfig, actionRequest,
                actionResponse);
        LOG.debug("After call of original action");

        String cmd = ParamUtil.getString(actionRequest, Constants.CMD);

        if (cmd.equals(Constants.ADD)) {

            // XXX [mso] hack to get redirect location with created user's login in pattern '...&_58_login=<user-login>'
            LOG.debug("Trying to get redirect location...");
            Class<?> actionResponseImplClass = actionResponse.getClass(); // com.liferay.portlet.ActionResponseImpl
            Class<?> stateAwareResponseImplClass = actionResponseImplClass.getSuperclass(); // com.liferay.portlet.StateAwareResponseImpl

            Method redirectLocationMethod = stateAwareResponseImplClass.getDeclaredMethod(METHOD_GET_REDIRECT_LOCATION);
            String redirectLocation = (String) redirectLocationMethod.invoke(actionResponse, (Object[]) null);
            LOG.debug("Redirect location: '" + redirectLocation + "'");

            if (!StringUtils.isBlank(redirectLocation)) {
                // user was created
                // redirect location example for login 'a3':
                // 'http://localhost:8080/web/guest/what-we-do?p_p_id=58&p_p_lifecycle=0&p_p_state=maximized&p_p_mode=view&saveLastPath=0&_58_struts_action=%2Flogin%2Flogin&_58_login=a3'
                // 1) get his login
                // 2) update his organizations if any

                // 1)
                LOG.debug("Trying to get new user's login from redirect location...");
                String login = getLoginFromRedirectUrl(redirectLocation);
                LOG.debug("User's login: '" + login + "'");
                if (!StringUtils.isBlank(login)) {
                    User user = loadUser(company, login);
                    if (user != null) {
                        // 2)
                        if (organizationId > 0l) {
                            LOG.info("Setting organization/suborganization: '" + organizationId + "' to user: '"
                                    + login + "'");
                            UserLocalServiceUtil.addOrganizationUsers(organizationId, new long[]{user.getUserId()});
                            LOG.debug("Organization/suborganization set successfully");
                        }
                    } else {
                        LOG.error("Unable to load user for login '" + login + "'");
                    }
                } else {
                    LOG.error("Unable to get login from redirect location: '" + redirectLocation + "'");
                }
            }
        }
    }

    protected User loadUser(Company company, String login) throws SystemException, PortalException {
        User result = null;
        LOG.debug("Trying to load user for login (ID/screenname/email address): '" + login + "'");
        if (company.getAuthType().equals(CompanyConstants.AUTH_TYPE_ID)) {
            result = UserLocalServiceUtil.getUserById(Long.parseLong(login));
        } else if (company.getAuthType().equals(CompanyConstants.AUTH_TYPE_SN)) {
            result = UserLocalServiceUtil.getUserByScreenName(company.getCompanyId(), login);
        } else {
            result = UserLocalServiceUtil.getUserByEmailAddress(company.getCompanyId(), login);
        }

        if (result != null) {
            LOG.debug("User for login '" + login + "' loaded successfully");
        } else {
            LOG.warn("User for login '" + login + "' not found");
        }
        return result;
    }

    @Override
    public String render(StrutsPortletAction originalStrutsPortletAction,
            PortletConfig portletConfig,
            RenderRequest renderRequest,
            RenderResponse renderResponse) throws Exception {
        return originalStrutsPortletAction.render(null, portletConfig, renderRequest, renderResponse);
    }

    @Override
    public void serveResource(StrutsPortletAction originalStrutsPortletAction,
            PortletConfig portletConfig,
            ResourceRequest resourceRequest,
            ResourceResponse resourceResponse) throws Exception {

        originalStrutsPortletAction.serveResource(originalStrutsPortletAction, portletConfig, resourceRequest,
                resourceResponse);
    }

    /**
     * Returns created user's login from redirect URL when new user is created fro sign-in page.
     */
    protected static String getLoginFromRedirectUrl(String redirectUrl) {
        String result = null;
        if (redirectUrl != null) {
            int index = redirectUrl.lastIndexOf(URL_PARAMETER_LOGIN);
            if (index > 0) {
                result = redirectUrl.substring(index + URL_PARAMETER_LOGIN.length());
            }
        }
        return result;
    }

}
