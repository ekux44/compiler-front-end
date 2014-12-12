public class TokenType {

  // only one of these will be non-null at a time
  private ReservedWordTypes rwt = null;
  private OtherTypes ot = null;

  public TokenType(ReservedWordTypes word) {
    rwt = word;
  }

  public TokenType(OtherTypes type) {
    ot = type;
  }

  enum ReservedWordTypes {
    PROGRAM, OPENPAREN, CLOSEPAREN, SEMICOLON, COMMA, VAR, COLON, ARRAY, OPENBRACKET, CLOSEBRACKET, OF, INT_NAME, REAL_NAME, PROC, BEGIN, END, ASSIGNOP, IF, THEN, ELSE, WHILE, DO, CALL, NOT
  }

  enum OtherTypes {
    ID, EOF, DOTDOT, NUM, RELOP, ADDOP, MULOP
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
