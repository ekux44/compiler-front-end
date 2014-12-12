import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.io.*;

import static java.lang.System.out;


public class Parser {

  ArrayList<String> sourceBuffer = new ArrayList<String>();
  HashMap<String, Token> reservedWordTable = new HashMap<String, Token>();


  public Parser(Scanner file) {
    loadReservedWordTable();

    while (file.hasNextLine()) {
      // Read source into buffer
      // Per project spec, only consider upto 72 characters per line.
      String line = file.nextLine();
      sourceBuffer.add(line.substring(0, Math.min(72, line.length())));
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

  public void computeProjectZero() {
    for (int i = 0; i < sourceBuffer.size(); i++) {
      out.println(i + ". " + sourceBuffer.get(i));
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
