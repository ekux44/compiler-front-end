package kuxhausen;

import java.util.ArrayList;

import kuxhausen.Token.ResWordAttr;
import kuxhausen.Token.Type;
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
    result += "encountered { " + mT.type.toString() + " " + mT.getAttribute() + " }";
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
              match(Type.SEMICOLON, null);
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
              declarationsTail();
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
              match(Type.CLOSEBRACKET, null);
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
              return;
          }
          break;
        case ID:
          optionalStatements();
          match(Type.RESWRD, ResWordAttr.END);
          return;
      }

      Token[] toks =
          {pair(Type.RESWRD, ResWordAttr.BEGIN), pair(Type.RESWRD, ResWordAttr.END),
              pair(Type.RESWRD, ResWordAttr.IF), pair(Type.RESWRD, ResWordAttr.WHILE),
              pair(Type.RESWRD, ResWordAttr.CALL), pair(Type.ID, null)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void optionalStatements() {
    mSet = new Token[] {pair(Type.RESWRD, ResWordAttr.END)};

    // try {
    switch (mT.type) {
      case RESWRD:
        switch (ResWordAttr.values()[(int) mT.attribute]) {
          case BEGIN:
            statementList();
            return;
          case IF:
            statementList();
            return;
          case WHILE:
            statementList();
            return;
          case CALL:
            statementList();
            return;
        }
        break;
      case ID:
        statementList();
        return;
    }

    Token[] toks =
        {pair(Type.RESWRD, ResWordAttr.BEGIN), pair(Type.RESWRD, ResWordAttr.IF),
            pair(Type.RESWRD, ResWordAttr.WHILE), pair(Type.RESWRD, ResWordAttr.CALL),
            pair(Type.ID, null)};
    wanted(toks);
    sync();

    /*
     * Unreachable } catch (ParErr e) { sync(); }
     */
  }

  void statementList() {
    mSet = new Token[] {pair(Type.RESWRD, ResWordAttr.END)};

    // try {
    switch (mT.type) {
      case RESWRD:
        switch (ResWordAttr.values()[(int) mT.attribute]) {
          case BEGIN:
            statement();
            statementListTail();
            return;
          case IF:
            statement();
            statementListTail();
            return;
          case WHILE:
            statement();
            statementListTail();
            return;
          case CALL:
            statement();
            statementListTail();
            return;
        }
        break;
      case ID:
        statement();
        statementListTail();
        return;
    }

    Token[] toks =
        {pair(Type.RESWRD, ResWordAttr.BEGIN), pair(Type.RESWRD, ResWordAttr.IF),
            pair(Type.RESWRD, ResWordAttr.WHILE), pair(Type.RESWRD, ResWordAttr.CALL),
            pair(Type.ID, null)};
    wanted(toks);
    sync();

    /*
     * Unreachable } catch (ParErr e) { sync(); }
     */
  }

  void statementListTail() {
    mSet = new Token[] {pair(Type.RESWRD, ResWordAttr.END)};

    try {
      switch (mT.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) mT.attribute]) {
            case END:
              return;
          }
          break;
        case SEMICOLON:
          match(Type.SEMICOLON, null);
          statement();
          statementListTail();
          return;
      }

      Token[] toks = {pair(Type.RESWRD, ResWordAttr.END), pair(Type.SEMICOLON, null)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void statement() {
    mSet =
        new Token[] {pair(Type.SEMICOLON, null), pair(Type.RESWRD, ResWordAttr.END),
            pair(Type.RESWRD, ResWordAttr.ELSE)};

    try {
      switch (mT.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) mT.attribute]) {
            case BEGIN:
              compoundStatement();
              return;
            case IF:
              match(Type.RESWRD, ResWordAttr.IF);
              expression();
              match(Type.RESWRD, ResWordAttr.THEN);
              statement();
              statementTail();
              return;
            case WHILE:
              match(Type.RESWRD, ResWordAttr.WHILE);
              expression();
              match(Type.RESWRD, ResWordAttr.DO);
              statement();
              return;
            case CALL:
              procedureStatment();
              return;
          }
          break;
        case ID:
          variable();
          match(Type.ASSIGNOP, null);
          expression();
          return;
      }

      Token[] toks =
          {pair(Type.RESWRD, ResWordAttr.BEGIN), pair(Type.RESWRD, ResWordAttr.IF),
              pair(Type.RESWRD, ResWordAttr.WHILE), pair(Type.RESWRD, ResWordAttr.CALL),
              pair(Type.ID, null)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void statementTail() {
    mSet =
        new Token[] {pair(Type.SEMICOLON, null), pair(Type.RESWRD, ResWordAttr.END),
            pair(Type.RESWRD, ResWordAttr.ELSE)};

    try {
      switch (mT.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) mT.attribute]) {
            case END:
              return;
            case ELSE:
              match(Type.RESWRD, ResWordAttr.ELSE);
              statement();
              return;
          }
          break;
        case SEMICOLON:
          return;
      }

      Token[] toks =
          {pair(Type.RESWRD, ResWordAttr.END), pair(Type.RESWRD, ResWordAttr.ELSE),
              pair(Type.SEMICOLON, null)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void variable() {
    mSet = new Token[] {pair(Type.ASSIGNOP, null)};

    try {
      switch (mT.type) {
        case ID:
          match(Type.ID, null);
          variableTail();
          return;
      }

      Token[] toks = {pair(Type.ID, null)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void variableTail() {
    mSet = new Token[] {pair(Type.ASSIGNOP, null)};

    try {
      switch (mT.type) {
        case OPENBRACKET:
          match(Type.OPENBRACKET, null);
          expression();
          match(Type.CLOSEBRACKET, null);
          return;
        case ASSIGNOP:
          return;
      }

      Token[] toks = {pair(Type.OPENBRACKET, null), pair(Type.ASSIGNOP, null)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void procedureStatment() {
    mSet =
        new Token[] {pair(Type.SEMICOLON, null), pair(Type.RESWRD, ResWordAttr.END),
            pair(Type.RESWRD, ResWordAttr.ELSE)};

    try {
      switch (mT.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) mT.attribute]) {
            case CALL:
              match(Type.RESWRD, ResWordAttr.CALL);
              match(Type.ID, null);
              procedureStatementTail();
              return;
          }
          break;
      }

      Token[] toks = {pair(Type.RESWRD, ResWordAttr.CALL)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void procedureStatementTail() {
    mSet =
        new Token[] {pair(Type.SEMICOLON, null), pair(Type.RESWRD, ResWordAttr.END),
            pair(Type.RESWRD, ResWordAttr.ELSE)};

    try {
      switch (mT.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) mT.attribute]) {
            case END:
              return;
            case ELSE:
              return;
          }
          break;
        case OPENPAREN:
          match(Type.OPENPAREN, null);
          expressionList();
          match(Type.CLOSEPAREN, null);
          return;
        case SEMICOLON:
          return;
      }

      Token[] toks =
          {pair(Type.RESWRD, ResWordAttr.END), pair(Type.RESWRD, ResWordAttr.ELSE),
              pair(Type.OPENPAREN, null), pair(Type.SEMICOLON, null)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void expressionList() {
    mSet = new Token[] {pair(Type.CLOSEPAREN, null)};

    // try {
    switch (mT.type) {
      case RESWRD:
        switch (ResWordAttr.values()[(int) mT.attribute]) {
          case NOT:
            expression();
            expressionListTail();
            return;
        }
        break;
      case OPENPAREN:
        expression();
        expressionListTail();
        return;
      case ADDOP:
        switch (AddopAttr.values()[(int) mT.attribute]) {
          case PLUS:
            expression();
            expressionListTail();
            return;
          case MINUS:
            expression();
            expressionListTail();
            return;
        }
        break;
      case ID:
        expression();
        expressionListTail();
        return;
      case NUM:
        expression();
        expressionListTail();
        return;
    }

    Token[] toks =
        {pair(Type.RESWRD, ResWordAttr.NOT), pair(Type.OPENPAREN, null),
            pair(Type.ADDOP, AddopAttr.PLUS), pair(Type.ADDOP, AddopAttr.MINUS),
            pair(Type.ID, null), pair(Type.NUM, null)};
    wanted(toks);
    sync();

    /*
     * Unreachable } catch (ParErr e) { sync(); }
     */
  }

  void expressionListTail() {
    mSet = new Token[] {pair(Type.CLOSEPAREN, null)};

    try {
      switch (mT.type) {
        case CLOSEPAREN:
          return;
        case COMMA:
          match(Type.COMMA, null);
          expression();
          expressionListTail();
          return;
      }

      Token[] toks = {pair(Type.CLOSEPAREN, null), pair(Type.COMMA, null)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void expression() {
    mSet =
        new Token[] {pair(Type.SEMICOLON, null), pair(Type.RESWRD, ResWordAttr.END),
            pair(Type.RESWRD, ResWordAttr.ELSE), pair(Type.RESWRD, ResWordAttr.THEN),
            pair(Type.CLOSEBRACKET, null), pair(Type.COMMA, null), pair(Type.CLOSEPAREN, null)};

    // try {
    switch (mT.type) {
      case RESWRD:
        switch (ResWordAttr.values()[(int) mT.attribute]) {
          case NOT:
            simpleExpression();
            expressionTail();
            return;
        }
        break;
      case OPENPAREN:
        simpleExpression();
        expressionTail();
        return;
      case ADDOP:
        switch (AddopAttr.values()[(int) mT.attribute]) {
          case PLUS:
            simpleExpression();
            expressionTail();
            return;
          case MINUS:
            simpleExpression();
            expressionTail();
            return;
        }
        break;
      case ID:
        simpleExpression();
        expressionTail();
        return;
      case NUM:
        simpleExpression();
        expressionTail();
        return;
    }

    Token[] toks =
        {pair(Type.RESWRD, ResWordAttr.NOT), pair(Type.OPENPAREN, null),
            pair(Type.ADDOP, AddopAttr.PLUS), pair(Type.ADDOP, AddopAttr.MINUS),
            pair(Type.ID, null), pair(Type.NUM, null)};
    wanted(toks);
    sync();

    /*
     * Unreachable } catch (ParErr e) { sync(); }
     */
  }

  void expressionTail() {
    mSet =
        new Token[] {pair(Type.SEMICOLON, null), pair(Type.RESWRD, ResWordAttr.END),
            pair(Type.RESWRD, ResWordAttr.ELSE), pair(Type.RESWRD, ResWordAttr.THEN),
            pair(Type.CLOSEBRACKET, null), pair(Type.COMMA, null), pair(Type.CLOSEPAREN, null)};

    try {
      switch (mT.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) mT.attribute]) {
            case END:
              return;
            case THEN:
              return;
            case ELSE:
              return;
            case DO:
              return;
          }
          break;
        case CLOSEPAREN:
          return;
        case SEMICOLON:
          return;
        case COMMA:
          return;
        case CLOSEBRACKET:
          return;
        case RELOP:
          match(Type.RELOP, null);
          simpleExpression();
          return;
      }

      Token[] toks =
          {pair(Type.RESWRD, ResWordAttr.END), pair(Type.RESWRD, ResWordAttr.THEN),
              pair(Type.RESWRD, ResWordAttr.ELSE), pair(Type.RESWRD, ResWordAttr.DO),
              pair(Type.CLOSEPAREN, null), pair(Type.SEMICOLON, null), pair(Type.COMMA, null),
              pair(Type.CLOSEBRACKET, null), pair(Type.RELOP, null)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void simpleExpression() {
    mSet =
        new Token[] {pair(Type.RELOP, null), pair(Type.SEMICOLON, null),
            pair(Type.RESWRD, ResWordAttr.END), pair(Type.RESWRD, ResWordAttr.ELSE),
            pair(Type.RESWRD, ResWordAttr.THEN), pair(Type.CLOSEBRACKET, null),
            pair(Type.COMMA, null), pair(Type.CLOSEPAREN, null)};

    // try {
    switch (mT.type) {
      case RESWRD:
        switch (ResWordAttr.values()[(int) mT.attribute]) {
          case NOT:
            term();
            simpleExpressionTail();
            return;
        }
        break;
      case OPENPAREN:
        term();
        simpleExpressionTail();
        return;
      case ADDOP:
        switch (AddopAttr.values()[(int) mT.attribute]) {
          case PLUS:
            sign();
            term();
            simpleExpressionTail();
            return;
          case MINUS:
            sign();
            term();
            simpleExpressionTail();
            return;
        }
        break;
      case ID:
        term();
        simpleExpressionTail();
        return;
      case NUM:
        term();
        simpleExpressionTail();
        return;
    }

    Token[] toks =
        {pair(Type.RESWRD, ResWordAttr.NOT), pair(Type.OPENPAREN, null),
            pair(Type.ADDOP, AddopAttr.PLUS), pair(Type.ADDOP, AddopAttr.MINUS),
            pair(Type.ID, null), pair(Type.NUM, null)};
    wanted(toks);
    sync();

    /*
     * Unreachable } catch (ParErr e) { sync(); }
     */
  }

  void simpleExpressionTail() {
    mSet =
        new Token[] {pair(Type.RELOP, null), pair(Type.SEMICOLON, null),
            pair(Type.RESWRD, ResWordAttr.END), pair(Type.RESWRD, ResWordAttr.ELSE),
            pair(Type.RESWRD, ResWordAttr.THEN), pair(Type.CLOSEBRACKET, null),
            pair(Type.COMMA, null), pair(Type.CLOSEPAREN, null)};

    try {
      switch (mT.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) mT.attribute]) {
            case END:
              return;
            case THEN:
              return;
            case ELSE:
              return;
            case DO:
              return;
          }
          break;
        case CLOSEPAREN:
          return;
        case SEMICOLON:
          return;
        case COMMA:
          return;
        case CLOSEBRACKET:
          return;
        case RELOP:
          return;
        case ADDOP:
          match(Type.ADDOP, null);
          term();
          simpleExpressionTail();
          return;
      }

      Token[] toks =
          {pair(Type.RESWRD, ResWordAttr.END), pair(Type.RESWRD, ResWordAttr.THEN),
              pair(Type.RESWRD, ResWordAttr.ELSE), pair(Type.RESWRD, ResWordAttr.DO),
              pair(Type.CLOSEPAREN, null), pair(Type.SEMICOLON, null), pair(Type.COMMA, null),
              pair(Type.CLOSEBRACKET, null), pair(Type.RELOP, null), pair(Type.ADDOP, null)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void term() {
    mSet =
        new Token[] {pair(Type.ADDOP, null), pair(Type.RELOP, null), pair(Type.SEMICOLON, null),
            pair(Type.RESWRD, ResWordAttr.END), pair(Type.RESWRD, ResWordAttr.ELSE),
            pair(Type.RESWRD, ResWordAttr.THEN), pair(Type.CLOSEBRACKET, null),
            pair(Type.COMMA, null), pair(Type.CLOSEPAREN, null)};

    // try {
    switch (mT.type) {
      case RESWRD:
        switch (ResWordAttr.values()[(int) mT.attribute]) {
          case NOT:
            factor();
            termTail();
            return;
        }
        break;
      case OPENPAREN:
        factor();
        termTail();
        return;
      case ID:
        factor();
        termTail();
        return;
      case NUM:
        factor();
        termTail();
        return;
    }

    Token[] toks =
        {pair(Type.RESWRD, ResWordAttr.NOT), pair(Type.OPENPAREN, null), pair(Type.ID, null),
            pair(Type.NUM, null)};
    wanted(toks);
    sync();

    /*
     * Unreachable } catch (ParErr e) { sync(); }
     */
  }

  void termTail() {
    mSet =
        new Token[] {pair(Type.ADDOP, null), pair(Type.RELOP, null), pair(Type.SEMICOLON, null),
            pair(Type.RESWRD, ResWordAttr.END), pair(Type.RESWRD, ResWordAttr.ELSE),
            pair(Type.RESWRD, ResWordAttr.THEN), pair(Type.CLOSEBRACKET, null),
            pair(Type.COMMA, null), pair(Type.CLOSEPAREN, null)};

    try {
      switch (mT.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) mT.attribute]) {
            case END:
              return;
            case THEN:
              return;
            case ELSE:
              return;
            case DO:
              return;
          }
          break;
        case CLOSEPAREN:
          return;
        case SEMICOLON:
          return;
        case COMMA:
          return;
        case CLOSEBRACKET:
          return;
        case RELOP:
          return;
        case ADDOP:
          return;
        case MULOP:
          match(Type.MULOP, null);
          factor();
          termTail();
          return;
      }

      Token[] toks =
          {pair(Type.RESWRD, ResWordAttr.END), pair(Type.RESWRD, ResWordAttr.THEN),
              pair(Type.RESWRD, ResWordAttr.ELSE), pair(Type.RESWRD, ResWordAttr.DO),
              pair(Type.CLOSEPAREN, null), pair(Type.SEMICOLON, null), pair(Type.COMMA, null),
              pair(Type.CLOSEBRACKET, null), pair(Type.RELOP, null), pair(Type.ADDOP, null),
              pair(Type.MULOP, null)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void factor() {
    mSet =
        new Token[] {pair(Type.ADDOP, null), pair(Type.RELOP, null), pair(Type.SEMICOLON, null),
            pair(Type.RESWRD, ResWordAttr.END), pair(Type.RESWRD, ResWordAttr.ELSE),
            pair(Type.RESWRD, ResWordAttr.THEN), pair(Type.CLOSEBRACKET, null),
            pair(Type.COMMA, null), pair(Type.CLOSEPAREN, null), pair(Type.MULOP, null)};

    try {
      switch (mT.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) mT.attribute]) {
            case NOT:
              match(Type.RESWRD, ResWordAttr.NOT);
              factor();
              return;
          }
          break;
        case OPENPAREN:
          match(Type.OPENPAREN, null);
          expression();
          match(Type.CLOSEPAREN, null);
          return;
        case ID:
          match(Type.ID, null);
          factorTail();
          return;
        case NUM:
          match(Type.NUM, null);
          return;
      }

      Token[] toks =
          {pair(Type.RESWRD, ResWordAttr.NOT), pair(Type.OPENPAREN, null), pair(Type.ID, null),
              pair(Type.NUM, null)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void factorTail() {
    mSet =
        new Token[] {pair(Type.ADDOP, null), pair(Type.RELOP, null), pair(Type.SEMICOLON, null),
            pair(Type.RESWRD, ResWordAttr.END), pair(Type.RESWRD, ResWordAttr.ELSE),
            pair(Type.RESWRD, ResWordAttr.THEN), pair(Type.CLOSEBRACKET, null),
            pair(Type.COMMA, null), pair(Type.CLOSEPAREN, null), pair(Type.MULOP, null)};

    try {
      switch (mT.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) mT.attribute]) {
            case END:
              return;
            case THEN:
              return;
            case ELSE:
              return;
            case DO:
              return;
          }
          break;
        case CLOSEPAREN:
          return;
        case SEMICOLON:
          return;
        case COMMA:
          return;
        case CLOSEBRACKET:
          return;
        case RELOP:
          return;
        case ADDOP:
          return;
        case MULOP:
          match(Type.MULOP, null);
          factor();
          termTail();
          return;
        case OPENBRACKET:
          match(Type.OPENBRACKET, null);
          expression();
          match(Type.CLOSEBRACKET, null);
          return;
      }

      Token[] toks =
          {pair(Type.RESWRD, ResWordAttr.END), pair(Type.RESWRD, ResWordAttr.THEN),
              pair(Type.RESWRD, ResWordAttr.ELSE), pair(Type.RESWRD, ResWordAttr.DO),
              pair(Type.CLOSEPAREN, null), pair(Type.SEMICOLON, null), pair(Type.COMMA, null),
              pair(Type.CLOSEBRACKET, null), pair(Type.RELOP, null), pair(Type.ADDOP, null),
              pair(Type.MULOP, null), pair(Type.OPENBRACKET, null)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void sign() {
    mSet =
        new Token[] {pair(Type.ID, null), pair(Type.NUM, null), pair(Type.OPENPAREN, null),
            pair(Type.RESWRD, ResWordAttr.NOT)};

    try {
      switch (mT.type) {
        case ADDOP:
          switch (AddopAttr.values()[(int) mT.attribute]) {
            case PLUS:
              match(Type.ADDOP, AddopAttr.PLUS);
              return;
            case MINUS:
              match(Type.ADDOP, AddopAttr.MINUS);
              return;
          }
          break;
      }

      Token[] toks = {};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }
}
