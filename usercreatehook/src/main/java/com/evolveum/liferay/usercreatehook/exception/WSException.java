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
package com.evolveum.liferay.usercreatehook.exception;

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
