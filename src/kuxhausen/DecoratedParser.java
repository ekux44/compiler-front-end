package kuxhausen;

import java.util.ArrayDeque;
import java.util.ArrayList;

import kuxhausen.Token.*;

/**
 * @author Eric Kuxhausen
 */
public class DecoratedParser {

  private Lexar mL;

  /**
   * current Token
   */
  private Token mT;
  private Token mConsumed;
  private SourcePointer mLine;

  /**
   * sync set for the current nonTerminal
   */
  private Token[] mSet;

  private ArrayList<Token> mTokens = new ArrayList<Token>();

  private ArrayDeque<GreenNode> mScope = new ArrayDeque<GreenNode>();
  {
    GreenNode invisibleRoot = new GreenNode();
    invisibleRoot.setName("");
    mScope.add(invisibleRoot);
  }

  DecoratedParser(Lexar lex) {
    mL = lex;
    consumeToken();
    program();
  }

  private void consumeToken() {
    mConsumed = mT;
    Token next = mL.getNextToken();

    if (next == null) {
      next = new Token(TokType.$, null, null, mLine);
    }
    mT = next;
    mTokens.add(next);
    mLine = next.position;
  }

  public ArrayList<Token> getTokenList() {
    return mTokens;
  }

  private class ParErr extends Exception {
  }

  Token pair(TokType type, Enum attr) {
    return new Token(type, (attr != null) ? attr.ordinal() : -1, null, null);
  }

  public void match(TokType type, Enum attr) throws ParErr {
    Token desired = pair(type, attr);
    if (mT.fullTypeMatch(desired)) {
      consumeToken();
    } else {
      Token[] toks = {pair(type, attr)};
      wanted(toks);
      throw new ParErr();
    }
  }

  private void wanted(Token[] wanted) {
    String message = generateErrorMessage(wanted);
    mTokens.add(new Token(TokType.SYNTAXERR, message, mT.lexeme, mT.position));
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
    while (mT.type != TokType.$ && !inSet(mSet)) {
      consumeToken();
    }
  }

  private boolean inSet(Token[] syncSet) {
    for (Token s : syncSet) {
      if (mT.fullTypeMatch(s))
        return true;
    }
    return false;
  }

  public void checkAddGreen(String name) {
    GreenNode green = new GreenNode();
    green.setName(name);

    boolean hasConflict = false;
    for (GreenNode g : mScope) {
      for (Node n : g.getChildren()) {
        if (n instanceof GreenNode && n.getName().equals(name)) {
          hasConflict = true;
        }
      }
    }
    if (hasConflict) {
      mTokens.add(new Token(TokType.SEMANTICERR, "A program or procedure named " + green.getName()
          + " already defined in this scope", name, mLine));
      // go ahead and add node anyway with modified name so that subtree can be typechecked
      green.setName(green.getName() + "#");
    }
    mScope.addFirst(green);
  }

  public void checkAddBlue(String name, PasType type) {
    boolean hasConflict = false;
    for (GreenNode g : mScope) {
      for (Node n : g.getChildren()) {
        if (n instanceof BlueNode && n.getName().equals(name)) {
          hasConflict = true;
        }
      }
    }
    if (hasConflict) {
      mTokens.add(new Token(TokType.SEMANTICERR, "A var or proc_param named " + name
          + " already defined in this scope", name, mLine));
    } else {
      BlueNode b = new BlueNode();
      b.setName(name);
      b.setType(type);
      mScope.getFirst().getChildren().add(b);
    }
  }

  public void checkGreen(String name) {
    for (GreenNode g : mScope) {
      for (Node n : g.getChildren()) {
        if (n instanceof GreenNode && n.getName().equals(name)) {
          return;
        }
      }
    }
    mTokens.add(new Token(TokType.SEMANTICERR, "No program or procedured named " + name
        + " defined yet in this scope", name, mLine));
  }

  public PasType checkBlue(String name) {
    for (GreenNode g : mScope) {
      for (Node n : g.getChildren()) {
        if (n instanceof BlueNode && n.getName().equals(name)) {
          return ((BlueNode) n).getType();
        }
      }
    }
    mTokens.add(new Token(TokType.SEMANTICERR, "No var or proc_param named " + name
        + " defined yet in this scope", name, mLine));
    return PasType.ERR;
  }

  public void exitScope() {
    mScope.removeFirst();
  }

  public PasType reportErrStar(String msg) {
    Token t = new Token(TokType.SEMANTICERR, msg, "", mLine);
    mTokens.add(t);
    return PasType.ERR;
  }

  void program() {
    mSet = new Token[] {};

    try {
      switch (mT.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) mT.attribute]) {
            case PROGRAM:
              match(TokType.RESWRD, ResWordAttr.PROGRAM);
              match(TokType.ID, null);
              match(TokType.OPENPAREN, null);
              identifierList();
              match(TokType.CLOSEPAREN, null);
              match(TokType.SEMICOLON, null);
              programTail();
              return;
          }
          break;
      }

      Token[] toks = {pair(TokType.RESWRD, ResWordAttr.PROGRAM)};
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
              match(TokType.EOF, null);
              return;
            case BEGIN:
              compoundStatement();
              match(TokType.EOF, null);
              return;
          }
          break;
      }

      Token[] toks =
          {pair(TokType.RESWRD, ResWordAttr.VAR), pair(TokType.RESWRD, ResWordAttr.PROC),
              pair(TokType.RESWRD, ResWordAttr.BEGIN)};
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
              match(TokType.EOF, null);
              return;
            case BEGIN:
              compoundStatement();
              match(TokType.EOF, null);
              return;
          }
          break;
      }

      Token[] toks =
          {pair(TokType.RESWRD, ResWordAttr.PROC), pair(TokType.RESWRD, ResWordAttr.BEGIN)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void identifierList() {
    mSet = new Token[] {pair(TokType.CLOSEPAREN, null)};

    try {
      switch (mT.type) {
        case ID:
          match(TokType.ID, null);
          identifierListTail();
          return;
      }

      Token[] toks = {pair(TokType.ID, null)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void identifierListTail() {
    mSet = new Token[] {pair(TokType.CLOSEPAREN, null)};

    try {
      switch (mT.type) {
        case CLOSEPAREN:
          return;
        case COMMA:
          match(TokType.COMMA, null);
          match(TokType.ID, null);
          identifierListTail();
          return;
      }

      Token[] toks = {pair(TokType.CLOSEPAREN, null), pair(TokType.COMMA, null)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void declarations() {
    mSet =
        new Token[] {pair(TokType.RESWRD, ResWordAttr.PROC),
            pair(TokType.RESWRD, ResWordAttr.BEGIN)};

    try {
      switch (mT.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) mT.attribute]) {
            case VAR:
              match(TokType.RESWRD, ResWordAttr.VAR);
              match(TokType.ID, null);
              match(TokType.COLON, null);
              type();
              match(TokType.SEMICOLON, null);
              declarationsTail();
              return;
          }
          break;

      }

      Token[] toks = {pair(TokType.RESWRD, ResWordAttr.VAR)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void declarationsTail() {
    mSet =
        new Token[] {pair(TokType.RESWRD, ResWordAttr.PROC),
            pair(TokType.RESWRD, ResWordAttr.BEGIN)};

    try {
      switch (mT.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) mT.attribute]) {
            case VAR:
              match(TokType.RESWRD, ResWordAttr.VAR);
              match(TokType.ID, null);
              match(TokType.COLON, null);
              type();
              match(TokType.SEMICOLON, null);
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
          {pair(TokType.RESWRD, ResWordAttr.VAR), pair(TokType.RESWRD, ResWordAttr.PROC),
              pair(TokType.RESWRD, ResWordAttr.BEGIN)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void type() {
    mSet = new Token[] {pair(TokType.SEMICOLON, null), pair(TokType.CLOSEPAREN, null)};

    try {
      switch (mT.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) mT.attribute]) {
            case ARRAY:
              match(TokType.RESWRD, ResWordAttr.ARRAY);
              match(TokType.OPENBRACKET, null);
              match(TokType.NUM, null);
              match(TokType.DOTDOT, null);
              match(TokType.NUM, null);
              match(TokType.CLOSEBRACKET, null);
              match(TokType.RESWRD, ResWordAttr.OF);
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
          {pair(TokType.RESWRD, ResWordAttr.ARRAY), pair(TokType.RESWRD, ResWordAttr.INT_NAME),
              pair(TokType.RESWRD, ResWordAttr.REAL_NAME)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void standardType() {
    mSet = new Token[] {pair(TokType.SEMICOLON, null), pair(TokType.CLOSEPAREN, null)};

    try {
      switch (mT.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) mT.attribute]) {
            case INT_NAME:
              match(TokType.RESWRD, ResWordAttr.INT_NAME);
              return;
            case REAL_NAME:
              match(TokType.RESWRD, ResWordAttr.REAL_NAME);
              return;
          }
          break;
      }

      Token[] toks =
          {pair(TokType.RESWRD, ResWordAttr.INT_NAME), pair(TokType.RESWRD, ResWordAttr.REAL_NAME)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void subprogramDeclarations() {
    mSet = new Token[] {pair(TokType.RESWRD, ResWordAttr.BEGIN)};

    try {
      switch (mT.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) mT.attribute]) {
            case PROC:
              subprogramDeclaration();
              match(TokType.SEMICOLON, null);
              subprogramDeclarationsTail();
              return;
          }
          break;
      }

      Token[] toks = {pair(TokType.RESWRD, ResWordAttr.PROC)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void subprogramDeclarationsTail() {
    mSet = new Token[] {pair(TokType.RESWRD, ResWordAttr.BEGIN)};

    try {
      switch (mT.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) mT.attribute]) {
            case PROC:
              subprogramDeclaration();
              match(TokType.SEMICOLON, null);
              subprogramDeclarationsTail();
              return;
            case BEGIN:
              return;
          }
          break;
      }

      Token[] toks =
          {pair(TokType.RESWRD, ResWordAttr.PROC), pair(TokType.RESWRD, ResWordAttr.BEGIN)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void subprogramDeclaration() {
    mSet = new Token[] {pair(TokType.SEMICOLON, null)};

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

    Token[] toks = {pair(TokType.RESWRD, ResWordAttr.PROC)};
    wanted(toks);
    sync();

    /*
     * Unreachable } catch (ParErr e) { sync(); }
     */
  }

  void subprogramDeclarationTail() {
    mSet = new Token[] {pair(TokType.SEMICOLON, null)};

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
        {pair(TokType.RESWRD, ResWordAttr.VAR), pair(TokType.RESWRD, ResWordAttr.PROC),
            pair(TokType.RESWRD, ResWordAttr.BEGIN)};
    wanted(toks);
    sync();

    /*
     * Unreachable } catch (ParErr e) { sync(); }
     */
  }

  void subprogramDeclarationTailTail() {
    mSet = new Token[] {pair(TokType.SEMICOLON, null)};

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

    Token[] toks =
        {pair(TokType.RESWRD, ResWordAttr.PROC), pair(TokType.RESWRD, ResWordAttr.BEGIN)};
    wanted(toks);
    sync();

    /*
     * Unreachable } catch (ParErr e) { sync(); }
     */
  }

  void subprogramHead() {
    mSet =
        new Token[] {pair(TokType.RESWRD, ResWordAttr.VAR), pair(TokType.RESWRD, ResWordAttr.PROC),
            pair(TokType.RESWRD, ResWordAttr.BEGIN)};

    try {
      switch (mT.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) mT.attribute]) {
            case PROC:
              match(TokType.RESWRD, ResWordAttr.PROC);
              match(TokType.ID, null);
              subprogramHeadTail();
              return;
          }
          break;
      }

      Token[] toks = {pair(TokType.RESWRD, ResWordAttr.PROC)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void subprogramHeadTail() {
    mSet =
        new Token[] {pair(TokType.RESWRD, ResWordAttr.VAR), pair(TokType.RESWRD, ResWordAttr.PROC),
            pair(TokType.RESWRD, ResWordAttr.BEGIN)};

    try {
      switch (mT.type) {
        case OPENPAREN:
          arguments();
          match(TokType.SEMICOLON, null);
          return;
        case SEMICOLON:
          match(TokType.SEMICOLON, null);
          return;
      }

      Token[] toks = {pair(TokType.OPENPAREN, null), pair(TokType.SEMICOLON, null)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void arguments() {
    mSet = new Token[] {pair(TokType.SEMICOLON, null)};

    try {
      switch (mT.type) {
        case OPENPAREN:
          match(TokType.OPENPAREN, null);
          parameterList();
          match(TokType.CLOSEPAREN, null);
          return;
      }

      Token[] toks = {pair(TokType.OPENPAREN, null)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void parameterList() {
    mSet = new Token[] {pair(TokType.CLOSEPAREN, null)};

    try {
      switch (mT.type) {
        case ID:
          match(TokType.ID, null);
          match(TokType.COLON, null);
          type();
          parameterListTail();
          return;
      }

      Token[] toks = {pair(TokType.ID, null)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void parameterListTail() {
    mSet = new Token[] {pair(TokType.CLOSEPAREN, null)};

    try {
      switch (mT.type) {
        case CLOSEPAREN:
          return;
        case SEMICOLON:
          match(TokType.SEMICOLON, null);
          match(TokType.ID, null);
          match(TokType.COLON, null);
          type();
          parameterListTail();
          return;
      }

      Token[] toks = {pair(TokType.CLOSEPAREN, null), pair(TokType.SEMICOLON, null)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void compoundStatement() {
    mSet = new Token[] {pair(TokType.EOF, null), pair(TokType.SEMICOLON, null)};

    try {
      switch (mT.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) mT.attribute]) {
            case BEGIN:
              match(TokType.RESWRD, ResWordAttr.BEGIN);
              compoundStatementTail();
              return;
          }
          break;
      }

      Token[] toks = {pair(TokType.RESWRD, ResWordAttr.BEGIN)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void compoundStatementTail() {
    mSet = new Token[] {pair(TokType.EOF, null), pair(TokType.SEMICOLON, null)};

    try {
      switch (mT.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) mT.attribute]) {
            case BEGIN:
              optionalStatements();
              match(TokType.RESWRD, ResWordAttr.END);
              return;
            case END:
              match(TokType.RESWRD, ResWordAttr.END);
              return;
            case IF:
              optionalStatements();
              match(TokType.RESWRD, ResWordAttr.END);
              return;
            case WHILE:
              optionalStatements();
              match(TokType.RESWRD, ResWordAttr.END);
              return;
            case CALL:
              optionalStatements();
              match(TokType.RESWRD, ResWordAttr.END);
              return;
          }
          break;
        case ID:
          optionalStatements();
          match(TokType.RESWRD, ResWordAttr.END);
          return;
      }

      Token[] toks =
          {pair(TokType.RESWRD, ResWordAttr.BEGIN), pair(TokType.RESWRD, ResWordAttr.END),
              pair(TokType.RESWRD, ResWordAttr.IF), pair(TokType.RESWRD, ResWordAttr.WHILE),
              pair(TokType.RESWRD, ResWordAttr.CALL), pair(TokType.ID, null)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void optionalStatements() {
    mSet = new Token[] {pair(TokType.RESWRD, ResWordAttr.END)};

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
        {pair(TokType.RESWRD, ResWordAttr.BEGIN), pair(TokType.RESWRD, ResWordAttr.IF),
            pair(TokType.RESWRD, ResWordAttr.WHILE), pair(TokType.RESWRD, ResWordAttr.CALL),
            pair(TokType.ID, null)};
    wanted(toks);
    sync();

    /*
     * Unreachable } catch (ParErr e) { sync(); }
     */
  }

  void statementList() {
    mSet = new Token[] {pair(TokType.RESWRD, ResWordAttr.END)};

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
        {pair(TokType.RESWRD, ResWordAttr.BEGIN), pair(TokType.RESWRD, ResWordAttr.IF),
            pair(TokType.RESWRD, ResWordAttr.WHILE), pair(TokType.RESWRD, ResWordAttr.CALL),
            pair(TokType.ID, null)};
    wanted(toks);
    sync();

    /*
     * Unreachable } catch (ParErr e) { sync(); }
     */
  }

  void statementListTail() {
    mSet = new Token[] {pair(TokType.RESWRD, ResWordAttr.END)};

    try {
      switch (mT.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) mT.attribute]) {
            case END:
              return;
          }
          break;
        case SEMICOLON:
          match(TokType.SEMICOLON, null);
          statement();
          statementListTail();
          return;
      }

      Token[] toks = {pair(TokType.RESWRD, ResWordAttr.END), pair(TokType.SEMICOLON, null)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void statement() {
    mSet =
        new Token[] {pair(TokType.SEMICOLON, null), pair(TokType.RESWRD, ResWordAttr.END),
            pair(TokType.RESWRD, ResWordAttr.ELSE)};

    try {
      switch (mT.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) mT.attribute]) {
            case BEGIN:
              compoundStatement();
              return;
            case IF:
              match(TokType.RESWRD, ResWordAttr.IF);
              expression();
              match(TokType.RESWRD, ResWordAttr.THEN);
              statement();
              statementTail();
              return;
            case WHILE:
              match(TokType.RESWRD, ResWordAttr.WHILE);
              expression();
              match(TokType.RESWRD, ResWordAttr.DO);
              statement();
              return;
            case CALL:
              procedureStatment();
              return;
          }
          break;
        case ID:
          variable();
          match(TokType.ASSIGNOP, null);
          expression();
          return;
      }

      Token[] toks =
          {pair(TokType.RESWRD, ResWordAttr.BEGIN), pair(TokType.RESWRD, ResWordAttr.IF),
              pair(TokType.RESWRD, ResWordAttr.WHILE), pair(TokType.RESWRD, ResWordAttr.CALL),
              pair(TokType.ID, null)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void statementTail() {
    mSet =
        new Token[] {pair(TokType.SEMICOLON, null), pair(TokType.RESWRD, ResWordAttr.END),
            pair(TokType.RESWRD, ResWordAttr.ELSE)};

    try {
      switch (mT.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) mT.attribute]) {
            case END:
              return;
            case ELSE:
              match(TokType.RESWRD, ResWordAttr.ELSE);
              statement();
              return;
          }
          break;
        case SEMICOLON:
          return;
      }

      Token[] toks =
          {pair(TokType.RESWRD, ResWordAttr.END), pair(TokType.RESWRD, ResWordAttr.ELSE),
              pair(TokType.SEMICOLON, null)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void variable() {
    mSet = new Token[] {pair(TokType.ASSIGNOP, null)};

    try {
      switch (mT.type) {
        case ID:
          match(TokType.ID, null);
          variableTail();
          return;
      }

      Token[] toks = {pair(TokType.ID, null)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void variableTail() {
    mSet = new Token[] {pair(TokType.ASSIGNOP, null)};

    try {
      switch (mT.type) {
        case OPENBRACKET:
          match(TokType.OPENBRACKET, null);
          expression();
          match(TokType.CLOSEBRACKET, null);
          return;
        case ASSIGNOP:
          return;
      }

      Token[] toks = {pair(TokType.OPENBRACKET, null), pair(TokType.ASSIGNOP, null)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void procedureStatment() {
    mSet =
        new Token[] {pair(TokType.SEMICOLON, null), pair(TokType.RESWRD, ResWordAttr.END),
            pair(TokType.RESWRD, ResWordAttr.ELSE)};

    try {
      switch (mT.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) mT.attribute]) {
            case CALL:
              match(TokType.RESWRD, ResWordAttr.CALL);
              match(TokType.ID, null);
              procedureStatementTail();
              return;
          }
          break;
      }

      Token[] toks = {pair(TokType.RESWRD, ResWordAttr.CALL)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void procedureStatementTail() {
    mSet =
        new Token[] {pair(TokType.SEMICOLON, null), pair(TokType.RESWRD, ResWordAttr.END),
            pair(TokType.RESWRD, ResWordAttr.ELSE)};

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
          match(TokType.OPENPAREN, null);
          expressionList();
          match(TokType.CLOSEPAREN, null);
          return;
        case SEMICOLON:
          return;
      }

      Token[] toks =
          {pair(TokType.RESWRD, ResWordAttr.END), pair(TokType.RESWRD, ResWordAttr.ELSE),
              pair(TokType.OPENPAREN, null), pair(TokType.SEMICOLON, null)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void expressionList() {
    mSet = new Token[] {pair(TokType.CLOSEPAREN, null)};

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
        {pair(TokType.RESWRD, ResWordAttr.NOT), pair(TokType.OPENPAREN, null),
            pair(TokType.ADDOP, AddopAttr.PLUS), pair(TokType.ADDOP, AddopAttr.MINUS),
            pair(TokType.ID, null), pair(TokType.NUM, null)};
    wanted(toks);
    sync();

    /*
     * Unreachable } catch (ParErr e) { sync(); }
     */
  }

  void expressionListTail() {
    mSet = new Token[] {pair(TokType.CLOSEPAREN, null)};

    try {
      switch (mT.type) {
        case CLOSEPAREN:
          return;
        case COMMA:
          match(TokType.COMMA, null);
          expression();
          expressionListTail();
          return;
      }

      Token[] toks = {pair(TokType.CLOSEPAREN, null), pair(TokType.COMMA, null)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void expression() {
    mSet =
        new Token[] {pair(TokType.SEMICOLON, null), pair(TokType.RESWRD, ResWordAttr.END),
            pair(TokType.RESWRD, ResWordAttr.ELSE), pair(TokType.RESWRD, ResWordAttr.THEN),
            pair(TokType.CLOSEBRACKET, null), pair(TokType.COMMA, null),
            pair(TokType.CLOSEPAREN, null)};

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
        {pair(TokType.RESWRD, ResWordAttr.NOT), pair(TokType.OPENPAREN, null),
            pair(TokType.ADDOP, AddopAttr.PLUS), pair(TokType.ADDOP, AddopAttr.MINUS),
            pair(TokType.ID, null), pair(TokType.NUM, null)};
    wanted(toks);
    sync();

    /*
     * Unreachable } catch (ParErr e) { sync(); }
     */
  }

  void expressionTail() {
    mSet =
        new Token[] {pair(TokType.SEMICOLON, null), pair(TokType.RESWRD, ResWordAttr.END),
            pair(TokType.RESWRD, ResWordAttr.ELSE), pair(TokType.RESWRD, ResWordAttr.THEN),
            pair(TokType.CLOSEBRACKET, null), pair(TokType.COMMA, null),
            pair(TokType.CLOSEPAREN, null)};

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
          match(TokType.RELOP, null);
          simpleExpression();
          return;
      }

      Token[] toks =
          {pair(TokType.RESWRD, ResWordAttr.END), pair(TokType.RESWRD, ResWordAttr.THEN),
              pair(TokType.RESWRD, ResWordAttr.ELSE), pair(TokType.RESWRD, ResWordAttr.DO),
              pair(TokType.CLOSEPAREN, null), pair(TokType.SEMICOLON, null),
              pair(TokType.COMMA, null), pair(TokType.CLOSEBRACKET, null),
              pair(TokType.RELOP, null)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void simpleExpression() {
    mSet =
        new Token[] {pair(TokType.RELOP, null), pair(TokType.SEMICOLON, null),
            pair(TokType.RESWRD, ResWordAttr.END), pair(TokType.RESWRD, ResWordAttr.ELSE),
            pair(TokType.RESWRD, ResWordAttr.THEN), pair(TokType.CLOSEBRACKET, null),
            pair(TokType.COMMA, null), pair(TokType.CLOSEPAREN, null)};

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
        {pair(TokType.RESWRD, ResWordAttr.NOT), pair(TokType.OPENPAREN, null),
            pair(TokType.ADDOP, AddopAttr.PLUS), pair(TokType.ADDOP, AddopAttr.MINUS),
            pair(TokType.ID, null), pair(TokType.NUM, null)};
    wanted(toks);
    sync();

    /*
     * Unreachable } catch (ParErr e) { sync(); }
     */
  }

  void simpleExpressionTail() {
    mSet =
        new Token[] {pair(TokType.RELOP, null), pair(TokType.SEMICOLON, null),
            pair(TokType.RESWRD, ResWordAttr.END), pair(TokType.RESWRD, ResWordAttr.ELSE),
            pair(TokType.RESWRD, ResWordAttr.THEN), pair(TokType.CLOSEBRACKET, null),
            pair(TokType.COMMA, null), pair(TokType.CLOSEPAREN, null)};

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
          match(TokType.ADDOP, null);
          term();
          simpleExpressionTail();
          return;
      }

      Token[] toks =
          {pair(TokType.RESWRD, ResWordAttr.END), pair(TokType.RESWRD, ResWordAttr.THEN),
              pair(TokType.RESWRD, ResWordAttr.ELSE), pair(TokType.RESWRD, ResWordAttr.DO),
              pair(TokType.CLOSEPAREN, null), pair(TokType.SEMICOLON, null),
              pair(TokType.COMMA, null), pair(TokType.CLOSEBRACKET, null),
              pair(TokType.RELOP, null), pair(TokType.ADDOP, null)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  PasType term() {
    mSet =
        new Token[] {pair(TokType.ADDOP, null), pair(TokType.RELOP, null),
            pair(TokType.SEMICOLON, null), pair(TokType.RESWRD, ResWordAttr.END),
            pair(TokType.RESWRD, ResWordAttr.ELSE), pair(TokType.RESWRD, ResWordAttr.THEN),
            pair(TokType.CLOSEBRACKET, null), pair(TokType.COMMA, null),
            pair(TokType.CLOSEPAREN, null)};

    // try {
    switch (mT.type) {
      case RESWRD:
        switch (ResWordAttr.values()[(int) mT.attribute]) {
          case NOT:
            PasType fact = factor();
            return termTail(fact);
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
        {pair(TokType.RESWRD, ResWordAttr.NOT), pair(TokType.OPENPAREN, null),
            pair(TokType.ID, null), pair(TokType.NUM, null)};
    wanted(toks);
    sync();

    /*
     * Unreachable } catch (ParErr e) { sync(); }
     */
  }

  PasType termTail(PasType i) {
    mSet =
        new Token[] {pair(TokType.ADDOP, null), pair(TokType.RELOP, null),
            pair(TokType.SEMICOLON, null), pair(TokType.RESWRD, ResWordAttr.END),
            pair(TokType.RESWRD, ResWordAttr.ELSE), pair(TokType.RESWRD, ResWordAttr.THEN),
            pair(TokType.CLOSEBRACKET, null), pair(TokType.COMMA, null),
            pair(TokType.CLOSEPAREN, null)};

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
          match(TokType.MULOP, null);
          factor();
          termTail();
          return;
      }

      Token[] toks =
          {pair(TokType.RESWRD, ResWordAttr.END), pair(TokType.RESWRD, ResWordAttr.THEN),
              pair(TokType.RESWRD, ResWordAttr.ELSE), pair(TokType.RESWRD, ResWordAttr.DO),
              pair(TokType.CLOSEPAREN, null), pair(TokType.SEMICOLON, null),
              pair(TokType.COMMA, null), pair(TokType.CLOSEBRACKET, null),
              pair(TokType.RELOP, null), pair(TokType.ADDOP, null), pair(TokType.MULOP, null)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  PasType factor() {
    mSet =
        new Token[] {pair(TokType.ADDOP, null), pair(TokType.RELOP, null),
            pair(TokType.SEMICOLON, null), pair(TokType.RESWRD, ResWordAttr.END),
            pair(TokType.RESWRD, ResWordAttr.ELSE), pair(TokType.RESWRD, ResWordAttr.THEN),
            pair(TokType.CLOSEBRACKET, null), pair(TokType.COMMA, null),
            pair(TokType.CLOSEPAREN, null), pair(TokType.MULOP, null)};

    try {
      switch (mT.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) mT.attribute]) {
            case NOT:
              match(TokType.RESWRD, ResWordAttr.NOT);
              PasType fOne = factor();
              switch(fOne){
                case BOOL: return PasType.BOOL;
                case ERR: return PasType.ERR;
                default: return reportErrStar("Attempted to use nonBoolean type as Boolean");
              }
          }
          break;
        case OPENPAREN:
          match(TokType.OPENPAREN, null);
          PasType eType = expression();
          match(TokType.CLOSEPAREN, null);
          return eType;
        case ID:
          match(TokType.ID, null);
          PasType idType = checkBlue(mConsumed.getAttribute());
          PasType fTail = factorTail();
          if (fTail == PasType.ERR || idType == PasType.ERR)
            return PasType.ERR;
          if (fTail == PasType.INT) {
            switch (idType) {
              case AINT:
                return PasType.INT;
              case PPAINT:
                return PasType.INT;
              case AREAL:
                return PasType.REAL;
              case PPAREAL:
                return PasType.REAL;
              default:
                return reportErrStar("Array type expected, " + idType.toString() + " recieved");
            }
          } else if (fTail == PasType.NULL) {
            switch (idType) {
              case INT:
                return PasType.INT;
              case PPINT:
                return PasType.INT;
              case REAL:
                return PasType.REAL;
              case PPREAL:
                return PasType.REAL;
              default:
                return reportErrStar("Numeric type expected, " + idType.toString() + " recieved");
            }
          } else {
            return reportErrStar("Invalid array index type, " + idType.toString() + " recieved");
          }
        case NUM:
          match(TokType.NUM, null);
          return mConsumed.getNumType();
      }

      Token[] toks =
          {pair(TokType.RESWRD, ResWordAttr.NOT), pair(TokType.OPENPAREN, null),
              pair(TokType.ID, null), pair(TokType.NUM, null)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  PasType factorTail() {
    mSet =
        new Token[] {pair(TokType.ADDOP, null), pair(TokType.RELOP, null),
            pair(TokType.SEMICOLON, null), pair(TokType.RESWRD, ResWordAttr.END),
            pair(TokType.RESWRD, ResWordAttr.ELSE), pair(TokType.RESWRD, ResWordAttr.THEN),
            pair(TokType.CLOSEBRACKET, null), pair(TokType.COMMA, null),
            pair(TokType.CLOSEPAREN, null), pair(TokType.MULOP, null)};

    try {
      switch (mT.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) mT.attribute]) {
            case END:
              return PasType.NULL;
            case THEN:
              return PasType.NULL;
            case ELSE:
              return PasType.NULL;
            case DO:
              return PasType.NULL;
          }
          break;
        case CLOSEPAREN:
          return PasType.NULL;
        case SEMICOLON:
          return PasType.NULL;
        case COMMA:
          return PasType.NULL;
        case CLOSEBRACKET:
          return PasType.NULL;
        case RELOP:
          return PasType.NULL;
        case ADDOP:
          return PasType.NULL;
        case MULOP:
          return PasType.NULL;
        case OPENBRACKET:
          match(TokType.OPENBRACKET, null);
          PasType exp = expression();
          match(TokType.CLOSEBRACKET, null);
          return exp;
      }

      Token[] toks =
          {pair(TokType.RESWRD, ResWordAttr.END), pair(TokType.RESWRD, ResWordAttr.THEN),
              pair(TokType.RESWRD, ResWordAttr.ELSE), pair(TokType.RESWRD, ResWordAttr.DO),
              pair(TokType.CLOSEPAREN, null), pair(TokType.SEMICOLON, null),
              pair(TokType.COMMA, null), pair(TokType.CLOSEBRACKET, null),
              pair(TokType.RELOP, null), pair(TokType.ADDOP, null), pair(TokType.MULOP, null),
              pair(TokType.OPENBRACKET, null)};
      wanted(toks);
      sync();

    } catch (ParErr e) {
      sync();
    }
  }

  void sign() {
    mSet =
        new Token[] {pair(TokType.ID, null), pair(TokType.NUM, null),
            pair(TokType.OPENPAREN, null), pair(TokType.RESWRD, ResWordAttr.NOT)};

    try {
      switch (mT.type) {
        case ADDOP:
          switch (AddopAttr.values()[(int) mT.attribute]) {
            case PLUS:
              match(TokType.ADDOP, AddopAttr.PLUS);
              return;
            case MINUS:
              match(TokType.ADDOP, AddopAttr.MINUS);
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
