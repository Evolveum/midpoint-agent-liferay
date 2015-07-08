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
package com.evolveum.liferay.usercreatehook.screenname;

import com.liferay.portal.NoSuchGroupException;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.CharPool;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PrefsPropsUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.security.auth.DefaultScreenNameValidator;
import com.liferay.portal.security.auth.ScreenNameGenerator;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.UserLocalServiceUtil;

/**
 * Customized - screenname is created from email in different way than normally.
 * 
 * @author Brian Wing Shun Chan
 * @author Alexander Chow
 * @author Juan Fernández
 * @author Marian Soban
 */
public class CustomScreenNameGenerator implements ScreenNameGenerator {

    public String generate(long companyId, long userId, String emailAddress) throws Exception {

        String screenName = null;

        if (Validator.isNotNull(emailAddress)) {
            // XXX change 1
            // screenName = StringUtil.extractFirst(emailAddress, CharPool.AT).toLowerCase();
            screenName = emailAddress.toLowerCase();

            for (char c : screenName.toCharArray()) {
                // XXX change 2
                // if (!Validator.isChar(c) && !Validator.isDigit(c) && (c != CharPool.DASH) && (c != CharPool.PERIOD))
                // {
                if (!Validator.isChar(c) && !Validator.isDigit(c) && (c != CharPool.DASH)) {
                    // XXX change 3
                    // screenName = StringUtil.replace(screenName, c, CharPool.PERIOD);
                    screenName = StringUtil.replace(screenName, c, CharPool.DASH);
                }
            }

            if (screenName.equals(DefaultScreenNameValidator.CYRUS)
                    || screenName.equals(DefaultScreenNameValidator.POSTFIX)) {

                screenName += StringPool.PERIOD + userId;
            }
        } else {
            screenName = String.valueOf(userId);
        }

        if (!_USERS_SCREEN_NAME_ALLOW_NUMERIC && Validator.isNumber(screenName)) {

            screenName = _NON_NUMERICAL_PREFIX + screenName;
        }

        String[] reservedScreenNames = PrefsPropsUtil.getStringArray(companyId, PropsKeys.ADMIN_RESERVED_SCREEN_NAMES,
                StringPool.NEW_LINE, _ADMIN_RESERVED_SCREEN_NAMES);

        for (String reservedScreenName : reservedScreenNames) {
            if (screenName.equalsIgnoreCase(reservedScreenName)) {
                return getUnusedScreenName(companyId, screenName);
            }
        }

        try {
            UserLocalServiceUtil.getUserByScreenName(companyId, screenName);
        } catch (NoSuchUserException nsue) {
            try {
                GroupLocalServiceUtil.getFriendlyURLGroup(companyId, StringPool.SLASH + screenName);
            } catch (NoSuchGroupException nsge) {
                return screenName;
            }
        }

        return getUnusedScreenName(companyId, screenName);
    }

    protected String getUnusedScreenName(long companyId, String screenName) throws PortalException, SystemException {

        for (int i = 1;; i++) {
            String tempScreenName = screenName + StringPool.PERIOD + i;

            try {
                UserLocalServiceUtil.getUserByScreenName(companyId, tempScreenName);
            } catch (NoSuchUserException nsue) {
                try {
                    GroupLocalServiceUtil.getFriendlyURLGroup(companyId, StringPool.SLASH + tempScreenName);
                } catch (NoSuchGroupException nsge) {
                    screenName = tempScreenName;

                    break;
                }
            }
        }

        return screenName;
    }

    private static final String[] _ADMIN_RESERVED_SCREEN_NAMES = StringUtil.splitLines(PropsUtil
            .get(PropsKeys.ADMIN_RESERVED_SCREEN_NAMES));

    private static final String _NON_NUMERICAL_PREFIX = "user.";

    private static final boolean _USERS_SCREEN_NAME_ALLOW_NUMERIC = GetterUtil.getBoolean(PropsUtil
            .get(PropsKeys.USERS_SCREEN_NAME_ALLOW_NUMERIC));

}
