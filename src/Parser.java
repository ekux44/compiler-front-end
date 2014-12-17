import java.util.ArrayList;

public class Parser {

  private Lexar l;
  private Token t;
  private ArrayList<Token> tokens = new ArrayList<Token>();

  Parser(Lexar lex) {
    l = lex;
    t = l.getNextToken();

  }

  public void writeListingFile(String string) {
    // TODO Auto-generated method stub

  }

  public void writeTokenFile(String string) {
    // TODO Auto-generated method stub

  }

  public void match(Token.Type type, Object attr) {
    if (type == t.type) {
      // TODO


      t = l.getNextToken();
    } else {
      // tokens.add(new Token(Type.SYNTAXERR, "Expected "))
    }
  }

  public void program() {
    switch (t.type) {

    }
  }
}
