grammar CpsPath;

cpsPath: cpsPathWithSingleLeafCondition | cpsPathWithDescendant | cpsPathWithDescendantAndLeafConditions ;

ancestorAxis: ANCESTOR ancestorPath;

ancestorPath: yangElement (SEPARATOR yangElement)*;

cpsPathWithSingleLeafCondition: prefix singleValueCondition ancestorAxis?;

/*
TODO EEITSIK This rule is to reflect current CPS impl. but it is neelsess restrictive
No need to ditinguish between cpsPathWithDescendant | cpsPathWithDescendantAndLeafConditions really!
But this requires refactoring of logic in CPS (wil do later)
*/

cpsPathWithDescendant: descendant ancestorAxis?;

cpsPathWithDescendantAndLeafConditions: descendant multipleValueConditions ancestorAxis?;

descendant: SEPARATOR prefix;

prefix: (SEPARATOR yangElement)* SEPARATOR containerName;

yangElement: containerName listElementRef?;

containerName: ALPHABET (ALPHABET | NUMERIC | '-' )*;

listElementRef: multipleValueConditions;

singleValueCondition: '[' leafCondition ']';

multipleValueConditions: '[' leafCondition (' and ' leafCondition)* ']';

leafCondition: '@' leafName '=' (intValue | stringValue | floatValue);

leafName: ALPHABET (ALPHABET | NUMERIC | '-' )*;

stringValue: SQSTRING | DQSTRING;

intValue: NUMERIC+;

floatValue: NUMERIC+ ('.' NUMERIC+)?;

/*
 * Lexer Rules
 */

ANCESTOR: '/ancestor::';

SQSTRING: '\'' .*? '\'';
DQSTRING: '"' .*? '"';

NUMERIC: [0-9];
ALPHABET: [a-zA-Z];
SEPARATOR: '/';

// Skip all Whitespace
WS: [ \n\r\t]+ -> skip ;

// handle characters which failed to match any other token (otherwise Antlr will ignore them)
ErrorCharacter : . ;
