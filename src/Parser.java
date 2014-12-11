import java.util.ArrayList;
import java.util.Scanner;
import java.io.*;

import static java.lang.System.out;


public class Parser {

  ArrayList<String> sourceBuffer = new ArrayList<String>();

  public Parser(Scanner file) {
    while (file.hasNext()) {
      // Read source into buffer
      // Per project spec, only consider upto 72 characters per line.
      String line = file.nextLine();
      sourceBuffer.add(line.substring(0, Math.min(72, line.length())));
    }
    file.close();
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
      out.println(filepath + " Not Found");

      e.printStackTrace();
      return null;
    }
  }

}
