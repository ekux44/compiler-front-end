package kuxhausen;

public class BlueNode implements Node{

  private String mName;
  
  @Override
  public void setName(String name) {
    mName = name;
  }

  @Override
  public String getName() {
    return mName;
  }
}
