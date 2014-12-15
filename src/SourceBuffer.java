import java.util.ArrayList;

/**
 * @author kuxhausen Stores source code with the requirements of: preserving line numbers, providing
 *         access by line number, and facilitating per-character linear traversal with
 *         SourcePointers.
 */
public class SourceBuffer {
  private ArrayList<String> sourceBuffer = new ArrayList<String>();

  public SourceBuffer() {}

  public void addLine(String line) {
    sourceBuffer.add(line);
  }

  public int getNumLines() {
    return sourceBuffer.size();
  }

  public String getLine(int number) {
    return sourceBuffer.get(number);
  }

  public boolean hasNextChar(SourcePointer position) {
    if (position.lineNum < sourceBuffer.size()
        && position.charInLineNum < sourceBuffer.get(position.lineNum).length()) {
      return true;
    } else
      return false;
  }

  /**
   * guard with hasNextCharacter() to prevent out of bounds issues
   */
  public char readNextChar(SourcePointer position) {
    return sourceBuffer.get(position.lineNum).charAt(position.charInLineNum);
  }

  public void advanceNextChar(SourcePointer position) {
    if (hasNextChar(position)) {
      if (position.charInLineNum < sourceBuffer.get(position.lineNum).length() - 1) {
        position.charInLineNum++;
      } else {
        position.lineNum++;
        position.charInLineNum = 0;
      }
    }
  }
}
