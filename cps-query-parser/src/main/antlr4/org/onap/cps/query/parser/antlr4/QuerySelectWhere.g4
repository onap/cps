grammar QuerySelectWhere;

query
    : selectClause whereClause? EOF
    ;

selectClause
    : KW_SELECT identifier (COMMA identifier)*
    ;

whereClause
    : KW_WHERE condition
    ;

condition
    : condition booleanOperator condition
    | OP condition CP
    | predicate
    ;

predicate
    : identifier comparativeOperator value
    ;

comparativeOperator
    : EQ | GT | LT | GE | LE | KW_LIKE
    ;

booleanOperator
    : KW_AND | KW_OR
    ;

identifier
    : ID
    ;

value
    : IntegerLiteral
    | DecimalLiteral
    | StringLiteral
    ;

KW_SELECT : 'SELECT' ;
KW_WHERE : 'WHERE' ;
KW_AND : 'and' ;
KW_OR : 'or' ;
KW_LIKE : 'LIKE' ;

EQ : '=' ;
GT : '>' ;
LT : '<' ;
GE : '>=' ;
LE : '<=' ;
COMMA : ',' ;
OP : '(' ;
CP : ')' ;

IntegerLiteral : FragDigits ;
DecimalLiteral : ('.' FragDigits) | (FragDigits '.' [0-9]*) ;
StringLiteral : '\'' (~['] | FragEscapeApos)* '\'' ;
fragment FragEscapeApos : '\'\'';
fragment FragDigits : [0-9]+ ;

ID : FragNCName ;
fragment FragNCName : FragNCNameStartChar FragNCNameChar* ;
fragment FragNCNameStartChar
    : 'A'..'Z' | '_' | 'a'..'z'
    | '\u00C0'..'\u00D6' | '\u00D8'..'\u00F6'
    | '\u00F8'..'\u02FF' | '\u0370'..'\u037D'
    | '\u037F'..'\u1FFF' | '\u200C'..'\u200D'
    | '\u2070'..'\u218F' | '\u2C00'..'\u2FEF'
    | '\u3001'..'\uD7FF' | '\uF900'..'\uFDCF'
    | '\uFDF0'..'\uFFFD' | '\u{10000}'..'\u{EFFFF}'
    ;
fragment FragNCNameChar
    : FragNCNameStartChar | '-' | '.' | '0'..'9'
    | '\u00B7' | '\u0300'..'\u036F' | '\u203F'..'\u2040'
    ;

Whitespace : ('\u000d' | '\u000a' | '\u0020' | '\u0009')+ -> skip ;
ErrorCharacter : . ;