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
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Test class responsible for layered architecture.
 */
@AnalyzeClasses(packages = "org.onap.cps", importOptions = {ImportOption.DoNotIncludeTests.class})
public class LayeredArchitectureTest {

    private static final String CONTROLLER = "controller";
    private static final String API_SERVICE = "apiService";
    private static final String SPI_SERVICE = "spiService";
    private static final String NCMP_CONTROLLER = "ncmpController";
    private static final String NCMP_SERVICE = "ncmpService";
    private static final String YANG_SCHEMA = "yangSchema";
    private static final String UTILS = "utils";
    private static final String REPOSITORY = "repository";

    private static final String REST_CONTROLLER_PACKAGE = "org.onap.cps.rest..";
    private static final String NCMP_REST_PACKAGE = "org.onap.cps.ncmp.rest..";
    private static final String API_SERVICE_PACKAGE = "org.onap.cps.api..";
    private static final String SPI_SERVICE_PACKAGE = "org.onap.cps.spi..";
    private static final String NCMP_SERVICE_PACKAGE =  "org.onap.cps.ncmp.api..";
    private static final String REPOSITORY_PACKAGE = "org.onap.cps.spi.repository..";
    private static final String UTILS_PACKAGE = "org.onap.cps.utils..";
    private static final String YANG_SCHEMA_PACKAGE = "org.onap.cps.yang..";

    @ArchTest
    static final ArchRule layeredArchitecturesRulesRespected = layeredArchitecture()
        .layer(CONTROLLER).definedBy(REST_CONTROLLER_PACKAGE)
        .layer(API_SERVICE).definedBy(API_SERVICE_PACKAGE)
        .layer(NCMP_SERVICE).definedBy(NCMP_SERVICE_PACKAGE)
        .layer(REPOSITORY).definedBy(REPOSITORY_PACKAGE)
        .layer(SPI_SERVICE).definedBy(SPI_SERVICE_PACKAGE)
        .layer(NCMP_CONTROLLER).definedBy(NCMP_REST_PACKAGE)
        .layer(YANG_SCHEMA).definedBy(YANG_SCHEMA_PACKAGE)
        .layer(UTILS).definedBy(UTILS_PACKAGE)
        .whereLayer(CONTROLLER).mayNotBeAccessedByAnyLayer()
        .whereLayer(API_SERVICE)
        .mayOnlyBeAccessedByLayers(CONTROLLER, SPI_SERVICE, NCMP_SERVICE,
            NCMP_CONTROLLER, YANG_SCHEMA, UTILS)
        .whereLayer(SPI_SERVICE).mayOnlyBeAccessedByLayers(CONTROLLER, API_SERVICE, NCMP_SERVICE,
            NCMP_CONTROLLER, YANG_SCHEMA, UTILS)
        .whereLayer(REPOSITORY).mayOnlyBeAccessedByLayers(API_SERVICE, SPI_SERVICE);

    @ArchTest
    static final ArchRule restControllerShouldOnlyDependOnRestController =
        classes().that().resideInAPackage(REST_CONTROLLER_PACKAGE).should().onlyHaveDependentClassesThat()
            .resideInAPackage(REST_CONTROLLER_PACKAGE);

    @ArchTest
    static final ArchRule apiServiceShouldNotDependOnController =
        noClasses().that().resideInAPackage(API_SERVICE_PACKAGE).or()
            .resideInAPackage(SPI_SERVICE_PACKAGE).should().dependOnClassesThat()
            .resideInAnyPackage(REST_CONTROLLER_PACKAGE);

    @ArchTest
    static final ArchRule apiOrSpiServiceShouldOnlyDependOnControllerAndServices =
        classes().that().resideInAPackage(API_SERVICE_PACKAGE).or()
            .resideInAPackage(SPI_SERVICE_PACKAGE).should().onlyHaveDependentClassesThat()
            .resideInAnyPackage(REST_CONTROLLER_PACKAGE, API_SERVICE_PACKAGE, SPI_SERVICE_PACKAGE, NCMP_REST_PACKAGE,
                NCMP_SERVICE_PACKAGE, UTILS_PACKAGE, YANG_SCHEMA_PACKAGE);

    @ArchTest
    static final ArchRule repositoryShouldOnlyDependOnServicesAndRepository =
        classes().that().resideInAPackage(REPOSITORY_PACKAGE).should().onlyHaveDependentClassesThat()
            .resideInAnyPackage(API_SERVICE_PACKAGE, SPI_SERVICE_PACKAGE, REPOSITORY_PACKAGE,
                UTILS_PACKAGE, YANG_SCHEMA_PACKAGE);
}
