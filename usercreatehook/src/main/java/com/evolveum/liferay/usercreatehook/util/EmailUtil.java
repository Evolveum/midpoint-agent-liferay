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
package com.evolveum.liferay.usercreatehook.util;

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
