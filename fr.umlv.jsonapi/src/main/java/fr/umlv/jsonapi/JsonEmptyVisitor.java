package fr.umlv.jsonapi;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

public class JsonEmptyVisitor implements JsonObjectVisitor, JsonArrayVisitor {
  @Override
  public JsonObjectVisitor visitMemberObject(String name) {
    return null;
  }
  @Override
  public JsonArrayVisitor visitMemberArray(String name) {
    return null;
  }
  @Override
  public void visitMemberText(String name, JsonText text) {
    Objects.requireNonNull(name);
  }
  @Override
  public void visitMemberNumber(String name, JsonNumber number) {
    Objects.requireNonNull(name);
  }
  @Override
  public void visitMemberConstant(String name, JsonConstant constant) {
    Objects.requireNonNull(name);
  }
  @Override
  public void visitEndObject() {
    // empty
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
  public void visitText(JsonText text) {
    // empty
  }
  @Override
  public void visitNumber(JsonNumber number) {
    // empty
  }
  @Override
  public void visitConstant(JsonConstant constant) {
    // empty
  }
  @Override
  public void visitEndArray() {
    // empty
  }
}