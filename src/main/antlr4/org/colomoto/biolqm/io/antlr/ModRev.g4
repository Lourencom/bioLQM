grammar ModRev;

// Skip whitespaces, tabs, newlines
WS: [ \t\r\n]+ -> skip ; 
COMMENT: '%' ~('\r' | '\n')+ -> skip;

// Define the starting rule
model: (statement)+ EOF;

// A statement can be one of the four types
statement: vertex | functionOr | functionAnd | edge | fixed | input;

// Define the structure of each statement
vertex: 'vertex(' ID ').' ;
functionOr: 'functionOr(' ID ',' range ').' ;
functionAnd: 'functionAnd(' ID ',' INT ',' ID ').' ;
edge: 'edge(' ID ',' ID ',' INT ').' ;
fixed: 'fixed(' ID ').' ;
input: 'input(' ID ').' ;

// Define 'range' to handle single integers or ranges (e.g., 1..2)
range: INT ('..' INT)? ;

// Lexer rules
INT: [0-9]+ ; // Integer numbers
ID:  '"' [a-z0-9A-Z]+ '"' | [a-z0-9A-Z]+;

