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

  public ArrayList<Token> getTokenList() {
    return mTokens;
  }

  private class ParErr extends Exception {
  }

  Token pair(Type type, Enum attr) {
    return new Token(type, (attr != null) ? attr.ordinal() : -1, null, null);
  }

  public void match(Type type, Enum attr) throws ParErr {
    Token desired = pair(type, attr);
    if (mT != null && mT.fullTypeMatch(desired)) {
      mT = mL.getNextToken();
    } else {
      Token[] toks = {pair(type, attr)};
      wanted(toks);
      throw new ParErr();
    }
  }

  private void wanted(Token[] wanted) {
    String message = generateErrorMessage(wanted);
    mTokens.add(Token.syntaxErr(message, mT.lexeme, mT.position));
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
    mSet = new Token[] {};

    try {
      switch (mT.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) mT.attribute]) {
            case VAR:
              declarations();
              programTailTail();
              return;
            case PROC:
              subprogramDeclarations();
              compoundStatement();
              match(Type.EOF, null);
              return;
            case BEGIN:
              compoundStatement();
              match(Type.EOF, null);
              return;
          }
          break;
      }

      Token[] toks =
          {pair(Type.RESWRD, ResWordAttr.VAR), pair(Type.RESWRD, ResWordAttr.PROC),
              pair(Type.RESWRD, ResWordAttr.BEGIN)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void programTailTail() {
    mSet = new Token[] {};

    try {
      switch (mT.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) mT.attribute]) {
            case PROC:
              subprogramDeclarations();
              compoundStatement();
              match(Type.EOF, null);
              return;
            case BEGIN:
              compoundStatement();
              match(Type.EOF, null);
              return;
          }
          break;
      }

      Token[] toks = {pair(Type.RESWRD, ResWordAttr.PROC), pair(Type.RESWRD, ResWordAttr.BEGIN)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void identifierList() {
    mSet = new Token[] {pair(Type.CLOSEPAREN, null)};

    try {
      switch (mT.type) {
        case ID:
          match(Type.ID, null);
          identifierListTail();
          return;
      }

      Token[] toks = {pair(Type.ID, null)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void identifierListTail() {
    mSet = new Token[] {pair(Type.CLOSEPAREN, null)};

    try {
      switch (mT.type) {
        case CLOSEPAREN:
          return;
        case COMMA:
          match(Type.COMMA, null);
          match(Type.ID, null);
          identifierListTail();
          return;
      }

      Token[] toks = {pair(Type.CLOSEPAREN, null), pair(Type.COMMA, null)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void declarations() {
    mSet = new Token[] {pair(Type.RESWRD, ResWordAttr.PROC), pair(Type.RESWRD, ResWordAttr.BEGIN)};

    try {
      switch (mT.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) mT.attribute]) {
            case VAR:
              match(Type.RESWRD, ResWordAttr.VAR);
              match(Type.ID, null);
              match(Type.COLON, null);
              type();
              match(Type.SEMICOLON, null);
              declarationsTail();
              return;
          }
          break;

      }

      Token[] toks = {pair(Type.RESWRD, ResWordAttr.VAR)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void declarationsTail() {
    mSet = new Token[] {pair(Type.RESWRD, ResWordAttr.PROC), pair(Type.RESWRD, ResWordAttr.BEGIN)};

    try {
      switch (mT.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) mT.attribute]) {
            case VAR:
              match(Type.RESWRD, ResWordAttr.VAR);
              match(Type.ID, null);
              match(Type.COLON, null);
              type();
              match(Type.SEMICOLON, null);
              return;
            case PROC:
              return;
            case BEGIN:
              return;
          }
          break;
      }

      Token[] toks =
          {pair(Type.RESWRD, ResWordAttr.VAR), pair(Type.RESWRD, ResWordAttr.PROC),
              pair(Type.RESWRD, ResWordAttr.BEGIN)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void type() {
    mSet = new Token[] {pair(Type.SEMICOLON, null), pair(Type.CLOSEPAREN, null)};

    try {
      switch (mT.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) mT.attribute]) {
            case ARRAY:
              match(Type.RESWRD, ResWordAttr.ARRAY);
              match(Type.OPENBRACKET, null);
              match(Type.NUM, null);
              match(Type.DOTDOT, null);
              match(Type.NUM, null);
              match(Type.RESWRD, ResWordAttr.OF);
              standardType();
              return;
            case INT_NAME:
              standardType();
              return;
            case REAL_NAME:
              standardType();
              return;
          }
          break;
      }

      Token[] toks =
          {pair(Type.RESWRD, ResWordAttr.ARRAY), pair(Type.RESWRD, ResWordAttr.INT_NAME),
              pair(Type.RESWRD, ResWordAttr.REAL_NAME)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void standardType() {
    mSet = new Token[] {pair(Type.SEMICOLON, null), pair(Type.CLOSEPAREN, null)};

    try {
      switch (mT.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) mT.attribute]) {
            case INT_NAME:
              match(Type.RESWRD, ResWordAttr.INT_NAME);
              return;
            case REAL_NAME:
              match(Type.RESWRD, ResWordAttr.REAL_NAME);
              return;
          }
          break;
      }

      Token[] toks =
          {pair(Type.RESWRD, ResWordAttr.INT_NAME), pair(Type.RESWRD, ResWordAttr.REAL_NAME)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void subprogramDeclarations() {
    mSet = new Token[] {pair(Type.RESWRD, ResWordAttr.BEGIN)};

    try {
      switch (mT.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) mT.attribute]) {
            case PROC:
              subprogramDeclaration();
              match(Type.SEMICOLON, null);
              subprogramDeclarationsTail();
              return;
          }
          break;
      }

      Token[] toks = {pair(Type.RESWRD, ResWordAttr.PROC)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void subprogramDeclarationsTail() {
    mSet = new Token[] {pair(Type.RESWRD, ResWordAttr.BEGIN)};

    try {
      switch (mT.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) mT.attribute]) {
            case PROC:
              subprogramDeclaration();
              match(Type.SEMICOLON, null);
              subprogramDeclarationsTail();
              return;
            case BEGIN:
              return;
          }
          break;
      }

      Token[] toks = {pair(Type.RESWRD, ResWordAttr.PROC), pair(Type.RESWRD, ResWordAttr.BEGIN)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void subprogramDeclaration() {
    mSet = new Token[] {pair(Type.SEMICOLON, null)};

    // try {
    switch (mT.type) {
      case RESWRD:
        switch (ResWordAttr.values()[(int) mT.attribute]) {
          case PROC:
            subprogramHead();
            subprogramDeclarationTail();
            return;
        }
        break;
    }

    Token[] toks = {pair(Type.RESWRD, ResWordAttr.PROC)};
    wanted(toks);
    sync();

    /*
     * Unreachable } catch (ParErr e) { sync(); }
     */
  }

  void subprogramDeclarationTail() {
    mSet = new Token[] {pair(Type.SEMICOLON, null)};

    // try {
    switch (mT.type) {
      case RESWRD:
        switch (ResWordAttr.values()[(int) mT.attribute]) {
          case VAR:
            declarations();
            subprogramDeclarationTailTail();
            return;
          case PROC:
            subprogramDeclarations();
            compoundStatement();
            return;
          case BEGIN:
            compoundStatement();
            return;
        }
        break;
    }

    Token[] toks =
        {pair(Type.RESWRD, ResWordAttr.VAR), pair(Type.RESWRD, ResWordAttr.PROC),
            pair(Type.RESWRD, ResWordAttr.BEGIN)};
    wanted(toks);
    sync();

    /*
     * Unreachable } catch (ParErr e) { sync(); }
     */
  }

  void subprogramDeclarationTailTail() {
    mSet = new Token[] {pair(Type.SEMICOLON, null)};

    // try {
    switch (mT.type) {
      case RESWRD:
        switch (ResWordAttr.values()[(int) mT.attribute]) {
          case PROC:
            subprogramDeclarations();
            compoundStatement();
            return;
          case BEGIN:
            compoundStatement();
            return;
        }
        break;
    }

    Token[] toks = {pair(Type.RESWRD, ResWordAttr.PROC), pair(Type.RESWRD, ResWordAttr.BEGIN)};
    wanted(toks);
    sync();

    /*
     * Unreachable } catch (ParErr e) { sync(); }
     */
  }

  void subprogramHead() {
    mSet =
        new Token[] {pair(Type.RESWRD, ResWordAttr.VAR), pair(Type.RESWRD, ResWordAttr.PROC),
            pair(Type.RESWRD, ResWordAttr.BEGIN)};

    try {
      switch (mT.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) mT.attribute]) {
            case PROC:
              match(Type.RESWRD, ResWordAttr.PROC);
              match(Type.ID, null);
              subprogramHeadTail();
              return;
          }
          break;
      }

      Token[] toks = {pair(Type.RESWRD, ResWordAttr.PROC)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void subprogramHeadTail() {
    mSet =
        new Token[] {pair(Type.RESWRD, ResWordAttr.VAR), pair(Type.RESWRD, ResWordAttr.PROC),
            pair(Type.RESWRD, ResWordAttr.BEGIN)};

    try {
      switch (mT.type) {
        case OPENPAREN:
          arguments();
          match(Type.SEMICOLON, null);
          return;
        case SEMICOLON:
          match(Type.SEMICOLON, null);
          return;
      }

      Token[] toks = {pair(Type.OPENPAREN, null), pair(Type.SEMICOLON, null)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void arguments() {
    mSet = new Token[] {pair(Type.SEMICOLON, null)};

    try {
      switch (mT.type) {
        case OPENPAREN:
          match(Type.OPENPAREN, null);
          parameterList();
          match(Type.CLOSEPAREN, null);
          return;
      }

      Token[] toks = {pair(Type.OPENPAREN, null)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void parameterList() {
    mSet = new Token[] {pair(Type.CLOSEPAREN, null)};

    try {
      switch (mT.type) {
        case ID:
          match(Type.ID, null);
          match(Type.COLON, null);
          type();
          parameterListTail();
          return;
      }

      Token[] toks = {pair(Type.ID, null)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void parameterListTail() {
    mSet = new Token[] {pair(Type.CLOSEPAREN, null)};

    try {
      switch (mT.type) {
        case CLOSEPAREN:
          return;
        case SEMICOLON:
          match(Type.SEMICOLON, null);
          match(Type.ID, null);
          match(Type.COLON, null);
          type();
          parameterListTail();
          return;
      }

      Token[] toks = {pair(Type.CLOSEPAREN, null), pair(Type.SEMICOLON, null)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void compoundStatement() {
    mSet = new Token[] {pair(Type.EOF, null), pair(Type.SEMICOLON, null)};

    try {
      switch (mT.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) mT.attribute]) {
            case BEGIN:
              match(Type.RESWRD, ResWordAttr.BEGIN);
              compoundStatementTail();
              return;
          }
          break;
      }

      Token[] toks = {pair(Type.RESWRD, ResWordAttr.BEGIN)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void compoundStatementTail() {
    mSet = new Token[] {pair(Type.EOF, null), pair(Type.SEMICOLON, null)};

    try {
      switch (mT.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) mT.attribute]) {
            case BEGIN:
              optionalStatements();
              match(Type.RESWRD, ResWordAttr.END);
              return;
            case END:
              match(Type.RESWRD, ResWordAttr.END);
              return;
            case IF:
              optionalStatements();
              match(Type.RESWRD, ResWordAttr.END);
              return;
            case WHILE:
              optionalStatements();
              match(Type.RESWRD, ResWordAttr.END);
              return;
            case CALL:
              optionalStatements();
              match(Type.RESWRD, ResWordAttr.END);
          }
          break;
        case ID:
          optionalStatements();
          match(Type.RESWRD, ResWordAttr.END);
      }

      Token[] toks =
          {pair(Type.RESWRD, ResWordAttr.BEGIN), pair(Type.RESWRD, ResWordAttr.END),
              pair(Type.RESWRD, ResWordAttr.IF), pair(Type.RESWRD, ResWordAttr.WHILE),
              pair(Type.RESWRD, ResWordAttr.CALL)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
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
