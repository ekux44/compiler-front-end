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
      Scanner wordFile = new Scanner(new BufferedReader(new FileReader("reservedwords.txt")));

      while (wordFile.hasNextLine()) {
        String lexeme = wordFile.next();
        String tokenType = wordFile.next();
        int attribute = wordFile.nextInt();

        for (TokenType.ReservedWordTypes tt : TokenType.ReservedWordTypes.values()) {
          if (lexeme.equals(tt.toString())) {
            reservedWordTable.put(lexeme, new Token(new TokenType(tt), attribute));
          }
        }
      }



    } catch (FileNotFoundException e) {
      out.println("reservedwords.txt not found");
    }
  }

  public Token getNextToken() {
    Token result = null;


    return null;
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

  private Token idResMachine() {
    SourceBuffer.SourcePointer backup = srcPosition.clone();

    // first consume whitespace expected before id / reserved words
    boolean hasConsumedWhitespace = false;
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
          } else { // else an id to check add to symbol table
            Token t = new Token(new TokenType(TokenType.OtherTypes.ID), candidate);
            if (!symbols.table.containsKey(candidate))
              symbols.table.put(candidate, t);
            return t;
          }
        }
      }
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

  private Token catchallMachine() {
    // TODO
    return null;
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
}
