package kuxhausen;

import java.util.ArrayList;

import kuxhausen.Token.*;

/**
 * @author Eric Kuxhausen
 */
public class Parser {

  private Lexar mL;
  private Token mT;
  private ArrayList<Token> mTokens = new ArrayList<Token>();

  Parser(Lexar lex) {
    mL = lex;
    mT = mL.getNextToken();
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
    if (type == mT.type) {
      // TODO


      mT = mL.getNextToken();
    } else {
      Type[] types = {type};
      Object[] attrs = {attr};
      String message = errMsg(types, attrs);
      mTokens.add(Token.syntaxErr(message, mT.position));
      throw new ParErr();
    }
  }

  private String errMsg(Type[] types, Object[] attrs) {
    String result = "Expected ";
    for (int i = 0; i < types.length; i++) {
      result += (i > 0) ? "," : "";
      result += "{ " + types[i].toString() + " " + Token.getAttribute(types[i], attrs[i]) + " }";
    }
    result += "encountered {" + mT.type.toString() + " " + mT.getAttribute();
    return result;
  }

  private void sync(Token[] syncSet) {
    while (mT != null && !inSet(syncSet)) {
      mT = mL.getNextToken();
    }
  }

  private boolean inSet(Token[] syncSet) {
    for (Token s : syncSet) {
      if (mT.type == s.type) {
        // if one of these types, have to compare attributes as well
        if (mT.type == Type.RESWRD || mT.type == Type.RELOP || mT.type == Type.ADDOP
            || mT.type == Type.MULOP) {
          if (((int) mT.attribute) == ((int) s.attribute)) {
            return true;
          }
        } else {
          return true;
        }
      }
    }
    return false;
  }

  void program() {
    try {
      switch (mT.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) mT.attribute]) {
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
