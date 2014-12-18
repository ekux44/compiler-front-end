package kuxhausen;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.io.*;

import static java.lang.System.out;

/**
 * @author Eric Kuxhausen
 */
public class Lexar {

  private HashMap<String, Token> reservedWordTable = new HashMap<String, Token>();
  private SourceBuffer source = new SourceBuffer();
  private SourcePointer srcPos = new SourcePointer();
  private SymbolTable symbols = new SymbolTable();
  private ArrayList<Token> tokens = new ArrayList<Token>();

  public Lexar(Scanner file) {
    loadReservedWordTable();

    while (file.hasNextLine()) {
      // Read source into buffer
      // Per project spec, only consider upto 71 characters per line including \n
      String line = file.nextLine();
      source.addLine(line.substring(0, Math.min(71, line.length())) + "\n");
    }
    file.close();
  }

  private void loadReservedWordTable() {
    try {
      Scanner wordFile = new Scanner(new BufferedReader(new FileReader("input/reservedwords.txt")));

      while (wordFile.hasNextLine() && wordFile.hasNext()) {
        String lexeme = wordFile.next();
        String resType = wordFile.next();
        int attribute = wordFile.nextInt();

        if (resType.equals(Token.Type.ADDOP.toString())) {
          reservedWordTable.put(lexeme, new Token(Token.Type.ADDOP, attribute, lexeme, srcPos));
        } else if (resType.equals(Token.Type.MULOP.toString())) {
          reservedWordTable.put(lexeme, new Token(Token.Type.MULOP, attribute, lexeme, srcPos));
        } else {
          for (Token.ResWordAttr tt : Token.ResWordAttr.values()) {
            if (resType.equals(tt.toString())) {
              reservedWordTable.put(lexeme, new Token(Token.Type.RESWRD, tt.ordinal(), lexeme,
                  srcPos));
            }
          }
        }
      }

      wordFile.close();
      out.println("successfully loaded " + reservedWordTable.size()
          + " reserved words from reservedwords.txt");
    } catch (FileNotFoundException e) {
      out.println("reservedwords.txt not found");
    }
  }

  public Token getNextToken() {
    Token result = null;

    result = reservedWordsMachine();
    if (result == null) {
      whitespaceMachine();
      if (!source.hasNext(srcPos)) // check there is more after removing whitespace
        return result;
      result = idMachine();
    }
    if (result == null) {
      result = realMachine();
    }
    if (result == null) {
      result = intMachine();
    }
    if (result == null) {
      result = relopMachine();
    }
    if (result == null) {
      result = catchAllMachine();
    }

    if (result != null)
      tokens.add(result);
    return result;
  }

  private boolean isWhiteSpace(char c) {
    if (c == ' ' || c == '\t' || c == '\n')
      return true;
    return false;
  }

  private boolean isLetter(char c) {
    if (c >= 'a' && c <= 'z')
      return true;
    if (c >= 'A' && c <= 'Z')
      return true;
    return false;
  }

  private boolean isDigit(char c) {
    if (c >= '0' && c <= '9')
      return true;
    return false;
  }

  private boolean isEOF(char c) {
    return (c == '.');
  }

  private Token reservedWordsMachine() {
    SourcePointer backup = srcPos.clone();

    // first consume whitespace expected before id / reserved words
    boolean hasConsumedWhitespace = false;
    if (this.srcPos.lineNum == 0 && srcPos.charInLineNum == 0) {
      hasConsumedWhitespace = true; // whitespace not needed before first char in source
    }

    while (source.hasNext(srcPos) && isWhiteSpace(source.peek(srcPos))) {
      source.advanceChar(srcPos);
      hasConsumedWhitespace = true;
    }

    if (hasConsumedWhitespace) {
      String candidate = "";

      // next consume one letter
      if (source.hasNext(srcPos) && isLetter(source.peek(srcPos))) {
        candidate += source.advanceChar(srcPos);

        // next consume any following letters or digits
        while (source.hasNext(srcPos)
            && (isLetter(source.peek(srcPos)) || isDigit(source.peek(srcPos)))) {
          candidate += source.advanceChar(srcPos);
        }

        // if candidate is followed by whitespace or EOF
        if (source.hasNext(srcPos)
            && (isWhiteSpace(source.peek(srcPos)) || isEOF(source.peek(srcPos)))) {

          // check reserved word table
          if (reservedWordTable.containsKey(candidate)) {
            Token result = reservedWordTable.get(candidate).clone();
            result.position = srcPos.clone();
            return result;
          }
        }
      }
    }

    // if no token matched, revert source pointer and return null
    srcPos = backup;
    return null;
  }


  private Token idMachine() {
    SourcePointer backup = srcPos.clone();
    String candidate = "";

    // consume one letter
    if (source.hasNext(srcPos) && isLetter(source.peek(srcPos))) {
      candidate += source.advanceChar(srcPos);

      // next consume any following letters or digits
      while (source.hasNext(srcPos)
          && (isLetter(source.peek(srcPos)) || isDigit(source.peek(srcPos)))) {
        candidate += source.advanceChar(srcPos);
      }

      if (candidate.length() > 10)
        return new Token(Token.Type.LEXERR, "Invalid ID: too long", candidate, srcPos);

      // Check add id to symbol table
      Token t = new Token(Token.Type.ID, candidate, candidate, srcPos);
      if (!symbols.table.containsKey(candidate))
        symbols.table.put(candidate, t);
      return t;
    }

    // if no token matched, revert source pointer and return null
    srcPos = backup;
    return null;
  }

  /**
   * consumes whitespace
   */
  private void whitespaceMachine() {
    while (source.hasNext(srcPos) && isWhiteSpace(source.peek(srcPos))) {
      source.advanceChar(srcPos);
    }
  }

  private Token relopMachine() {
    SourcePointer backup = srcPos.clone();

    if (source.hasNext(srcPos)) {
      String lex = "" + source.advanceChar(srcPos);
      switch (lex) {
        case "=":
          return new Token(Token.Type.RELOP, Token.RelopAttr.EQ.ordinal(), lex, srcPos);
        case "<":
          if (source.hasNext(srcPos)) {
            if (source.hasNext(srcPos) && source.peek(srcPos) == '>') {
              lex += source.advanceChar(srcPos);
              return new Token(Token.Type.RELOP, Token.RelopAttr.NEQ.ordinal(), lex, srcPos);
            } else if (source.hasNext(srcPos) && source.peek(srcPos) == '=') {
              lex += source.advanceChar(srcPos);
              return new Token(Token.Type.RELOP, Token.RelopAttr.LTE.ordinal(), lex, srcPos);
            } else {
              return new Token(Token.Type.RELOP, Token.RelopAttr.LT.ordinal(), lex, srcPos);
            }
          }
          break;
        case ">":
          if (source.hasNext(srcPos)) {
            if (source.hasNext(srcPos) && source.peek(srcPos) == '=') {
              lex += source.advanceChar(srcPos);
              return new Token(Token.Type.RELOP, Token.RelopAttr.GTE.ordinal(), lex, srcPos);
            } else {
              return new Token(Token.Type.RELOP, Token.RelopAttr.GT.ordinal(), lex, srcPos);
            }
          }
          break;
      }
    }

    // if no token matched, revert source pointer and return null
    srcPos = backup;
    return null;
  }

  private Token intMachine() {
    SourcePointer backup = srcPos.clone();

    if (source.hasNext(srcPos) && isDigit(source.peek(srcPos))) {
      String lex = "" + source.advanceChar(srcPos);

      while (source.hasNext(srcPos) && isDigit(source.peek(srcPos))) {
        lex += source.advanceChar(srcPos);
      }

      if (lex.startsWith("00"))
        return new Token(Token.Type.LEXERR, "Invalid INT: multiple leading zeros", lex, srcPos);
      if (lex.length() > 10)
        return new Token(Token.Type.LEXERR, "Invalid INT: too long", lex, srcPos);


      return new Token(Token.Type.NUM, lex, lex, srcPos);

    }

    // if no token matched, revert source pointer and return null
    srcPos = backup;
    return null;
  }

  private Token realMachine() {
    SourcePointer backup = srcPos.clone();

    String lex = "";
    int xCount = 0;
    boolean hasDot = false;
    int yCount = 0;
    boolean hasExp = false;
    int zCount = 0;

    while (source.hasNext(srcPos) && isDigit(source.peek(srcPos))) {
      xCount++;
      lex += source.advanceChar(srcPos);
    }

    if (source.hasNext(srcPos) && source.peek(srcPos) == '.') {
      hasDot = true;
      lex += source.advanceChar(srcPos);

      while (source.hasNext(srcPos) && isDigit(source.peek(srcPos))) {
        yCount++;
        lex += source.advanceChar(srcPos);
      }
    }

    SourcePointer notLongBackup = srcPos.clone();
    if (source.hasNext(srcPos) && source.peek(srcPos) == 'E') {
      hasExp = true;
      lex += source.advanceChar(srcPos);

      if (source.hasNext(srcPos) && (source.peek(srcPos) == '+' || source.peek(srcPos) == '-')) {
        lex += source.advanceChar(srcPos);
      }

      while (source.hasNext(srcPos) && isDigit(source.peek(srcPos))) {
        zCount++;
        lex += source.advanceChar(srcPos);
      }
    }


    if (xCount > 0 && hasDot && yCount > 0) {
      if (lex.startsWith("00"))
        return new Token(Token.Type.LEXERR, "Invalid REAL: multiple leading zeros in xx", lex,
            srcPos);
      if (xCount > 5)
        return new Token(Token.Type.LEXERR, "Invalid REAL: xx too long", lex, srcPos);
      if (yCount > 5)
        return new Token(Token.Type.LEXERR, "Invalid REAL: yy too long", lex, srcPos);

      if (hasExp && zCount > 0) {
        if (zCount > 2)
          return new Token(Token.Type.LEXERR, "Invalid REAL: zz too long", lex, srcPos);
        else if (lex.substring(lex.length() - zCount).startsWith("00"))
          return new Token(Token.Type.LEXERR, "Invalid REAL: multiple leading zeros in zz", lex,
              srcPos);
        else
          return new Token(Token.Type.NUM, lex, lex, srcPos);
      } else {
        srcPos = notLongBackup;
        return new Token(Token.Type.NUM, lex, lex, srcPos);
      }
    }


    // if no token matched, revert source pointer and return null
    srcPos = backup;
    return null;
  }

  private Token catchAllMachine() {
    SourcePointer backup = srcPos.clone();
    String lex = "" + source.advanceChar(srcPos);

    switch (lex) {
      case "(":
        return new Token(Token.Type.OPENPAREN, null, lex, srcPos);
      case ")":
        return new Token(Token.Type.CLOSEPAREN, null, lex, srcPos);
      case ";":
        return new Token(Token.Type.SEMICOLON, null, lex, srcPos);
      case ",":
        return new Token(Token.Type.COMMA, null, lex, srcPos);
      case "[":
        return new Token(Token.Type.OPENBRACKET, null, lex, srcPos);
      case "]":
        return new Token(Token.Type.CLOSEBRACKET, null, lex, srcPos);
      case "+":
        return new Token(Token.Type.ADDOP, Token.AddopAttr.PLUS.ordinal(), lex, srcPos);
      case "-":
        return new Token(Token.Type.ADDOP, Token.AddopAttr.MINUS.ordinal(), lex, srcPos);
      case "*":
        return new Token(Token.Type.MULOP, Token.MulopAttr.TIMES.ordinal(), lex, srcPos);
      case "/":
        return new Token(Token.Type.MULOP, Token.MulopAttr.SLASH.ordinal(), lex, srcPos);
    }

    if (lex.equals(":")) {
      if (source.hasNext(srcPos) && source.peek(srcPos) == '=') {
        lex += source.advanceChar(srcPos);
        return new Token(Token.Type.ASSIGNOP, null, lex, srcPos);
      } else
        return new Token(Token.Type.COLON, null, lex, srcPos);
    } else if (lex.equals(".")) {
      if (source.hasNext(srcPos) && source.peek(srcPos) == '.') {
        lex += source.advanceChar(srcPos);
        return new Token(Token.Type.DOTDOT, null, lex, srcPos);
      } else {
        return new Token(Token.Type.EOF, null, lex, srcPos);
      }
    }

    Token err = new Token(Token.Type.LEXERR, "Unrecog Symbol", lex, srcPos);
    return err;
  }

  public void computeProjectZero() {
    for (int i = 0; i < source.getNumLines(); i++) {
      out.print(i + ". " + source.getLine(i));
    }
  }

  public static Scanner getFile(String filepath) {
    try {
      return new Scanner(new BufferedReader(new FileReader(filepath)));
    } catch (FileNotFoundException e) {
      out.println("Source not found at " + filepath);
      return null;
    }
  }

  public void writeListingFile(String string) {
    PrintWriter output = null;
    try {
      output = new PrintWriter(string);
    } catch (FileNotFoundException e) {
    }

    int lineNo = -1;
    for (Token t : tokens) {
      while (t.position.lineNum > lineNo) {
        lineNo++;
        output.print(String.format("%-8s", "" + lineNo) + source.getLine(lineNo));
      }
      if (t.type == Token.Type.LEXERR)
        output.println("LEXERR: " + t.attribute);
    }
    output.close();
  }

  public void writeTokenFile(String string) {
    PrintWriter output = null;
    try {
      output = new PrintWriter(string);
    } catch (FileNotFoundException e) {
    }

    String formatting = "%-10s%-20s%-20s%-10s";
    output.println(String.format(formatting, "Line No.", "Lexeme", "TOKEN-TYPE", "ATTRIBUTE"));
    for (Token t : tokens) {
      output.println(String.format(formatting, t.position.lineNum, t.lexeme, t.type.toString(),
          t.getAttribute()));
    }
    output.close();
  }
}
