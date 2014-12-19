package kuxhausen;

public class BlueNode implements Node {

  private String mName;
  private PasType mType;

  @Override
  public String getName() {
    return mName;
  }

  @Override
  public void setName(String name) {
    mName = name;
  }

  public PasType getType() {
    return mType;
  }

  public void setType(PasType t) {
    mType = t;
  }
}
