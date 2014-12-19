package kuxhausen;

import java.util.Scanner;

/**
 * @author Eric Kuxhausen
 */
public class Project34 {
  public static void main(String[] args) {
    for (String filename : args) {
      Scanner file = Lexar.getFile("input/" + filename + ".pas");
      if (file != null) {
        Lexar l = new Lexar(file);
        DecoratedParser p = new DecoratedParser(l, ("output/" + filename + ".loc"));

        Utils.writeListingFile("output/" + filename + ".listing", p.getTokenList(),
            l.getSourceBuffer());
        Utils.writeTokenFile("output/" + filename + ".token", p.getTokenList());

      }
    }
  }
}
