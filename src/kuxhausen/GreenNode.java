package kuxhausen;

import java.util.ArrayList;

public class GreenNode implements Node{

  private String mName;
  ArrayList<Node> mChildren = new ArrayList<Node>();

  @Override
  public void setName(String name) {
    mName = name;
  }

  @Override
  public String getName() {
    return mName;
  }
}
