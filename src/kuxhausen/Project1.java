package kuxhausen;

import java.util.Scanner;

/**
 * @author Eric Kuxhausen
 */
public class Project1 {

  public static void main(String[] args) {
    for (String filename : args) {
      Scanner file = Lexar.getFile("input/" + filename + ".pas");
      if (file != null) {
        Lexar l = new Lexar(file);
        while (true) {
          if (l.getNextToken() == null)
            break;
        }
        Utils.writeListingFile("output/" + filename + ".listing", l.getTokenList(),
            l.getSourceBuffer());
        Utils.writeTokenFile("output/" + filename + ".token", l.getTokenList());

      }
    }
  }

}
