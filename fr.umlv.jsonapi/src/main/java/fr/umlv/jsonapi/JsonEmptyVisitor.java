package fr.umlv.jsonapi;

import static java.util.Objects.requireNonNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

public class JsonEmptyVisitor implements JsonObjectVisitor, JsonArrayVisitor {
  @Override
  public JsonObjectVisitor visitMemberObject(String name) {
    requireNonNull(name);
    return null;
  }
  @Override
  public JsonArrayVisitor visitMemberArray(String name) {
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
  public JsonObjectVisitor visitObject() {
    return null;
  }
  @Override
  public JsonArrayVisitor visitArray() {
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