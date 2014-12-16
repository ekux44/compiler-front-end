import java.util.Scanner;

/**
 * @author Eric Kuxhausen
 */
public class Project1 {

  public static void main(String[] args) {
    for (String filename : args) {
      Scanner file = Parser.getFile("input/" + filename + ".pas");
      if (file != null) {
        Parser p = new Parser(file);
        while (true) {
          if (p.getNextToken() == null)
            break;
        }
        p.writeListingFile("output/" + filename + ".listing");
        p.writeTokenFile("output/" + filename + ".token");

      }
    }
  }

}
