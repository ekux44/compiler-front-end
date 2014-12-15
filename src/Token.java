
public class Token {

  public Type type;
  public Object attribute;
  public String lexeme;
  public int lineNum;

  public Token(Type t, Object attr, String lex, int lineNo) {
    type = t;
    attribute = attr;
    lexeme = lex;
    lineNum = lineNo;
  }

  public static enum Type {
    RESWRD, ID, EOF, DOTDOT, NUM, RELOP, ADDOP, MULOP, LEXERR, OPENPAREN, CLOSEPAREN, SEMICOLON, COMMA, OPENBRACKET, CLOSEBRACKET, 
  }
  
  public static enum ResWordAttr {
    PROGRAM, VAR, COLON, ARRAY, OF, INT_NAME, REAL_NAME, PROC, BEGIN, END, ASSIGNOP, IF, THEN, ELSE, WHILE, DO, CALL, NOT
  }

  public static enum RelopAttr {
    EQ, NEQ, LT, LTE, GTE, GT
  }

  public static enum AddopAttr {
    PLUS, MINUS, OR
  }

  public static enum MulopAttr {
    TIMES, SLASH, DIV, MOD, AND
  }
}
