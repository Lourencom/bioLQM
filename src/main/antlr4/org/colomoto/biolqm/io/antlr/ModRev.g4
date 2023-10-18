// Define a grammar called ModRev
grammar ModRev;

// Parser rules
model: statement* EOF;

statement: vertex | edge | functionOr | functionAnd;

vertex: 'vertex(' ID ').\n';
edge: 'edge(' ID ',' ID ',' INT ').\n';
functionOr: 'functionOr(' ID ', 1' range ').\n';
functionAnd: 'functionAnd(' ID ',' INT ',' ID ').\n';

range: ('..' INT)?;

// Lexer rules
ID: [a-zA-Z_] [a-zA-Z_0-9]*;
INT: [0-9]+;
WS: [ \t\n\r]+ -> skip;

