import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.io.*;

import static java.lang.System.out;


public class Parser {

  private HashMap<String, Token> reservedWordTable = new HashMap<String, Token>();
  private SourceBuffer source = new SourceBuffer();
  private SourceBuffer.SourcePointer srcPosition = new SourceBuffer.SourcePointer();
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
        String tokenType = wordFile.next();
        int attribute = wordFile.nextInt();

        for (TokenType.ReservedWordTypes tt : TokenType.ReservedWordTypes.values()) {
          if (lexeme.equals(tt.toString())) {
            reservedWordTable.put(lexeme, new Token(new TokenType(tt), attribute, lexeme,
                srcPosition.lineNum));
          }
        }
      }

      wordFile.close();
    } catch (FileNotFoundException e) {
      out.println("reservedwords.txt not found");
    }
  }

  public boolean hasNextToken() {
    return source.hasNextChar(srcPosition);
  }

  public Token getNextToken() {
    Token result = null;

    result = reservedWordsMachine();
    if (result == null) {
      whitespaceMachine();
      if (!source.hasNextChar(srcPosition)) // check there is something left after removing
                                            // whitespace
        return result;
      result = idMachine();
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
    SourceBuffer.SourcePointer backup = srcPosition.clone();

    // first consume whitespace expected before id / reserved words
    boolean hasConsumedWhitespace = false;
    if (this.srcPosition.lineNum == 0 && srcPosition.charInLineNum == 0) {
      hasConsumedWhitespace = true; // whitespace not needed before first char in source
    }

    while (source.hasNextChar(srcPosition) && isWhiteSpace(source.readNextChar(srcPosition))) {
      source.advanceNextChar(srcPosition);
      hasConsumedWhitespace = true;
    }

    if (hasConsumedWhitespace) {
      String candidate = "";

      // next consume one letter
      if (source.hasNextChar(srcPosition) && isLetter(source.readNextChar(srcPosition))) {
        candidate += source.readNextChar(srcPosition);
        source.advanceNextChar(srcPosition);

        // next consume any following letters or digits
        while (source.hasNextChar(srcPosition)
            && (isLetter(source.readNextChar(srcPosition)) || isDigit(source
                .readNextChar(srcPosition)))) {
          candidate += source.readNextChar(srcPosition);
          source.advanceNextChar(srcPosition);
        }

        // if candidate is followed by whitespace or EOF
        if (source.hasNextChar(srcPosition)
            && (isWhiteSpace(source.readNextChar(srcPosition)) || isEOF(source
                .readNextChar(srcPosition)))) {

          // check reserved word table
          if (reservedWordTable.containsKey(candidate)) {
            return reservedWordTable.get(candidate);
          }
        }
      }
    }

    // if no token matched, revert source pointer and return null
    srcPosition = backup;
    return null;
  }


  private Token idMachine() {
    SourceBuffer.SourcePointer backup = srcPosition.clone();
    String candidate = "";

    // consume one letter
    if (source.hasNextChar(srcPosition) && isLetter(source.readNextChar(srcPosition))) {
      candidate += source.readNextChar(srcPosition);
      source.advanceNextChar(srcPosition);

      // next consume any following letters or digits
      while (source.hasNextChar(srcPosition)
          && (isLetter(source.readNextChar(srcPosition)) || isDigit(source
              .readNextChar(srcPosition)))) {
        candidate += source.readNextChar(srcPosition);
        source.advanceNextChar(srcPosition);
      }

      // Check add id to symbol table
      Token t =
          new Token(new TokenType(TokenType.OtherTypes.ID), candidate, candidate,
              srcPosition.lineNum);
      if (!symbols.table.containsKey(candidate))
        symbols.table.put(candidate, t);
      return t;
    }

    // if no token matched, revert source pointer and return null
    srcPosition = backup;
    return null;
  }

  /**
   * consumes whitespace
   */
  private void whitespaceMachine() {
    while (source.hasNextChar(srcPosition) && isWhiteSpace(source.readNextChar(srcPosition))) {
      source.advanceNextChar(srcPosition);
    }
  }

  private Token relopMachine() {
    // TODO
    return null;
  }

  private Token eofMachine() {
    // TODO
    return null;
  }

  private Token intMachine() {
    // TODO
    return null;
  }

  private Token longMachine() {
    // TODO
    return null;
  }

  private Token longrealMachine() {
    // TODO
    return null;
  }

  private Token catchAllMachine() {
    // TODO

    String lex = "" + source.readNextChar(srcPosition);


    Token result = null;
    switch (lex) {
      case "(":
        result =
            new Token(new TokenType(TokenType.OtherTypes.OPENPAREN), lex, lex, srcPosition.lineNum);
        break;
      case ")":
        result =
            new Token(new TokenType(TokenType.OtherTypes.CLOSEPAREN), lex, lex, srcPosition.lineNum);
        break;
      case ";":
        result =
            new Token(new TokenType(TokenType.OtherTypes.SEMICOLON), lex, lex, srcPosition.lineNum);
        break;
      case ",":
        result =
            new Token(new TokenType(TokenType.OtherTypes.COMMA), lex, lex, srcPosition.lineNum);
        break;

      case "[":
        result =
            new Token(new TokenType(TokenType.OtherTypes.OPENBRACKET), lex, lex,
                srcPosition.lineNum);
        break;
      case "]":
        result =
            new Token(new TokenType(TokenType.OtherTypes.CLOSEBRACKET), lex, lex,
                srcPosition.lineNum);
        break;

      case "+":
        result =
            new Token(new TokenType(TokenType.OtherTypes.ADDOP), TokenType.AddopAttributes.PLUS,
                lex, srcPosition.lineNum);
        break;
      case "-":
        result =
            new Token(new TokenType(TokenType.OtherTypes.ADDOP), TokenType.AddopAttributes.MINUS,
                lex, srcPosition.lineNum);
        break;
      case "*":
        result =
            new Token(new TokenType(TokenType.OtherTypes.MULOP), TokenType.MulopAttributes.TIMES,
                lex, srcPosition.lineNum);
        break;
      case "/":
        result =
            new Token(new TokenType(TokenType.OtherTypes.MULOP), TokenType.MulopAttributes.SLASH,
                lex, srcPosition.lineNum);
        break;
    }

    if (result != null) {
      source.advanceNextChar(srcPosition);
      return result;
    }



    source.advanceNextChar(srcPosition);

    Token err =
        new Token(new TokenType(TokenType.OtherTypes.LEXERR), "Unrecog Symbol", lex,
            srcPosition.lineNum);
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
      e.printStackTrace();
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
      e.printStackTrace();
    }

    output.println(String.format("%1$-10s%2$-15s%3$-15s%4$-10s", "Line No.", "Lexeme",
        "TOKEN-TYPE", "ATTRIBUTE"));
    for (Token t : tokens) {
      output.println(String.format("%1$-10s%2$-15s%3$-15s%4$-10s", t.lineNum, t.lexeme,
          t.type.toString(), t.attribute));
      // TODO add formatting and tokens
    }
    output.close();
  }
}
