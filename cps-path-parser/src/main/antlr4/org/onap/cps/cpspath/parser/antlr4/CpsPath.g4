/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

grammar CpsPath ;

cpsPath: (cpsPathWithSingleLeafCondition | cpsPathWithDescendant | cpsPathWithDescendantAndLeafConditions) ancestorAxis? ;

ancestorAxis: SLASH KW_ANCESTOR COLONCOLON ancestorPath ;

ancestorPath: yangElement (SLASH yangElement)* ;

cpsPathWithSingleLeafCondition: prefix singleValueCondition postfix? ;

/*
No need to ditinguish between cpsPathWithDescendant | cpsPathWithDescendantAndLeafConditions really!
See https://jira.onap.org/browse/CPS-436
*/

cpsPathWithDescendant: descendant ;

cpsPathWithDescendantAndLeafConditions: descendant multipleValueConditions ;

descendant: SLASH prefix ;

prefix: (SLASH yangElement)* SLASH containerName ;

postfix: (SLASH yangElement)+ ;

yangElement: containerName listElementRef? ;

containerName: QName ;

listElementRef: multipleValueConditions ;

singleValueCondition: '[' leafCondition ']' ;

multipleValueConditions: '[' leafCondition (' and ' leafCondition)* ']' ;

leafCondition: '@' leafName '=' (IntegerLiteral | StringLiteral ) ;

//To Confirm: defintion of Lefname with external xPath grammar
leafName: QName ;

/*
 * Lexer Rules
 * Most of the lexer rules below are 'imporetd' from
 * https://raw.githubusercontent.com/antlr/grammars-v4/master/xpath/xpath31/XPath31.g4
 */

SLASH : '/';
COLONCOLON : '::' ;

// KEYWORDS

KW_ANCESTOR : 'ancestor' ;

IntegerLiteral : FragDigits ;
// Add below type definitions for leafvalue comparision in https://jira.onap.org/browse/CPS-440
DecimalLiteral : ('.' FragDigits) | (FragDigits '.' [0-9]*) ;
DoubleLiteral : (('.' FragDigits) | (FragDigits ('.' [0-9]*)?)) [eE] [+-]? FragDigits ;
StringLiteral : ('"' (FragEscapeQuot | ~[^"])*? '"') | ('\'' (FragEscapeApos | ~['])*? '\'') ;
fragment FragEscapeQuot : '""' ;
fragment FragEscapeApos : '\'';
fragment FragDigits : [0-9]+ ;

QName  : FragQName ;
NCName : FragmentNCName ;
fragment FragQName : FragPrefixedName | FragUnprefixedName ;
fragment FragPrefixedName : FragPrefix ':' FragLocalPart ;
fragment FragUnprefixedName : FragLocalPart ;
fragment FragPrefix : FragmentNCName ;
fragment FragLocalPart : FragmentNCName ;
fragment FragNCNameStartChar
  :  'A'..'Z'
  |  '_'
  | 'a'..'z'
  | '\u00C0'..'\u00D6'
  | '\u00D8'..'\u00F6'
  | '\u00F8'..'\u02FF'
  | '\u0370'..'\u037D'
  | '\u037F'..'\u1FFF'
  | '\u200C'..'\u200D'
  | '\u2070'..'\u218F'
  | '\u2C00'..'\u2FEF'
  | '\u3001'..'\uD7FF'
  | '\uF900'..'\uFDCF'
  | '\uFDF0'..'\uFFFD'
  | '\u{10000}'..'\u{EFFFF}'
  ;
fragment FragNCNameChar
  :  FragNCNameStartChar | '-' | '.' | '0'..'9'
  |  '\u00B7' | '\u0300'..'\u036F'
  |  '\u203F'..'\u2040'
  ;
fragment FragmentNCName : FragNCNameStartChar FragNCNameChar*  ;

// https://www.w3.org/TR/REC-xml/#NT-Char

fragment FragChar : '\u0009' | '\u000a' | '\u000d'
  | '\u0020'..'\ud7ff'
  | '\ue000'..'\ufffd'
  | '\u{10000}'..'\u{10ffff}'
 ;

// Skip all Whitespace
Whitespace : ('\u000d' | '\u000a' | '\u0020' | '\u0009')+ -> skip ;

// handle characters which failed to match any other token (otherwise Antlr will ignore them)
ErrorCharacter : . ;
