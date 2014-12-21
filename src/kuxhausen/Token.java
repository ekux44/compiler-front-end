package kuxhausen;

/**
 * @author Eric Kuxhausen
 */
public class Token implements Cloneable {

  public TokType type;
  public Object attribute;
  public String lexeme;
  public SourcePointer position;

  public Token(TokType t, int attr, String lex, SourcePointer pos) {
    this(t, (Object) attr, lex, pos);
  }

  public Token(TokType t, String attr, String lex, SourcePointer pos) {
    this(t, (Object) attr, lex, pos);
  }

  private Token(TokType t, Object attr, String lex, SourcePointer pos) {
    type = t;
    attribute = attr;
    lexeme = lex;
    position = (pos != null) ? pos.clone() : null;
  }

  public Token clone() {
    return new Token(type, attribute, lexeme, position.clone());
  }

  public PasType getNumType() {
    if (type == TokType.NUM) {
      if (lexeme.contains("."))
        return PasType.REAL;
      else
        return PasType.INT;
    }
    return PasType.ERR;
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

  public RelopAttr getRelop() {
    return RelopAttr.values()[(int) attribute];
  }

  public MulopAttr getMulop() {
    return MulopAttr.values()[(int) attribute];
  }

  public AddopAttr getAddop() {
    return AddopAttr.values()[(int) attribute];
  }

  public boolean fullTypeMatch(Token other) {
    if (type == other.type) {
      // if one of these types, have to compare attributes as well
      if (type == TokType.RESWRD || type == TokType.RELOP || type == TokType.ADDOP
          || type == TokType.MULOP) {
        // unless the attribute wasn't specified, in which case it's a wildcard
        if ((int) attribute == -1 || (int) other.attribute == -1) {
          return true;
        }
        if (((int) attribute) == ((int) other.attribute)) {
          return true;
        }
      } else {
        return true;
      }
    }
    return false;
  }

  public static enum TokType {
    RESWRD, ID, DOT, NUM, RELOP, ADDOP, MULOP, LEXERR, SYNTAXERR, SEMANTICERR, OPENPAREN, CLOSEPAREN, SEMICOLON, COMMA, COLON, OPENBRACKET, DOTDOT, CLOSEBRACKET, ASSIGNOP, $
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
