grammar CpsPath;

cpsPath: (cpsPathWithSingleLeafCondition | cpsPathWithDescendant | cpsPathWithDescendantAndLeafConditions);

cpsPathWithSingleLeafCondition: prefix singleValueCondition;

cpsPathWithDescendant: descendant;

cpsPathWithDescendantAndLeafConditions: descendant multipleValueConditions;

descendant: SEPARATOR prefix;

prefix: yangContainer+;

yangContainer: SEPARATOR containerName;

containerName: ALPHABET (ALPHABET | NUMERIC | '-' )*;

singleValueCondition: '[@' leafCondition ']';

multipleValueConditions: '[@' leafCondition (' and ' leafCondition)* ']';

leafCondition: leafName '=' (intValue | stringValue);

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
SEPARATOR: '/';

// Skip all Whitespace
WS: [ \n\r\t]+ -> skip ;
