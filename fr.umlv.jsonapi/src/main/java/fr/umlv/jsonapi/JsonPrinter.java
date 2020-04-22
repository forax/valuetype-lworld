package fr.umlv.jsonapi;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

public class JsonPrinter implements JsonObjectVisitor, JsonArrayVisitor {
  private final StringBuilder builder;
  private String separator = "";

  public JsonPrinter(StringBuilder builder) {
    this.builder = Objects.requireNonNull(builder);
  }
  public JsonPrinter() {
    this(new StringBuilder());
  }

  @Override
  public String toString() {
    return builder.toString();
  }

  private static void appendText(StringBuilder builder, String text) {
    builder.append("\"").append(text).append("\""); //FIXME
  }

  @Override
  public JsonObjectVisitor visitMemberObject(String name) {
    Objects.requireNonNull(name);
    builder.append(separator);
    appendText(builder, name);
    builder.append(": { ");
    separator = "";
    return this;
  }

  @Override
  public JsonArrayVisitor visitMemberArray(String name) {
    Objects.requireNonNull(name);
    builder.append(separator);
    appendText(builder, name);
    builder.append("[ ");
    separator = "";
    return this;
  }

  @Override
  public void visitMemberString(String name, String value) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(value);
    builder.append(separator);
    appendText(builder, name);
    builder.append(": ");
    appendText(builder, value);
    separator = ", ";
  }

  @Override
  public void visitMemberNumber(String name, int value) {
    Objects.requireNonNull(name);
    builder.append(separator);
    appendText(builder, name);
    builder.append(": ").append(value);
    separator = ", ";
  }

  @Override
  public void visitMemberNumber(String name, long value) {
    Objects.requireNonNull(name);
    builder.append(separator);
    appendText(builder, name);
    builder.append(": ").append(value);
    separator = ", ";
  }

  @Override
  public void visitMemberNumber(String name, double value) {
    Objects.requireNonNull(name);
    builder.append(separator);
    appendText(builder, name);
    builder.append(": ").append(value);
    separator = ", ";
  }

  @Override
  public void visitMemberNumber(String name, BigInteger value) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(value);
    builder.append(separator);
    appendText(builder, name);
    builder.append(": ").append(value);
    separator = ", ";
  }

  @Override
  public void visitMemberNumber(String name, BigDecimal value) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(value);
    builder.append(separator);
    appendText(builder, name);
    builder.append(": ").append(value);
    separator = ", ";
  }

  @Override
  public void visitMemberBoolean(String name, boolean value) {
    Objects.requireNonNull(name);
    builder.append(separator);
    appendText(builder, name);
    builder.append(": ").append(value);
    separator = ", ";
  }

  @Override
  public void visitMemberNull(String name) {
    Objects.requireNonNull(name);
    builder.append(separator);
    appendText(builder, name);
    builder.append(": null");
    separator = ", ";
  }

  @Override
  public void visitEndObject() {
    builder.append(" }");
    separator = ", ";
  }

  @Override
  public JsonObjectVisitor visitObject() {
    builder.append(separator).append("{ ");
    separator = "";
    return this;
  }

  @Override
  public JsonArrayVisitor visitArray() {
    builder.append(separator).append("[ ");
    separator = "";
    return this;
  }

  @Override
  public void visitString(String value) {
    Objects.requireNonNull(value);
    builder.append(separator);
    appendText(builder, value);
    separator = ", ";
  }

  @Override
  public void visitNumber(int value) {
    builder.append(separator).append(value);
    separator = ", ";
  }

  @Override
  public void visitNumber(long value) {
    builder.append(separator).append(value);
    separator = ", ";
  }

  @Override
  public void visitNumber(double value) {
    builder.append(separator).append(value);
    separator = ", ";
  }

  @Override
  public void visitNumber(BigInteger value) {
    Objects.requireNonNull(value);
    builder.append(separator).append(value);
    separator = ", ";
  }

  @Override
  public void visitNumber(BigDecimal value) {
    Objects.requireNonNull(value);
    builder.append(separator).append(value);
    separator = ", ";
  }

  @Override
  public void visitBoolean(boolean value) {
    builder.append(separator).append(value);
    separator = ", ";
  }

  @Override
  public void visitNull() {
    builder.append(separator).append("null");
    separator = ", ";
  }

  @Override
  public void visitEndArray() {
    builder.append(" ]");
    separator = ", ";
  }
}