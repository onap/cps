/*
 * ============LICENSE_START=======================================================
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

grammar CpsPath;

cpsPath: (cpsPathWithSingleLeafCondition | cpsPathWithDescendant | cpsPathWithDescendantAndLeafConditions) ancestorAxis?;

ancestorAxis: SLASH KW_ANCESTOR COLONCOLON ancestorPath;

ancestorPath: yangElement (SLASH yangElement)*;

cpsPathWithSingleLeafCondition: prefix singleValueCondition ;

/*
TODO EEITSIK This rule is to reflect current CPS impl. but it is neelsess restrictive
No need to ditinguish between cpsPathWithDescendant | cpsPathWithDescendantAndLeafConditions really!
But this requires refactoring of logic in CPS (wil do later)
*/

cpsPathWithDescendant: descendant;

cpsPathWithDescendantAndLeafConditions: descendant multipleValueConditions;

descendant: SLASH prefix;

prefix: (SLASH yangElement)* SLASH containerName;

yangElement: containerName listElementRef?;

containerName: ALPHABET (ALPHABET | NUMERIC | '-' )*;

listElementRef: multipleValueConditions;

singleValueCondition: '[' leafCondition ']';

multipleValueConditions: '[' leafCondition (' and ' leafCondition)* ']';

leafCondition: '@' leafName '=' (intValue | stringValue );

leafName: ALPHABET (ALPHABET | NUMERIC | '-' )*;

stringValue: SQSTRING | DQSTRING;

intValue: NUMERIC+;

/*
 * Lexer Rules
 */

SQSTRING: '\'' .*? '\'';
DQSTRING: '"' .*? '"';

NUMERIC: [0-9];
ALPHABET: [a-zA-Z];
SLASH: '/';
COLONCOLON : '::' ;

// KEYWORDS

KW_ANCESTOR : 'ancestor' ;

// Skip all Whitespace
Whitespace: ('\u000d' | '\u000a' | '\u0020' | '\u0009')+ -> skip ;

// handle characters which failed to match any other token (otherwise Antlr will ignore them)
ErrorCharacter : . ;
