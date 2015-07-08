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
package com.evolveum.liferay.usercreatehook.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import javax.mail.internet.InternetAddress;

import org.apache.commons.lang.StringUtils;

import com.evolveum.liferay.usercreatehook.exception.WSException;
import com.evolveum.liferay.usercreatehook.ws.ModelPortWrapper;
import com.evolveum.liferay.usercreatehook.ws.WSConfig;
import com.liferay.mail.service.MailServiceUtil;
import com.liferay.portal.UserPasswordException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.mail.MailMessage;
import com.liferay.portal.kernel.util.PrefsPropsUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.Organization;
import com.liferay.portal.model.Role;
import com.liferay.portal.model.User;
import com.liferay.portal.model.UserGroupRole;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.UserLocalService;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.service.UserLocalServiceWrapper;

/**
 * Customization of liferay's UserLocalServiceImpl for integration with external system.<br/>
 * External system is called by WebService after successful email (account) verification.
 * 
 * @author Marian Soban
 */
public class CustomUserLocalServiceImpl extends UserLocalServiceWrapper {

    private static final Log LOG = LogFactoryUtil.getLog(CustomUserLocalServiceImpl.class);

    public static final String DEFAULT_LIFERAY_ROLE = "User";

    public static final String TIME_PATTERN = "dd.MM.yyyy HH:mm:ss.SSS";

    /**
     * For logging purposes.
     */
    private static final long TRESHOLD = 3000l; // [ms]

    public CustomUserLocalServiceImpl(UserLocalService userLocalService) {
        super(userLocalService);
    }

    @Override
    public User updatePasswordManually(long userId,
            String password,
            boolean passwordEncrypted,
            boolean passwordReset,
            Date passwordModifiedDate) throws PortalException, SystemException {
        if (Validator.isNotNull(password)) {
            checkPasswordValidity(password);
        }

        if (!callWSForPing()) {
            throw new WSException();
        }
        User result = super.updatePasswordManually(userId, password, passwordEncrypted, passwordReset,
                passwordModifiedDate);
        if (!result.isDefaultUser()) {
            // XXX if we got here, password was changed successfully - we can call external system now.
            LOG.info("Password change for user '" + result.getScreenName() + "' OK");
            internalUpdatePassword(result, password);
        }
        return result;
    }

    @Override
    public User updatePassword(long userId, String password1, String password2, boolean passwordReset)
            throws PortalException, SystemException {
        if (Validator.isNotNull(password1) && Validator.isNotNull(password2) && password1.equals(password2)) {
            checkPasswordValidity(password1);
        }

        // if (!callWSForPing()) {
        // throw new WSException();
        // }
        User result = super.updatePassword(userId, password1, password2, passwordReset);
        if (!result.isDefaultUser()) {
            // XXX if we got here, password was changed successfully - we can call external system now.
            LOG.info("Password change for user '" + result.getScreenName() + "' OK");
            internalUpdatePassword(result, password1);
        }
        return result;
    }

    @Override
    public User updatePassword(long userId,
            String password1,
            String password2,
            boolean passwordReset,
            boolean silentUpdate) throws PortalException, SystemException {
        if (Validator.isNotNull(password1) && Validator.isNotNull(password2) && password1.equals(password2)) {
            checkPasswordValidity(password1);
        }

        // if (!callWSForPing()) {
        // throw new WSException();
        // }
        User result = super.updatePassword(userId, password1, password2, passwordReset, silentUpdate);
        if (!result.isDefaultUser()) {
            // XXX if we got here, password was changed successfully - we can call external system now.
            LOG.info("Password change for user '" + result.getScreenName() + "' OK");
            internalUpdatePassword(result, password1);
        }
        return result;
    }

    private static void internalUpdatePassword(User user, String password) throws SystemException, PortalException {
    	LOG.info("internalUpdatePassword u:"+user.getScreenName());
        Boolean userExists = callWSToCheckUserExistency(user.getScreenName(), user.getCompanyId());
        if (userExists != null) {
            if (userExists.booleanValue()) {
                // user already exists in midpoint - just change his password
            	try {
                callWSForPasswordChange(user.getScreenName(), password, user.getCompanyId());
            	} catch (Exception e) {
            		if (e instanceof WSException) {
            			LOG.error("Retrying user creation", e);
                        createUserInMidpoint(user);
            		} else {
            			LOG.error(e);
            		}
            	}
            } else {
                // user doesn't exist - create him in midpoint
                createUserInMidpoint(user);
            }
        }
    }

    @Override
    public User updateUser(long userId,
            String oldPassword,
            String newPassword1,
            String newPassword2,
            boolean passwordReset,
            String reminderQueryQuestion,
            String reminderQueryAnswer,
            String screenName,
            String emailAddress,
            long facebookId,
            String openId,
            String languageId,
            String timeZoneId,
            String greeting,
            String comments,
            String firstName,
            String middleName,
            String lastName,
            int prefixId,
            int suffixId,
            boolean male,
            int birthdayMonth,
            int birthdayDay,
            int birthdayYear,
            String smsSn,
            String aimSn,
            String facebookSn,
            String icqSn,
            String jabberSn,
            String msnSn,
            String mySpaceSn,
            String skypeSn,
            String twitterSn,
            String ymSn,
            String jobTitle,
            long[] groupIds,
            long[] organizationIds,
            long[] roleIds,
            List<UserGroupRole> userGroupRoles,
            long[] userGroupIds,
            ServiceContext serviceContext) throws PortalException, SystemException {
        if (Validator.isNotNull(newPassword1) && Validator.isNotNull(newPassword2) && newPassword1.equals(newPassword2)) {
            checkPasswordValidity(newPassword1);
        }
        User oldUser = UserLocalServiceUtil.getUser(userId);
        List<String> oldRoleNames = convertRolesToRoleNames(oldUser != null ? oldUser.getRoles() : null);
        String oldFirstName = oldUser != null ? oldUser.getFirstName() : null;
        String oldLastName = oldUser != null ? oldUser.getLastName() : null;
        String oldFullName = oldUser != null ? oldUser.getFullName() : null;

        // if (!callWSForPing()) {
        // throw new WSException();
        // }
        User result = super.updateUser(userId, oldPassword, newPassword1, newPassword2, passwordReset,
                reminderQueryQuestion, reminderQueryAnswer, screenName, emailAddress, facebookId, openId, languageId,
                timeZoneId, greeting, comments, firstName, middleName, lastName, prefixId, suffixId, male,
                birthdayMonth, birthdayDay, birthdayYear, smsSn, aimSn, facebookSn, icqSn, jabberSn, msnSn, mySpaceSn,
                skypeSn, twitterSn, ymSn, jobTitle, groupIds, organizationIds, roleIds, userGroupRoles, userGroupIds,
                serviceContext);
        // XXX if we got here, password could be changed successfully or roles could be change- we can call external
        // system now for these cases
        if (!result.isDefaultUser() && Validator.isNotNull(newPassword1)) {
            LOG.info("Password change for user '" + result.getScreenName() + "' OK");
            try {
            	internalUpdatePassword(result, newPassword1);
            } catch (Exception e) {
            	LOG.error("Password change failed",e);
            }
        }

        // name change part
        if (!StringUtils.equals(oldFirstName, result.getFirstName())
                || !StringUtils.equals(oldLastName, result.getLastName())
                || !StringUtils.equals(oldFullName, result.getFullName())) {
            LOG.info("User name change for user '" + result.getScreenName() + "' OK");
            callWSForUserNameChange(result.getScreenName(), result.getFirstName(), result.getLastName(),
                    result.getFullName(), result.getCompanyId());
        }
        // StringUtils.equals(ymSn, jobTitle)

        // roles update part
        List<String> newRoleNames = convertRolesToRoleNames(result != null ? result.getRoles() : null);
        List<String> rolesToAdd = new ArrayList<String>();
        List<String> rolesToRemove = new ArrayList<String>();

        for (String newRole : newRoleNames) {
            if (!oldRoleNames.contains(newRole)) {
                // added role
                rolesToAdd.add(newRole);
            }
        }
        for (String oldRole : oldRoleNames) {
            if (!newRoleNames.contains(oldRole)) {
                // deleted role
                rolesToRemove.add(oldRole);
            }
        }
        if (!rolesToAdd.isEmpty() || !rolesToRemove.isEmpty()) {
            callWSForUserRolesChange(result.getScreenName(), rolesToAdd, rolesToRemove, result.getCompanyId());
        }
        return result;
    }

    @Override
    public User deleteUser(long userId) throws PortalException, SystemException {
        User user = UserLocalServiceUtil.getUser(userId);
        boolean defaultUser = user != null ? user.isDefaultUser() : true;
        String screenName = user != null ? user.getScreenName() : null;
        long companyId = user != null ? user.getCompanyId() : -1;
        User result = super.deleteUser(userId);
        if (!defaultUser) {
            // XXX if we got here, user was deleted successfully - we can call external system now.
            LOG.info("Delete of user '" + screenName + "' OK");
            callWSForUserDelete(screenName, companyId);
        }
        return result;
    }

    @Override
    public User deleteUser(User user) throws PortalException, SystemException {
        boolean defaultUser = user != null ? user.isDefaultUser() : true;
        String screenName = user != null ? user.getScreenName() : null;
        long companyId = user != null ? user.getCompanyId() : -1;
        User result = super.deleteUser(user);
        if (!defaultUser) {
            // XXX if we got here, user was deleted successfully - we can call external system now.
            LOG.info("Delete of user '" + screenName + "' OK");
            callWSForUserDelete(screenName, companyId);
        }
        return result;
    }

    // ---

    private static void createUserInMidpoint(User user) throws PortalException, SystemException {
        LOG.info("Creating user in midpoint '" + (user != null ? user.getScreenName() : null) + "'");
        if (!callWSForPing()) {
            throw new WSException();
        }
        String email = user.getEmailAddress();
        String name = user.getScreenName(); // screenname
        // XXX password is unecrypted only when portal configuration (portal-ext.properties) contains:
        // passwords.encryption.algorithm=NONE
        String password = user.getPassword();
        String firstName = user.getFirstName();
        String lastName = user.getLastName();
        String fullName = user.getFullName();
        List<Organization> userOrganizations = user.getOrganizations();
        if (userOrganizations != null && userOrganizations.size() > 1) {
            LOG.warn("User with login '" + name + "' has more than 1 organization, taking for export the first one");
        }
        Organization organization = userOrganizations != null && userOrganizations.size() > 0 ? userOrganizations
                .get(0) : null;
        String organizationName = null;
        String subOrganizationName = null;
        if (organization != null) {
            if (organization.isRoot()) {
                organizationName = organization.getName();
                subOrganizationName = null;
            } else {
                subOrganizationName = organization.getName();
                organizationName = organization.getParentOrganization().getName();
            }
        }

        List<String> roleNames = convertRolesToRoleNames(user.getRoles());

        callWSForAccountCreate(email, name, password, firstName, lastName, fullName, organizationName,
                subOrganizationName, roleNames, user.getCompanyId());
    }

    /**
     * Gets list of role names.<br/>
     * Warning: <b>Removes default Liferay's role - 'User' from list</b>
     */
    protected static List<String> convertRolesToRoleNames(List<Role> userRoles) {
        List<String> result = new ArrayList<String>();
        if (userRoles != null) {
            for (Role role : userRoles) {
                String roleName = role.getName();
                if (DEFAULT_LIFERAY_ROLE.equals(roleName)) {
                    continue; // XXX ignore default Liferay's role 'User'
                }
                result.add(roleName);
            }
        }
        return result;
    }

    protected static void checkPasswordValidity(String password) throws UserPasswordException {
        HashSet<String> tmp = new HashSet<String>(
                com.evolveum.liferay.usercreatehook.password.StringPolicyUtils.stringTokenizer(password));

        boolean containsUpperCase = password.matches(".*\\p{javaLowerCase}.*");
        boolean containsLowerCase = password.matches(".*\\p{javaUpperCase}.*");
        boolean containsNumber = password.matches(".*\\d.*");
        boolean containsSpecial = password.matches(".*[\\_\\.\\!\\@\\$\\*\\=\\-\\?].*");
        if (WSConfig.getMidpointPasswordMinUniqueChars() > tmp.size() ||
        		WSConfig.getMidpointPasswordMinLenght() > password.length() || !containsUpperCase
        		|| !containsLowerCase || !containsNumber || !containsSpecial) {
            throw new UserPasswordException(UserPasswordException.PASSWORD_TOO_TRIVIAL);
        }
    }

    /**
     * Checks WS accessibility.
     */
    public static boolean callWSForPing() {
        LOG.debug("Calling external WS for ping.");
        long start = System.currentTimeMillis();
        LOG.debug("callWSForPing() START");
        boolean result = ModelPortWrapper.ping();
        long took = System.currentTimeMillis() - start;
        String message = "callWSForPing() took " + took + " [ms] and returned '" + result + "' END";
        if (result == false || took > TRESHOLD) {
            LOG.info(message);
        } else {
            LOG.debug(message);
        }
        return result;
    }

    public static void callWSForAccountCreate(String email,
            String name,
            String password,
            String firstName,
            String lastName,
            String fullName,
            String organizationName,
            String subOrganizationName,
            List<String> roles,
            long companyId) {
        LOG.info("Calling external WS for verified user creation with following data: [email='" + email + "', name='"
                + name + "', password='***', firstName='" + firstName + "', lastName='" + lastName + "', fullName='"
                + fullName + "', organizationName='" + organizationName + "', subOrganizationName='"
                + subOrganizationName + "', roles='" + roles.toString() + "']");
        long start = System.currentTimeMillis();
        LOG.debug("callWSForAccountCreate() for screenname '" + name + "' START");
        boolean result = ModelPortWrapper.createUser(email, name, password, firstName, lastName, fullName,
                organizationName, subOrganizationName, roles);
        long took = System.currentTimeMillis() - start;
        String message = "callWSForAccountCreate() for screenname '" + name + "' took " + took + " [ms] and returned '"
                + result + "' END";
        if (result == false || took > TRESHOLD) {
            LOG.info(message);
        } else {
            LOG.debug(message);
        }

        if (!result) {
            sendEmail("callWSForAccountCreate()", companyId);
        }
    }

    public static Boolean callWSToCheckUserExistency(String name, long companyId) {
        LOG.info("Calling external WS for check of user existency with following data: [name='" + name + "']");
        long start = System.currentTimeMillis();
        LOG.debug("callWSToCheckUserExistency() for screenname '" + name + "' START");
        Boolean result = ModelPortWrapper.existsUser(name);
        long took = System.currentTimeMillis() - start;
        String message = "callWSToCheckUserExistency() for screenname '" + name + "' took " + took
                + " [ms] and returned '" + result + "' END";
        LOG.info(message);
        if (result == null) {
            sendEmail("callWSToCheckUserExistency()", companyId);
        }
        return result;
    }

    public static void callWSForUserDelete(String name, long companyId) {
        LOG.info("Calling external WS for user delete with following data: [name='" + name + "']");
        long start = System.currentTimeMillis();
        LOG.debug("callWSForUserDelete() for screenname '" + name + "' START");
        boolean result = ModelPortWrapper.deleteUser(name);
        long took = System.currentTimeMillis() - start;
        String message = "callWSForUserDelete() for screenname '" + name + "' took " + took + " [ms] and returned '"
                + result + "' END";
        LOG.info(message);
        if (!result) {
            sendEmail("callWSForUserDelete()", companyId);
        }
    }

    public static void callWSForUserNameChange(String name,
            String newFirstName,
            String newLastName,
            String newFullname,
            long companyId) {
        LOG.info("Calling external WS for user name change with following data: [name='" + name + "', newFirstName='"
                + newFirstName + "', newLastName='" + newLastName + "', newFullname='" + newFullname + "']");
        long start = System.currentTimeMillis();
        LOG.debug("callWSForUserNameChange() for screenname '" + name + "' START");
        boolean result = ModelPortWrapper.changeName(name, newFirstName, newLastName, newFullname);
        long took = System.currentTimeMillis() - start;
        String message = "callWSForUserNameChange() for screenname '" + name + "' took " + took
                + " [ms] and returned '" + result + "' END";
        LOG.info(message);
        if (!result) {
            sendEmail("callWSForUserNameChange()", companyId);
        }
    }

    public static void callWSForPasswordChange(String name, String password, long companyId) throws Exception {
        LOG.info("Calling external WS for password change with following data: [name='" + name + "', password='***']");
        long start = System.currentTimeMillis();
        LOG.debug("callWSForPasswordChange() for screenname '" + name + "' START");
        boolean result = ModelPortWrapper.changePassword(name, password);
        long took = System.currentTimeMillis() - start;
        String message = "callWSForPasswordChange() for screenname '" + name + "' took " + took
                + " [ms] and returned '" + result + "' END";
        LOG.info(message);
        if (!result) {
            sendEmail("callWSForPasswordChange()", companyId);
        }
    }

    public static void callWSForUserRolesChange(String name,
            List<String> rolesToAdd,
            List<String> rolesToRemove,
            long companyId) {
        LOG.info("Calling external WS for user roles change with following data: [name='" + name + "', rolesToAdd='"
                + rolesToAdd + "', rolesToRemove='" + rolesToRemove + "']");
        long start = System.currentTimeMillis();
        LOG.debug("callWSForUserRolesChange() for screenname '" + name + "' START");
        boolean result = ModelPortWrapper.changeRoles(name, rolesToAdd, rolesToRemove);
        long took = System.currentTimeMillis() - start;
        String message = "callWSForUserRolesChange() for screenname '" + name + "' took " + took
                + " [ms] and returned '" + result + "' END";
        LOG.info(message);
        if (!result) {
            sendEmail("callWSForUserRolesChange()", companyId);
        }
    }

    // ---

    protected static void sendEmail(String wsCall, long companyId) {
        try {

            String fromName = PrefsPropsUtil.getString(companyId, "admin.email.from.name");
            String fromAddress = PrefsPropsUtil.getString(companyId, "admin.email.from.address");

            String toAddress = fromAddress;
            String toName = fromName;

            ClassLoader classLoader = CustomUserLocalServiceImpl.class.getClassLoader();

            SimpleDateFormat sdf = new SimpleDateFormat(TIME_PATTERN);
            String serverTime = sdf.format(new Date());

            String subject = StringUtil.read(classLoader, "content/email/email_ws_error_subject.tmpl");
            String body = StringUtil.read(classLoader, "content/email/email_ws_error_body.tmpl");

            subject = StringUtil.replace(subject, new String[]{"[$FROM_ADDRESS$]", "[$FROM_NAME$]", "[$TO_ADDRESS$]",
                "[$TO_NAME$]", "[$WS_CALL$]", "[$SERVER_TIME$]"}, new String[]{fromAddress, fromName, toAddress,
                toName, wsCall, serverTime});

            body = StringUtil.replace(body, new String[]{"[$FROM_ADDRESS$]", "[$FROM_NAME$]", "[$TO_ADDRESS$]",
                "[$TO_NAME$]", "[$WS_CALL$]", "[$SERVER_TIME$]"}, new String[]{fromAddress, fromName, toAddress,
                toName, wsCall, serverTime});

            InternetAddress from = new InternetAddress(fromAddress, fromName);

            InternetAddress to = new InternetAddress(toAddress, toName);

            MailMessage mailMessage = new MailMessage(from, to, subject, body, true);

            MailServiceUtil.sendEmail(mailMessage);
        } catch (Exception e) {
            LOG.error("Error while sending email: " + e.getMessage(), e);
        }
    }
    
/*    public static void main(String[] args) {
    	try {
    		checkPasswordValidity("Abcde123");
    	} catch (Exception e) {
    		System.out.println(e);
    	}
    }
*/
}
