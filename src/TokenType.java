public class TokenType {

  public TokenType(ReservedWordTypes word) {

  }

  enum ReservedWordTypes {
    PROGRAM, OPENPAREN, CLOSEPAREN, SEMICOLON, COMMA, VAR, COLON, ARRAY, OPENBRACKET, CLOSEBRACKET, OF, INT_NAME, REAL_NAME, PROC, BEGIN, END, ASSIGNOP, IF, THEN, ELSE, WHILE, DO, CALL, NOT
  }
  
  enum OtherTypes {
    EOF, DOTDOT, NUM, RELOP, ADDOP, MULOP
  }
  
  enum RelopAttributes {
    EQ, NEQ, LT, LTE, GTE, GT
  }
  
  enum AddopAttributes {
    PLUS, MINUS, OR
  }
  
  enum MulopAttributes {
    TIMES, SLASH, DIV, MOD, AND
  }

}
