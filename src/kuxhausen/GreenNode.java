package kuxhausen;

import java.util.ArrayList;

public class GreenNode implements Node {

  private String mName;
  private ArrayList<Node> mChildren = new ArrayList<Node>();

  @Override
  public String getName() {
    return mName;
  }

  @Override
  public void setName(String name) {
    mName = name;
  }

  public ArrayList<Node> getChildren() {
    return mChildren;
  }
}
