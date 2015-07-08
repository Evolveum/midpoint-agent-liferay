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
