package kuxhausen;

import java.util.ArrayList;

import kuxhausen.Token.*;

/**
 * @author Eric Kuxhausen
 */
public class Parser {

  private Lexar l;
  private Token t;
  private ArrayList<Token> tokens = new ArrayList<Token>();

  Parser(Lexar lex) {
    l = lex;
    t = l.getNextToken();
    program();
  }

  public void writeListingFile(String string) {
    // TODO Auto-generated method stub

  }

  public void writeTokenFile(String string) {
    // TODO Auto-generated method stub

  }

  class ParErr extends Exception {
  }

  public void match(Type type, Object attr) throws ParErr {
    if (type == t.type) {
      // TODO


      t = l.getNextToken();
    } else {
      Type[] types = {type};
      Object[] attrs = {attr};
      String message = errMsg(types, attrs);
      tokens.add(Token.syntaxErr(message, t.position));
      throw new ParErr();
    }
  }

  private String errMsg(Type[] types, Object[] attrs) {
    String result = "Expected ";
    for (int i = 0; i < types.length; i++) {
      result += (i > 0) ? "," : "";
      result += "{ " + types[i].toString() + " " + Token.getAttribute(types[i], attrs[i]) + " }";
    }
    result += "encountered {" + t.type.toString() + " " + t.getAttribute();
    return result;
  }


  void program() {
    try {
      switch (t.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) t.attribute]) {
            case PROGRAM:
              match(Type.RESWRD, ResWordAttr.PROGRAM);
              match(Type.ID, null);
              match(Type.OPENPAREN, null);
              identifierList();
              match(Type.CLOSEPAREN, null);
              programTail();
              return;
          }
          break;
      }
    } catch (ParErr e) {
    }
  }

  void programTail() {

  }

  void programTailTail() {

  }

  void identifierList() {

  }

  void identifierListTail() {

  }

  void declarations() {

  }

  void declarationsTail() {

  }

  void type() {

  }

  void standardType() {

  }

  void subprogramDeclarations() {

  }

  void subprogramDeclarationsTail() {

  }

  void subprogramDeclaration() {

  }

  void subprogramDeclarationTail() {

  }

  void subprogramDeclarationTailTail() {

  }

  void subprogramHead() {

  }

  void subprogramHeadTail() {

  }

  void arguments() {

  }

  void parameterList() {

  }

  void compoundStatement() {

  }

  void compoundStatementTail() {

  }

  void optionalStatements() {

  }

  void statementList() {

  }

  void statementListTail() {

  }

  void statement() {

  }

  void statementTail() {

  }

  void variable() {

  }

  void variableTail() {

  }

  void procedureStatment() {

  }

  void procedureStatementTail() {

  }

  void expressionList() {

  }

  void ExpressionListTail() {

  }

  void expression() {

  }

  void expressionTail() {

  }

  void simpleExpression() {

  }

  void simpleExpressionTail() {

  }

  void term() {

  }

  void termTail() {

  }

  void factor() {

  }

  void factorTail() {

  }

  void sign() {

  }
}
