/**
 * @author Eric Kuxhausen
 */
public class Project0 {

  public static void main(String[] args) {

    Parser p = new Parser(Parser.getFile("input/sampleFromGrammar.pas"));
    p.computeProjectZero();
  }

}
