import java.util.ArrayList;


public class SourceBuffer {
  private ArrayList<String> sourceBuffer = new ArrayList<String>();
  
  private int positionInSourceBuffer = 0;
  private int positionInSourceBufferLine = 0;

  public SourceBuffer(){
  }
  
  public void addLine(String line){
    sourceBuffer.add(line);
  }
  
  public int getNumLines(){
    return sourceBuffer.size();
  }
  
  public String getLine(int number){
    return sourceBuffer.get(number);
  }
  
  public boolean hasNextCharacter(){
    if(positionInSourceBuffer < sourceBuffer.size()
        && positionInSourceBufferLine < sourceBuffer.get(positionInSourceBuffer).length()){
    return true;
    }
    else
      return false;
  }
  
  /**
   * guard with hasNextCharacter() to prevent out of bounds issues
   */
  public char peekNextCharacter(){
      return sourceBuffer.get(positionInSourceBuffer).charAt(positionInSourceBufferLine);
  }
  
  public void advanceNextCharacter(int offset){
    for(int i=0; i< offset; i++){
      if(hasNextCharacter()){
        if(positionInSourceBufferLine < sourceBuffer.get(positionInSourceBuffer).length()){
          positionInSourceBufferLine++;
        } else {
          positionInSourceBuffer++;
          positionInSourceBufferLine=0;
        }
      }
    }
  }
}
