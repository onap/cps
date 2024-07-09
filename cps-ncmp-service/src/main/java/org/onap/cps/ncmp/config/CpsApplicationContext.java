/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023 Nordix Foundation
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class CpsApplicationContext implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    /**
     * Returns the spring managed cps bean instance of the given class type if it exists.
     * Returns null otherwise.
     *
     * @param cpsBeanClass cps class type
     * @return requested bean instance
     */
    public static <T extends Object> T getCpsBean(final Class<T> cpsBeanClass) {
        return applicationContext.getBean(cpsBeanClass);
    }

    @Override
    public void setApplicationContext(final ApplicationContext cpsApplicationContext) {
        setCpsApplicationContext(cpsApplicationContext);
    }

    private static synchronized void setCpsApplicationContext(final ApplicationContext cpsApplicationContext) {
        CpsApplicationContext.applicationContext = cpsApplicationContext;
    }
}
