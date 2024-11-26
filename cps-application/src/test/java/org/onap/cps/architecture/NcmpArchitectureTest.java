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

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "org.onap.cps", importOptions = {ImportOption.DoNotIncludeTests.class})
public class NcmpArchitectureTest extends ArchitectureTestBase {

    @ArchTest
    static final ArchRule nothingDependsOnCpsNcmpRest =
            classes().that().resideInAPackage("org.onap.cps.ncmp.rest..").should().onlyHaveDependentClassesThat()
                    .resideInAPackage("org.onap.cps.ncmp.rest..");

    @ArchTest
    static final ArchRule ncmpRestControllerShouldOnlyDependOnNcmpService =
            classes().that().resideInAPackage("org.onap.cps.ncmp.rest..")
                    .should()
                    .onlyDependOnClassesThat()
                    .resideInAnyPackage(commonAndListedPackages("org.onap.cps.ncmp.api..",
                                                                "org.onap.cps.ncmp.rest..",
                                                                "org.onap.cps.api..",
                                                                // Below packages are breaking the agreed dependencies
                                                                // and need to be removed from this rule.
                                                                // This will be handled in a separate user story
                                                                "org.onap.cps.utils..",
                                                                "org.onap.cps.ncmp.impl.."));

    @ArchTest
    static final ArchRule ncmpServiceApiShouldOnlyDependOnThirdPartyPackages =
            classes().that().resideInAPackage("org.onap.cps.ncmp.api..").should().onlyDependOnClassesThat()
                    .resideInAnyPackage(commonAndListedPackages("org.onap.cps.api..",
                                                                // Below packages are breaking the agreed dependencies
                                                                // and need to be removed from this rule.
                                                                // This will be handled in a separate user story
                                                                "org.onap.cps.ncmp.api..",
                                                                "org.onap.cps.ncmp.impl..",
                                                                "org.onap.cps.ncmp.config",
                                                                "org.onap.cps.utils.."));

    @ArchTest
    static final ArchRule ncmpServiceImplShouldOnlyDependOnCpsServiceAndNcmpEvents =
            classes().that().resideInAPackage("org.onap.cps.ncmp.impl..").should().onlyDependOnClassesThat()
                    .resideInAnyPackage(commonAndListedPackages("org.onap.cps.api..",
                                                                "org.onap.cps.ncmp.api..",
                                                                "org.onap.cps.ncmp.impl..",
                                                                "org.onap.cps.ncmp.event..",
                                                                "org.onap.cps.ncmp.events..",
                                                                "org.onap.cps.ncmp.utils..",
                                                                "org.onap.cps.ncmp.config..",
                                                                "org.onap.cps.ncmp.exceptions..",
                                                                "org.onap.cps.spi.api..",
                                                                // Below packages are breaking the agreed dependencies
                                                                // and need to be removed from this rule.
                                                                // This will be handled in a separate user story
                                                                "org.onap.cps.cpspath..",
                                                                "org.onap.cps.events..",
                                                                "org.onap.cps.impl..",
                                                                "org.onap.cps.utils.."));
}

