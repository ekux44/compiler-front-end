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
    position = (pos != null) ? pos.clone() : null;
  }

  public static Token syntaxErr(String message, String lexeme, SourcePointer pos) {
    return new Token(Type.SYNTAXERR, message, lexeme, pos);
  }

  public Token clone() {
    return new Token(type, attribute, lexeme, position.clone());
  }

  public String getAttribute() {
    if (attribute != null) {
      if (attribute instanceof Integer && (int) attribute != -1) {
        switch (type) {
          case RESWRD:
            return ResWordAttr.values()[(int) attribute].toString();
          case RELOP:
            return RelopAttr.values()[(int) attribute].toString();
          case ADDOP:
            return AddopAttr.values()[(int) attribute].toString();
          case MULOP:
            return MulopAttr.values()[(int) attribute].toString();
        }
      } else if (!(attribute instanceof Integer)) {
        return attribute.toString();
      }
    }
    return "NULL";
  }

  public boolean fullTypeMatch(Token other) {
    if (type == other.type) {
      // if one of these types, have to compare attributes as well
      if (type == Type.RESWRD || type == Type.RELOP || type == Type.ADDOP || type == Type.MULOP) {
        if (((int) attribute) == ((int) other.attribute)) {
          return true;
        }
      } else {
        return true;
      }
    }
    return false;
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
