package kuxhausen;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

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

  private class SyntaxErr extends Exception {
  }

  Token pair(TokType type, Enum attr) {
    return new Token(type, (attr != null) ? attr.ordinal() : -1, null, null);
  }

  public void match(TokType type, Enum attr) throws SyntaxErr {
    Token desired = pair(type, attr);
    if (mT.fullTypeMatch(desired)) {
      consumeToken();
    } else {
      Token[] toks = {pair(type, attr)};
      wanted(toks);
      throw new SyntaxErr();
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

  public List<BlueNode> getPPs(String name) {
    ArrayList<BlueNode> result = new ArrayList<BlueNode>();
    GreenNode parrent = null;
    for (GreenNode g : mScope) {
      if (g.getName().equals(name)) {
        parrent = g;
      }
    }
    if (parrent != null) {
      for (Node n : parrent.getChildren()) {
        if (n instanceof BlueNode) {
          switch (((BlueNode) n).getType()) {
            case PPAINT:
              result.add((BlueNode) n);
              break;
            case PPAREAL:
              result.add((BlueNode) n);
              break;
            case PPINT:
              result.add((BlueNode) n);
              break;
            case PPREAL:
              result.add((BlueNode) n);
              break;
          }
        }
      }
    }
    return result;
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

    } catch (SyntaxErr e) {
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

    } catch (SyntaxErr e) {
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

    } catch (SyntaxErr e) {
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

    } catch (SyntaxErr e) {
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

    } catch (SyntaxErr e) {
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

    } catch (SyntaxErr e) {
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

    } catch (SyntaxErr e) {
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

    } catch (SyntaxErr e) {
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

    } catch (SyntaxErr e) {
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

    } catch (SyntaxErr e) {
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

    } catch (SyntaxErr e) {
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

    } catch (SyntaxErr e) {
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

    } catch (SyntaxErr e) {
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

    } catch (SyntaxErr e) {
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

    } catch (SyntaxErr e) {
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

    } catch (SyntaxErr e) {
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

    } catch (SyntaxErr e) {
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

    } catch (SyntaxErr e) {
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

    } catch (SyntaxErr e) {
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

    } catch (SyntaxErr e) {
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

    } catch (SyntaxErr e) {
      sync();
    }
  }

  PasType variable() {
    mSet = new Token[] {pair(TokType.ASSIGNOP, null)};

    try {
      switch (mT.type) {
        case ID:
          match(TokType.ID, null);
          Token id = mConsumed;
          PasType idT = checkBlue(id.lexeme);
          PasType varT = variableTail();

          if (idT == PasType.ERR || varT == PasType.ERR)
            return PasType.ERR;
          else if (varT == PasType.INT) {
            switch (idT) {
              case AINT:
                return PasType.INT;
              case PPAINT:
                return PasType.INT;
              case AREAL:
                return PasType.REAL;
              case PPAREAL:
                return PasType.REAL;
              default:
                return reportErrStar("Array type expected, " + idT.toString() + " recieved");
            }
          } else if (varT == PasType.NULL) {
            switch (idT) {
              case INT:
                return PasType.INT;
              case PPINT:
                return PasType.INT;
              case REAL:
                return PasType.REAL;
              case PPREAL:
                return PasType.REAL;
              default:
                return reportErrStar("Numeric type expected, " + idT.toString() + " recieved");
            }
          } else {
            return reportErrStar("Invalid array index type, " + idT.toString() + " recieved");
          }
      }

      Token[] toks = {pair(TokType.ID, null)};
      wanted(toks);
      sync();

    } catch (SyntaxErr e) {
      sync();
    }
    return PasType.ERR;
  }

  PasType variableTail() {
    mSet = new Token[] {pair(TokType.ASSIGNOP, null)};

    try {
      switch (mT.type) {
        case OPENBRACKET:
          match(TokType.OPENBRACKET, null);
          PasType exp = expression();
          match(TokType.CLOSEBRACKET, null);
          return exp;
        case ASSIGNOP:
          return PasType.NULL;
      }

      Token[] toks = {pair(TokType.OPENBRACKET, null), pair(TokType.ASSIGNOP, null)};
      wanted(toks);
      sync();

    } catch (SyntaxErr e) {
      sync();
    }

    return PasType.ERR;
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
              Token id = mConsumed;
              checkGreen(id.lexeme);
              int numDesired = getPPs(id.lexeme).size();
              int numSeen = procedureStatementTail(new PPPair(id.lexeme, 0));
              if (numDesired != numSeen)
                reportErrStar("procedure " + id.lexeme + " called with " + numSeen
                    + " params, yet expected " + numDesired);
              return;
          }
          break;
      }

      Token[] toks = {pair(TokType.RESWRD, ResWordAttr.CALL)};
      wanted(toks);
      sync();

    } catch (SyntaxErr e) {
      sync();
    }
  }

  int procedureStatementTail(PPPair i) {
    mSet =
        new Token[] {pair(TokType.SEMICOLON, null), pair(TokType.RESWRD, ResWordAttr.END),
            pair(TokType.RESWRD, ResWordAttr.ELSE)};

    try {
      switch (mT.type) {
        case RESWRD:
          switch (ResWordAttr.values()[(int) mT.attribute]) {
            case END:
              return 0;
            case ELSE:
              return 0;
          }
          break;
        case OPENPAREN:
          match(TokType.OPENPAREN, null);
          int listNum = expressionList(i);
          match(TokType.CLOSEPAREN, null);
          return listNum;
        case SEMICOLON:
          return 0;
      }

      Token[] toks =
          {pair(TokType.RESWRD, ResWordAttr.END), pair(TokType.RESWRD, ResWordAttr.ELSE),
              pair(TokType.OPENPAREN, null), pair(TokType.SEMICOLON, null)};
      wanted(toks);
      sync();

    } catch (SyntaxErr e) {
      sync();
    }

    return 0;
  }

  int expressionList(PPPair i) {
    mSet = new Token[] {pair(TokType.CLOSEPAREN, null)};

    // try {
    switch (mT.type) {
      case RESWRD:
        switch (ResWordAttr.values()[(int) mT.attribute]) {
          case NOT:
            PasType exp1 = expression();
            if (getPPs(i.procName).size() >= i.paramNum)
              reportErrStar("Unexpected procedure param of type" + exp1.toString());
            else if (getPPs(i.procName).get(i.paramNum).getType() != exp1)
              reportErrStar("Incorrect procedure param type: got " + exp1 + ", expected "
                  + getPPs(i.procName).get(i.paramNum).getType());
            return expressionListTail(new PPPair(i.procName, i.paramNum + 1));
        }
        break;
      case OPENPAREN:
        PasType exp2 = expression();
        if (getPPs(i.procName).size() >= i.paramNum)
          reportErrStar("Unexpected procedure param of type" + exp2.toString());
        else if (getPPs(i.procName).get(i.paramNum).getType() != exp2)
          reportErrStar("Incorrect procedure param type: got " + exp2 + ", expected "
              + getPPs(i.procName).get(i.paramNum).getType());
        return expressionListTail(new PPPair(i.procName, i.paramNum + 1));
      case ADDOP:
        switch (AddopAttr.values()[(int) mT.attribute]) {
          case PLUS:
            PasType exp3 = expression();
            if (getPPs(i.procName).size() >= i.paramNum)
              reportErrStar("Unexpected procedure param of type" + exp3.toString());
            else if (getPPs(i.procName).get(i.paramNum).getType() != exp3)
              reportErrStar("Incorrect procedure param type: got " + exp3 + ", expected "
                  + getPPs(i.procName).get(i.paramNum).getType());
            return expressionListTail(new PPPair(i.procName, i.paramNum + 1));
          case MINUS:
            PasType exp4 = expression();
            if (getPPs(i.procName).size() >= i.paramNum)
              reportErrStar("Unexpected procedure param of type" + exp4.toString());
            else if (getPPs(i.procName).get(i.paramNum).getType() != exp4)
              reportErrStar("Incorrect procedure param type: got " + exp4 + ", expected "
                  + getPPs(i.procName).get(i.paramNum).getType());
            return expressionListTail(new PPPair(i.procName, i.paramNum + 1));
        }
        break;
      case ID:
        PasType exp5 = expression();
        if (getPPs(i.procName).size() >= i.paramNum)
          reportErrStar("Unexpected procedure param of type" + exp5.toString());
        else if (getPPs(i.procName).get(i.paramNum).getType() != exp5)
          reportErrStar("Incorrect procedure param type: got " + exp5 + ", expected "
              + getPPs(i.procName).get(i.paramNum).getType());
        return expressionListTail(new PPPair(i.procName, i.paramNum + 1));
      case NUM:
        PasType exp6 = expression();
        if (getPPs(i.procName).size() >= i.paramNum)
          reportErrStar("Unexpected procedure param of type" + exp6.toString());
        else if (getPPs(i.procName).get(i.paramNum).getType() != exp6)
          reportErrStar("Incorrect procedure param type: got " + exp6 + ", expected "
              + getPPs(i.procName).get(i.paramNum).getType());
        return expressionListTail(new PPPair(i.procName, i.paramNum + 1));
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

    return 0;
  }

  int expressionListTail(PPPair i) {
    mSet = new Token[] {pair(TokType.CLOSEPAREN, null)};

    try {
      switch (mT.type) {
        case CLOSEPAREN:
          return i.paramNum;
        case COMMA:
          match(TokType.COMMA, null);
          PasType exp = expression();
          if (getPPs(i.procName).size() >= i.paramNum)
            reportErrStar("Unexpected procedure param of type" + exp.toString());
          else if (getPPs(i.procName).get(i.paramNum).getType() != exp)
            reportErrStar("Incorrect procedure param type: got " + exp + ", expected "
                + getPPs(i.procName).get(i.paramNum).getType());
          return expressionListTail(new PPPair(i.procName, i.paramNum + 1));
      }

      Token[] toks = {pair(TokType.CLOSEPAREN, null), pair(TokType.COMMA, null)};
      wanted(toks);
      sync();

    } catch (SyntaxErr e) {
      sync();
    }

    return 0;
  }

  PasType expression() {
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
            PasType se1 = simpleExpression();
            return expressionTail(se1);
        }
        break;
      case OPENPAREN:
        PasType se2 = simpleExpression();
        return expressionTail(se2);
      case ADDOP:
        switch (AddopAttr.values()[(int) mT.attribute]) {
          case PLUS:
            PasType se3 = simpleExpression();
            return expressionTail(se3);
          case MINUS:
            PasType se4 = simpleExpression();
            return expressionTail(se4);
        }
        break;
      case ID:
        PasType se5 = simpleExpression();
        return expressionTail(se5);
      case NUM:
        PasType se6 = simpleExpression();
        return expressionTail(se6);
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

    return PasType.ERR;
  }

  PasType expressionTail(PasType i) {
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
              return i;
            case THEN:
              return i;
            case ELSE:
              return i;
            case DO:
              return i;
          }
          break;
        case CLOSEPAREN:
          return i;
        case SEMICOLON:
          return i;
        case COMMA:
          return i;
        case CLOSEBRACKET:
          return i;
        case RELOP:
          match(TokType.RELOP, null);
          Token relop = mConsumed;
          PasType se = simpleExpression();



          if (i == PasType.ERR || se == PasType.ERR)
            return PasType.ERR;
          else if (i == PasType.BOOL && relop.getRelop() == RelopAttr.EQ && se == PasType.BOOL)
            return PasType.BOOL;
          else if (i == PasType.BOOL && relop.getRelop() == RelopAttr.NEQ && se == PasType.BOOL)
            return PasType.BOOL;
          else if (i == PasType.INT && se == PasType.INT)
            return PasType.INT;
          else if (i == PasType.REAL && se == PasType.REAL)
            return PasType.REAL;
          else
            return reportErrStar("type error" + i.toString() + " " + relop.getAttribute() + " "
                + se.toString() + " cannot be used together");
      }

      Token[] toks =
          {pair(TokType.RESWRD, ResWordAttr.END), pair(TokType.RESWRD, ResWordAttr.THEN),
              pair(TokType.RESWRD, ResWordAttr.ELSE), pair(TokType.RESWRD, ResWordAttr.DO),
              pair(TokType.CLOSEPAREN, null), pair(TokType.SEMICOLON, null),
              pair(TokType.COMMA, null), pair(TokType.CLOSEBRACKET, null),
              pair(TokType.RELOP, null)};
      wanted(toks);
      sync();

    } catch (SyntaxErr e) {
      sync();
    }

    return PasType.ERR;
  }

  PasType simpleExpression() {
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
            PasType term = term();
            return simpleExpressionTail(term);
        }
        break;
      case OPENPAREN:
        PasType term1 = term();
        return simpleExpressionTail(term1);
      case ADDOP:
        switch (AddopAttr.values()[(int) mT.attribute]) {
          case PLUS:
            sign();
            PasType term2 = term();
            PasType set1 = simpleExpressionTail(term2);
            if (set1 == PasType.BOOL)
              return reportErrStar("Expected num after +,- but recieved BOOL");
            else
              return set1;
          case MINUS:
            sign();
            PasType term3 = term();
            PasType set2 = simpleExpressionTail(term3);
            if (set2 == PasType.BOOL)
              return reportErrStar("Expected num after +,- but recieved BOOL");
            else
              return set2;
        }
        break;
      case ID:
        PasType term4 = term();
        return simpleExpressionTail(term4);
      case NUM:
        PasType term5 = term();
        return simpleExpressionTail(term5);
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

    return PasType.ERR;
  }

  PasType simpleExpressionTail(PasType i) {
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
              return i;
            case THEN:
              return i;
            case ELSE:
              return i;
            case DO:
              return i;
          }
          break;
        case CLOSEPAREN:
          return i;
        case SEMICOLON:
          return i;
        case COMMA:
          return i;
        case CLOSEBRACKET:
          return i;
        case RELOP:
          return i;
        case ADDOP:
          match(TokType.ADDOP, null);
          Token addop = mConsumed;
          PasType term = term();
          PasType set1i;
          if (i == PasType.ERR || term == PasType.ERR)
            set1i = PasType.ERR;
          else if (i == PasType.BOOL && addop.getAddop() == AddopAttr.OR && term == PasType.BOOL)
            set1i = PasType.BOOL;
          else if (i == PasType.INT && addop.getAddop() != AddopAttr.OR && term == PasType.INT)
            set1i = PasType.INT;
          else if (i == PasType.REAL && addop.getAddop() != AddopAttr.OR && term == PasType.REAL)
            set1i = PasType.REAL;
          else
            set1i =
                reportErrStar("type error" + i.toString() + " " + addop.getAttribute() + " "
                    + term.toString() + " cannot be used together");

          return simpleExpressionTail(set1i);
      }

      Token[] toks =
          {pair(TokType.RESWRD, ResWordAttr.END), pair(TokType.RESWRD, ResWordAttr.THEN),
              pair(TokType.RESWRD, ResWordAttr.ELSE), pair(TokType.RESWRD, ResWordAttr.DO),
              pair(TokType.CLOSEPAREN, null), pair(TokType.SEMICOLON, null),
              pair(TokType.COMMA, null), pair(TokType.CLOSEBRACKET, null),
              pair(TokType.RELOP, null), pair(TokType.ADDOP, null)};
      wanted(toks);
      sync();

    } catch (SyntaxErr e) {
      sync();
    }

    return PasType.ERR;
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
        PasType fact1 = factor();
        return termTail(fact1);
      case ID:
        PasType fact2 = factor();
        return termTail(fact2);
      case NUM:
        PasType fact3 = factor();
        return termTail(fact3);
    }

    Token[] toks =
        {pair(TokType.RESWRD, ResWordAttr.NOT), pair(TokType.OPENPAREN, null),
            pair(TokType.ID, null), pair(TokType.NUM, null)};
    wanted(toks);
    sync();

    /*
     * Unreachable } catch (ParErr e) { sync(); }
     */

    return PasType.ERR;
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
              return i;
            case THEN:
              return i;
            case ELSE:
              return i;
            case DO:
              return i;
          }
          break;
        case CLOSEPAREN:
          return i;
        case SEMICOLON:
          return i;
        case COMMA:
          return i;
        case CLOSEBRACKET:
          return i;
        case RELOP:
          return i;
        case ADDOP:
          return i;
        case MULOP:
          match(TokType.MULOP, null);
          Token mulop = mConsumed;
          PasType fact = factor();
          PasType term1i;
          if (i == PasType.ERR || fact == PasType.ERR)
            term1i = PasType.ERR;
          else if (i == PasType.BOOL && mulop.getMulop() == MulopAttr.AND && fact == PasType.BOOL)
            term1i = PasType.BOOL;
          else if (i == PasType.INT && mulop.getMulop() != MulopAttr.AND && fact == PasType.INT)
            term1i = PasType.INT;
          else if (i == PasType.REAL && mulop.getMulop() != MulopAttr.AND && fact == PasType.REAL)
            term1i = PasType.REAL;
          else
            term1i =
                reportErrStar("type error" + i.toString() + " " + mulop.getAttribute() + " "
                    + fact.toString() + " cannot be used together");

          return termTail(term1i);
      }

      Token[] toks =
          {pair(TokType.RESWRD, ResWordAttr.END), pair(TokType.RESWRD, ResWordAttr.THEN),
              pair(TokType.RESWRD, ResWordAttr.ELSE), pair(TokType.RESWRD, ResWordAttr.DO),
              pair(TokType.CLOSEPAREN, null), pair(TokType.SEMICOLON, null),
              pair(TokType.COMMA, null), pair(TokType.CLOSEBRACKET, null),
              pair(TokType.RELOP, null), pair(TokType.ADDOP, null), pair(TokType.MULOP, null)};
      wanted(toks);
      sync();

    } catch (SyntaxErr e) {
      sync();
    }

    return PasType.ERR;
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
              switch (fOne) {
                case BOOL:
                  return PasType.BOOL;
                case ERR:
                  return PasType.ERR;
                default:
                  return reportErrStar("Attempted to use nonBoolean type as Boolean");
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

    } catch (SyntaxErr e) {
      sync();
    }

    return PasType.ERR;
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

    } catch (SyntaxErr e) {
      sync();
    }

    return PasType.ERR;
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

    } catch (SyntaxErr e) {
      sync();
    }
  }
}
