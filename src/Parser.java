import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.io.*;

import static java.lang.System.out;

/**
 * @author Eric Kuxhausen
 */
public class Parser {

  private HashMap<String, Token> reservedWordTable = new HashMap<String, Token>();
  private SourceBuffer source = new SourceBuffer();
  private SourcePointer srcPos = new SourcePointer();
  private SymbolTable symbols = new SymbolTable();
  private ArrayList<Token> tokens = new ArrayList<Token>();

  public Parser(Scanner file) {
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

        for (Token.ResWordAttr tt : Token.ResWordAttr.values()) {
          if (resType.equals(tt.toString())) {
            reservedWordTable.put(lexeme, new Token(Token.Type.RESWRD, tt, lexeme, srcPos));
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

  public boolean hasNextToken() {
    return source.hasNext(srcPos);
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
      result = eofMachine();
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
        candidate += source.peek(srcPos);
        source.advanceChar(srcPos);

        // next consume any following letters or digits
        while (source.hasNext(srcPos)
            && (isLetter(source.peek(srcPos)) || isDigit(source.peek(srcPos)))) {
          candidate += source.peek(srcPos);
          source.advanceChar(srcPos);
        }

        // if candidate is followed by whitespace or EOF
        if (source.hasNext(srcPos)
            && (isWhiteSpace(source.peek(srcPos)) || isEOF(source.peek(srcPos)))) {

          // check reserved word table
          if (reservedWordTable.containsKey(candidate)) {
            return reservedWordTable.get(candidate);
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
      candidate += source.peek(srcPos);
      source.advanceChar(srcPos);

      // next consume any following letters or digits
      while (source.hasNext(srcPos)
          && (isLetter(source.peek(srcPos)) || isDigit(source.peek(srcPos)))) {
        candidate += source.peek(srcPos);
        source.advanceChar(srcPos);
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
      String first = "" + source.peek(srcPos);
      source.advanceChar(srcPos);
      switch (first) {
        case "=":
          return new Token(Token.Type.RELOP, Token.RelopAttr.EQ, first, srcPos);
        case "<":
          if (source.hasNext(srcPos)) {
            if (source.hasNext(srcPos) && source.peek(srcPos) == '>') {
              source.advanceChar(srcPos);
              return new Token(Token.Type.RELOP, Token.RelopAttr.NEQ, first, srcPos);
            } else if (source.hasNext(srcPos) && source.peek(srcPos) == '=') {
              source.advanceChar(srcPos);
              return new Token(Token.Type.RELOP, Token.RelopAttr.LTE, first, srcPos);
            } else {
              return new Token(Token.Type.RELOP, Token.RelopAttr.LT, first, srcPos);
            }
          }
          break;
        case ">":
          if (source.hasNext(srcPos)) {
            if (source.hasNext(srcPos) && source.peek(srcPos) == '=') {
              source.advanceChar(srcPos);
              return new Token(Token.Type.RELOP, Token.RelopAttr.GTE, first, srcPos);
            } else {
              return new Token(Token.Type.RELOP, Token.RelopAttr.GT, first, srcPos);
            }
          }
          break;
      }
    }

    // if no token matched, revert source pointer and return null
    srcPos = backup;
    return null;
  }

  private Token eofMachine() {
    if (source.hasNext(srcPos) && isEOF(source.peek(srcPos))) {
      source.advanceChar(srcPos);
      return new Token(Token.Type.EOF, null, ".", srcPos);
    }
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

    eval: if (source.hasNext(srcPos) && isDigit(source.peek(srcPos))) {
      String lex = "" + source.advanceChar(srcPos);

      while (source.hasNext(srcPos) && isDigit(source.peek(srcPos))) {
        lex += source.advanceChar(srcPos);
      }

      if (source.hasNext(srcPos) && source.peek(srcPos) == '.') {
        lex += source.advanceChar(srcPos);

        if (source.hasNext(srcPos) && isDigit(source.peek(srcPos))) {
          lex += source.advanceChar(srcPos);

          while (source.hasNext(srcPos) && isDigit(source.peek(srcPos))) {
            lex += source.advanceChar(srcPos);
          }

          int dotIndex = lex.indexOf('.');

          if (lex.startsWith("00"))
            return new Token(Token.Type.LEXERR, "Invalid REAL: multiple leading zeros in xx", lex,
                srcPos);
          if (lex.substring(0, dotIndex).length() > 5)
            return new Token(Token.Type.LEXERR, "Invalid REAL: xx too long", lex, srcPos);
          if (lex.substring(dotIndex + 1).length() > 5)
            return new Token(Token.Type.LEXERR, "Invalid REAL: yy too long", lex, srcPos);

          return new Token(Token.Type.NUM, lex, lex, srcPos);
        }
      }
    }

    // if no token matched, revert source pointer and return null
    srcPos = backup;
    return null;
  }

  private Token longrealMachine() {
    // TODO
    return null;
  }

  private Token catchAllMachine() {
    // TODO

    String lex = "" + source.peek(srcPos);


    Token result = null;
    switch (lex) {
      case "(":
        result = new Token(Token.Type.OPENPAREN, lex, lex, srcPos);
        break;
      case ")":
        result = new Token(Token.Type.CLOSEPAREN, lex, lex, srcPos);
        break;
      case ";":
        result = new Token(Token.Type.SEMICOLON, lex, lex, srcPos);
        break;
      case ",":
        result = new Token(Token.Type.COMMA, lex, lex, srcPos);
        break;

      case "[":
        result = new Token(Token.Type.OPENBRACKET, lex, lex, srcPos);
        break;
      case "]":
        result = new Token(Token.Type.CLOSEBRACKET, lex, lex, srcPos);
        break;

      case "+":
        result = new Token(Token.Type.ADDOP, Token.AddopAttr.PLUS, lex, srcPos);
        break;
      case "-":
        result = new Token(Token.Type.ADDOP, Token.AddopAttr.MINUS, lex, srcPos);
        break;
      case "*":
        result = new Token(Token.Type.MULOP, Token.MulopAttr.TIMES, lex, srcPos);
        break;
      case "/":
        result = new Token(Token.Type.MULOP, Token.MulopAttr.SLASH, lex, srcPos);
        break;
    }

    if (result != null) {
      source.advanceChar(srcPos);
      return result;
    }


    source.advanceChar(srcPos);

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

    for (int i = 0; i < source.getNumLines(); i++) {
      output.print(i + "   " + source.getLine(i));
      // TODO add formatting and tokens
    }
    output.close();
  }

  public void writeTokenFile(String string) {
    PrintWriter output = null;
    try {
      output = new PrintWriter(string);
    } catch (FileNotFoundException e) {
    }

    String formatting = "%-10s%-15s%-15s%-10s";
    output.println(String.format(formatting, "Line No.", "Lexeme", "TOKEN-TYPE", "ATTRIBUTE"));
    for (Token t : tokens) {
      output.println(String.format(formatting, t.position.lineNum, t.lexeme, t.type.toString(),
          t.attribute));
    }
    output.close();
  }
}
