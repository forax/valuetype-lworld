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
  public void visitMemberString(String name, String value) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(value);
  }
  @Override
  public void visitMemberNumber(String name, int value) {
    Objects.requireNonNull(name);
  }
  @Override
  public void visitMemberNumber(String name, long value) {
    Objects.requireNonNull(name);
  }
  @Override
  public void visitMemberNumber(String name, double value) {
    Objects.requireNonNull(name);
  }
  @Override
  public void visitMemberNumber(String name, BigInteger value) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(value);
  }
  @Override
  public void visitMemberBoolean(String name, boolean value) {
    Objects.requireNonNull(name);
  }
  @Override
  public void visitMemberNull(String name) {
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
  public void visitString(String value) {
    // empty
  }
  @Override
  public void visitNumber(int value) {
    // empty
  }
  @Override
  public void visitNumber(long value) {
    // empty
  }
  @Override
  public void visitNumber(double value) {
    // empty
  }
  @Override
  public void visitNumber(BigInteger value) {
    Objects.requireNonNull(value);
  }
  @Override
  public void visitBoolean(boolean value) {
    // empty
  }
  @Override
  public void visitNull() {
    // empty
  }
  @Override
  public void visitEndArray() {
    // empty
  }
}