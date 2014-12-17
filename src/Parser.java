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
      String message = errMsg({type}, {attr});
      tokens.add(Token.syntaxErr(message, t.position));
    }
  }

  private String errMsg(Token.Type[] types, Object[] attrs) {
    String result = "Expected ";
    for (int i = 0; i < types.length; i++) {
      result += (i > 0) ? "," : "";
      result += "{ " + types[i].toString() + " " + Token.getAttribute(types[i], attrs[i]) + " }";
    }
    result += "encountered {" + t.type.toString() + " " + t.getAttributes();
    return result;
  }


  public void program() {
    switch (t.type) {

    }
  }
}
