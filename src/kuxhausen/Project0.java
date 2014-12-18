package kuxhausen;
/**
 * @author Eric Kuxhausen
 */
public class Project0 {

  public static void main(String[] args) {

    Lexar l = new Lexar(Lexar.getFile("input/sampleFromGrammar.pas"));
    l.computeProjectZero();
  }

}
