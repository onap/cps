/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
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
public class CpsArchitectureTest extends ArchitectureTestBase {

    @ArchTest
    static final ArchRule cpsRestControllerShouldOnlyDependOnCpsService =
            classes().that().resideInAPackage("org.onap.cps.rest..").should().onlyDependOnClassesThat()
                    .resideInAnyPackage(commonAndListedPackages("org.onap.cps.rest..",
                                                                "org.onap.cps.api..",
                                                                "org.onap.cps.utils.."));

    @ArchTest
    static final ArchRule cpsServiceApiShouldNotDependOnAnything =
            classes().that().resideInAPackage("org.onap.cps.api.").should().onlyDependOnClassesThat()
                    .resideInAnyPackage(commonAndListedPackages()).allowEmptyShould(true);

    @ArchTest
    static final ArchRule cpsServiceImplShouldDependOnServiceAndEventsAndPathParserPackages =
            classes().that().resideInAPackage("org.onap.cps.impl..").should().onlyDependOnClassesThat()
                    .resideInAnyPackage(commonAndListedPackages("org.onap.cps.api..",
                                                                "org.onap.cps.impl..",
                                                                "org.onap.cps.events..",
                                                                "org.onap.cps.impl.utils..",
                                                                "org.onap.cps.spi..",
                                                                "org.onap.cps.utils..",
                                                                "org.onap.cps.cpspath.parser..",
                                                                "org.onap.cps.yang.."));

    @ArchTest
    static final ArchRule cpsReferenceImplShouldHaveNoDependants =
            classes().that().resideInAPackage("org.onap.cps.ri..").should().onlyHaveDependentClassesThat()
                    .resideInAnyPackage("org.onap.cps.ri..");

    @ArchTest
    static final ArchRule referenceImplShouldOnlyHaveDependantsInReferenceImpl =
            classes().that().resideInAPackage("org.onap.cps.ri.repository..").should().onlyHaveDependentClassesThat()
                    .resideInAnyPackage("org.onap.cps.ri..");
}
