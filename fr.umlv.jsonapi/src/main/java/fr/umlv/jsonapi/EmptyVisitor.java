package fr.umlv.jsonapi;

import static java.util.Objects.requireNonNull;

public class EmptyVisitor implements ObjectVisitor, ArrayVisitor {
  @Override
  public ObjectVisitor visitMemberObject(String name) {
    requireNonNull(name);
    return null;
  }
  @Override
  public ArrayVisitor visitMemberArray(String name) {
    requireNonNull(name);
    return null;
  }
  @Override
  public void visitMemberValue(String name, JsonValue value) {
    requireNonNull(name);
  }
  @Override
  public Object visitEndObject() {
    return null;
  }

  @Override
  public ObjectVisitor visitObject() {
    return null;
  }
  @Override
  public ArrayVisitor visitArray() {
    return null;
  }
  @Override
  public void visitValue(JsonValue value) {
    // empty
  }
  @Override
  public Object visitEndArray() {
    return null;
  }
}