package kuxhausen;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import static kuxhausen.Token.*;

public class Utils {

  public static void writeListingFile(String filename, ArrayList<Token> tokens, SourceBuffer source) {
    PrintWriter output = null;
    try {
      output = new PrintWriter(filename);
    } catch (FileNotFoundException e) {
    }

    int lineNo = -1;
    for (Token t : tokens) {
      while (t.position.lineNum > lineNo && (lineNo + 2) <= source.getNumLines()) {
        lineNo++;
        output.print(String.format("%-8s", "" + (lineNo + 1)) + source.getLine(lineNo));
      }
      if (t.type == TokType.LEXERR)
        output.println("LEXERR: " + t.attribute);
      if (t.type == TokType.SYNTAXERR)
        output.println("SYNTAXERR: " + t.attribute);
      if (t.type == TokType.SEMANTICERR)
        output.println("SYNTAXERR: " + t.attribute);
    }
    output.close();
  }

  public static void writeTokenFile(String filename, ArrayList<Token> tokens) {
    PrintWriter output = null;
    try {
      output = new PrintWriter(filename);
    } catch (FileNotFoundException e) {
    }

    String formatting = "%-9s%-17s%-13s%-10s";
    output.println(String.format(formatting, "Line No.", "Lexeme", "TOKEN-TYPE", "ATTRIBUTE"));
    for (Token t : tokens) {
      output.println(String.format(formatting, (t.position.lineNum + 1), t.lexeme,
          t.type.toString(), t.getAttribute()));
    }
    output.close();
  }
}
