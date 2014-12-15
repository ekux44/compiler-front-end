import java.util.ArrayList;

/**
 * @author Eric Kuxhausen Stores source code with the requirements of: preserving line numbers,
 *         providing access by line number, and facilitating per-character linear traversal with
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

  public boolean hasNext(SourcePointer position) {
    if (position.lineNum < sourceBuffer.size()
        && position.charInLineNum < sourceBuffer.get(position.lineNum).length()) {
      return true;
    } else
      return false;
  }

  /**
   * guard with hasNextCharacter() to prevent out of bounds issues
   */
  public char peek(SourcePointer position) {
    return sourceBuffer.get(position.lineNum).charAt(position.charInLineNum);
  }

  public char advanceChar(SourcePointer position) {
    if (hasNext(position)) {
      char result = peek(position);

      if (position.charInLineNum < sourceBuffer.get(position.lineNum).length() - 1) {
        position.charInLineNum++;
      } else {
        position.lineNum++;
        position.charInLineNum = 0;
      }

      return result;
    }

    return 0;
  }
}
