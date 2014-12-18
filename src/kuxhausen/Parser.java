package kuxhausen;

import java.util.ArrayList;

import kuxhausen.Token.*;

/**
 * @author Eric Kuxhausen
 */
public class Parser {

  private Lexar mL;

  /**
   * current Token
   */
  private Token mT;

  /**
   * sync set for the current nonTerminal
   */
  private Token[] mSet;

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

  private class ParErr extends Exception {
  }

  Token pair(Type type, Enum attr) {
    return new Token(type, (attr != null) ? attr.ordinal() : null, null, null);
  }

  public void match(Type type, Enum attr) throws ParErr {
    Token desired = pair(type, attr);
    if (mT!=null && mT.fullTypeMatch(desired)) {
      mT = mL.getNextToken();
    } else {
      Token[] toks = {pair(type, attr)};
      wanted(toks);
      throw new ParErr();
    }
  }

  private void wanted(Token[] wanted) {
    String message = generateErrorMessage(wanted);
    mTokens.add(Token.syntaxErr(message, mT.position));
  }

  private String generateErrorMessage(Token[] tokens) {
    String result = "Expected ";
    for (int i = 0; i < tokens.length; i++) {
      result += (i > 0) ? "," : "";
      result += "{ " + tokens[i].type.toString() + " " + tokens[i].getAttribute() + " }";
    }
    result += "encountered {" + mT.type.toString() + " " + mT.getAttribute();
    return result;
  }

  private void sync() {
    while (mT != null && !inSet(mSet)) {
      mT = mL.getNextToken();
    }
  }

  private boolean inSet(Token[] syncSet) {
    for (Token s : syncSet) {
      if (mT.fullTypeMatch(s))
        return true;
    }
    return false;
  }

  void program() {
    mSet = new Token[] {};

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

      Token[] toks = {pair(Type.RESWRD, ResWordAttr.PROGRAM)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
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
