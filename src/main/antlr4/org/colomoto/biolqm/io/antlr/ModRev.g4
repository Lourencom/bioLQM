grammar ModRev;

// Define the starting rule
model: (statement)+ EOF;

// A statement can be one of the four types
statement: vertex | functionOr | functionAnd | edge;

// Define the structure of each statement
vertex: 'vertex(' ID ').' ;
functionOr: 'functionOr(' ID ',' range ').' ;
functionAnd: 'functionAnd(' ID ',' INT ',' ID ').' ;
edge: 'edge(' ID ',' ID ',' INT ').' ;

// Define 'range' to handle single integers or ranges (e.g., 1..2)
range: INT ('..' INT)? ;

// Lexer rules
ID: '\''? [a-zA-Z]+ '\''?;
INT: [0-9]+ ; // Integer numbers
WS: [ \t\r\n]+ -> skip ; // Skip whitespaces, tabs, newlines
