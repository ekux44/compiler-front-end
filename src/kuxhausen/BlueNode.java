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

  public PasType getPPFreeType() {
    switch (mType) {
      case PPINT:
        return PasType.INT;
      case PPREAL:
        return PasType.REAL;
      case PPAINT:
        return PasType.AINT;
      case PPAREAL:
        return PasType.AREAL;
      default:
        return mType;
    }
  }

  public void setType(PasType t) {
    mType = t;
  }
}
