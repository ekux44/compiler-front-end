/**
 * @author Eric Kuxhausen
 */
public class SourcePointer implements Cloneable {
  public int lineNum;
  public int charInLineNum;

  @Override
  public SourcePointer clone() {
    SourcePointer copy = new SourcePointer();
    copy.lineNum = lineNum;
    copy.charInLineNum = charInLineNum;
    return copy;
  }
}
