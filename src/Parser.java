import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.io.*;

import static java.lang.System.out;


public class Parser {

  private HashMap<String, Token> reservedWordTable = new HashMap<String, Token>();
  private SourceBuffer source = new SourceBuffer();

  public Parser(Scanner file) {
    loadReservedWordTable();

    while (file.hasNextLine()) {
      // Read source into buffer
      // Per project spec, only consider upto 72 characters per line.
      String line = file.nextLine();
      source.addLine(line.substring(0, Math.min(72, line.length())));
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

  /**
   * consumes whitespace
   */
  private void whitespaceMachine() {
    while (source.hasNextCharacter()) {
      if (isWhiteSpace(source.peekNextCharacter())) {
        source.advanceNextCharacter(1);
      }
    }
  }

  private boolean isWhiteSpace(char c) {
    if (c == ' ' || c == '\t' || c == '\n')
      return true;
    return false;
  }

  private Token relopMachine() {
    // TODO
    return null;
  }

  private Token idResMachine() {
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
      out.println(i + ". " + source.getLine(i));
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
