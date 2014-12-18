package kuxhausen;

import java.util.Scanner;

/**
 * @author Eric Kuxhausen
 */
public class Project2 {
  public static void main(String[] args) {
    for (String filename : args) {
      Scanner file = Lexar.getFile("input/" + filename + ".pas");
      if (file != null) {
        Lexar l = new Lexar(file);
        Parser p = new Parser(l);

        p.writeListingFile("output/" + filename + ".listing");
        p.writeTokenFile("output/" + filename + ".token");

      }
    }
  }
}
