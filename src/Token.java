public class Token {

  public TokenType type;
  public Object attribute;
  public String lexeme;
  public int lineNum;

  public Token(TokenType t, Object attr, String lex, int lineNo) {
    type = t;
    attribute = attr;
    lexeme = lex;
    lineNum = lineNo;
  }
}
