package kuxhausen;

/**
 * @author Eric Kuxhausen
 */
public class Token implements Cloneable {

  public Type type;
  public Object attribute;
  public String lexeme;
  public SourcePointer position;

  public Token(Type t, int attr, String lex, SourcePointer pos) {
    this(t, (Object) attr, lex, pos);
  }

  public Token(Type t, String attr, String lex, SourcePointer pos) {
    this(t, (Object) attr, lex, pos);
  }

  private Token(Type t, Object attr, String lex, SourcePointer pos) {
    type = t;
    attribute = attr;
    lexeme = lex;
    position = pos.clone();
  }

  public static Token syntaxErr(String message, SourcePointer pos) {
    return new Token(Type.SYNTAXERR, message, null, pos);
  }

  public Token clone() {
    return new Token(type, attribute, lexeme, position.clone());
  }

  public static String getAttribute(Type t, Object attr) {
    switch (t) {
      case RESWRD:
        return ResWordAttr.values()[(int) attr].toString();
      case RELOP:
        return RelopAttr.values()[(int) attr].toString();
      case ADDOP:
        return AddopAttr.values()[(int) attr].toString();
      case MULOP:
        return MulopAttr.values()[(int) attr].toString();
    }
    if (attr != null)
      return attr.toString();
    return "NULL";
  }

  public String getAttribute() {
    return Token.getAttribute(type, attribute);
  }

  public static enum Type {
    RESWRD, ID, EOF, NUM, RELOP, ADDOP, MULOP, LEXERR, SYNTAXERR, OPENPAREN, CLOSEPAREN, SEMICOLON, COMMA, COLON, OPENBRACKET, DOTDOT, CLOSEBRACKET, ASSIGNOP,
  }

  public static enum ResWordAttr {
    PROGRAM, VAR, ARRAY, OF, INT_NAME, REAL_NAME, PROC, BEGIN, END, IF, THEN, ELSE, WHILE, DO, CALL, NOT
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
