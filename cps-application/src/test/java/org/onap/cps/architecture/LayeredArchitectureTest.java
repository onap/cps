/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation
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
import com.tngtech.archunit.junit.ArchIgnore;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Test class responsible for layered architecture.
 */
@AnalyzeClasses(packages = "org.onap.cps", importOptions = {ImportOption.DoNotIncludeTests.class})
public class LayeredArchitectureTest {

    @ArchTest
    static final ArchRule restControllerShouldOnlyDependOnRestController =
            classes().that().resideInAPackage("org.onap.cps.rest..").should().onlyHaveDependentClassesThat()
                    .resideInAPackage("org.onap.cps.rest..");

    @ArchTest
    @ArchIgnore
    static final ArchRule apiOrSpiServiceShouldOnlyBeDependedOnByControllerAndServicesAndCommonUtilityPackages =
            freeze(classes().that().resideInAPackage("org.onap.cps.api..")
                    .or().resideInAPackage("org.onap.cps.ri..").should().onlyHaveDependentClassesThat()
                    .resideInAnyPackage("org.onap.cps.rest..", "org.onap.cps.api..", "org.onap.cps.ri..",
                            "org.onap.cps.ncmp.rest..", "org.onap.cps.ncmp.api..", "org.onap.cps.yang..",
                            "org.onap.cps.notification..", "org.onap.cps.utils..", "org.onap.cps.ncmp.init..",
                            "org.onap.cps.cache..", "org.onap.cps.events.."));


    @ArchTest
    static final ArchRule repositoryShouldOnlyBeDependedOnByServicesAndRepository =
            classes().that().resideInAPackage("org.onap.cps.ri.repository..").should().onlyHaveDependentClassesThat()
                    .resideInAnyPackage("org.onap.cps.api..", "org.onap.cps.ri..", "org.onap.cps.ri.repository..");

    @ArchTest
    static final ArchRule ncmpRestControllerShouldOnlyDependOnService =
            classes().that().resideInAPackage("org.onap.cps.ncmp.rest..").should().onlyDependOnClassesThat()
                    .resideInAnyPackage("org.onap.cps.ncmp.rest..", "org.onap.cps.ncmp.api..", "org.onap.cps.api..",
                            // third party dependencies like Java, Lombok etc.
                            "io.swagger..", "org.mapstruct..", "java..", "org.springframework..",
                            "com.fasterxml..", "jakarta..", "lombok..", "org.slf4j..", "io.micrometer..",
                            // these packages are breaking the architecture rules
                            "org.onap.cps.spi..", "org.onap.cps.utils..", "org.onap.cps.ncmp.impl..");

    @ArchTest
    static final ArchRule ncmpServiceShouldOnlyDependOnCpsServiceAndUtils =
            classes().that().resideInAPackage("org.onap.cps.ncmp.api..").should().onlyDependOnClassesThat()
                    .resideInAnyPackage("org.onap.cps.ncmp.api..", "org.onap.cps.ncmp.impl..",
                            "org.onap.cps.ncmp.config",
                            // might break the rules?
                            "org.onap.cps.api..", "org.onap.cps.spi..", "org.onap.cps.utils..",
                            // third party dependencies like Java, Lombok etc.
                            "io.swagger..", "org.mapstruct..", "java..", "org.springframework..", "com.google.common..",
                            "com.fasterxml..", "jakarta..", "lombok..", "org.slf4j..", "io.micrometer..");
}

