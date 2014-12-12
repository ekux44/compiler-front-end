public class Token {

  public TokenType type;
  public Object attribute;

  public Token(TokenType t, Object attr) {
    type = t;
    attribute = attr;
  }
}
