/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.architecture;


import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.freeze.FreezingArchRule.freeze;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Test class responsible for layered architecture.
 */
@AnalyzeClasses(packages = "org.onap.cps", importOptions = {ImportOption.DoNotIncludeTests.class})
public class LayeredArchitectureTest {

    private static final String REST_CONTROLLER_PACKAGE = "org.onap.cps.rest..";
    private static final String NCMP_REST_PACKAGE = "org.onap.cps.ncmp.rest..";
    private static final String API_SERVICE_PACKAGE = "org.onap.cps.api..";
    private static final String SPI_SERVICE_PACKAGE = "org.onap.cps.spi..";
    private static final String NCMP_SERVICE_PACKAGE = "org.onap.cps.ncmp.api..";
    private static final String SPI_REPOSITORY_PACKAGE = "org.onap.cps.spi.repository..";
    private static final String YANG_SCHEMA_PACKAGE = "org.onap.cps.yang..";
    private static final String NOTIFICATION_PACKAGE = "org.onap.cps.notification..";
    private static final String CPS_UTILS_PACKAGE = "org.onap.cps.utils..";

    @ArchTest
    static final ArchRule restControllerShouldOnlyDependOnRestController =
        classes().that().resideInAPackage(REST_CONTROLLER_PACKAGE).should().onlyHaveDependentClassesThat()
            .resideInAPackage(REST_CONTROLLER_PACKAGE);

    @ArchTest
    static final ArchRule apiOrSpiServiceShouldOnlyBeDependedOnByControllerAndServices =
        freeze(classes().that().resideInAPackage(API_SERVICE_PACKAGE)
            .or().resideInAPackage(SPI_SERVICE_PACKAGE).should().onlyHaveDependentClassesThat()
            .resideInAnyPackage(REST_CONTROLLER_PACKAGE, API_SERVICE_PACKAGE, SPI_SERVICE_PACKAGE, NCMP_REST_PACKAGE,
                NCMP_SERVICE_PACKAGE, YANG_SCHEMA_PACKAGE, NOTIFICATION_PACKAGE, CPS_UTILS_PACKAGE));

    @ArchTest
    static final ArchRule repositoryShouldOnlyBeDependedOnByServicesAndRepository =
        classes().that().resideInAPackage(SPI_REPOSITORY_PACKAGE).should().onlyHaveDependentClassesThat()
            .resideInAnyPackage(API_SERVICE_PACKAGE, SPI_SERVICE_PACKAGE, SPI_REPOSITORY_PACKAGE);
}
