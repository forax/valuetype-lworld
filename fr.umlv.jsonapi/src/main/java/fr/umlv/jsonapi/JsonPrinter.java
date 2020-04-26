package fr.umlv.jsonapi;

import java.util.Objects;

public final class JsonPrinter implements ObjectVisitor, ArrayVisitor {
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
    builder.append('"').append(text).append('"'); //FIXME escape ?
  }

  @Override
  public ObjectVisitor visitMemberObject(String name) {
    Objects.requireNonNull(name);
    builder.append(separator);
    appendText(builder, name);
    builder.append(": { ");
    separator = "";
    return this;
  }

  @Override
  public ArrayVisitor visitMemberArray(String name) {
    Objects.requireNonNull(name);
    builder.append(separator);
    appendText(builder, name);
    builder.append("[ ");
    separator = "";
    return this;
  }

  @Override
  public void visitMemberValue(String name, JsonValue value) {
    Objects.requireNonNull(name);
    builder.append(separator);
    appendText(builder, name);
    builder.append(": ").append(value);
    separator = ", ";
  }

  @Override
  public Object visitEndObject() {
    builder.append(" }");
    separator = ", ";
    return null;
  }

  @Override
  public ObjectVisitor visitObject() {
    builder.append(separator).append("{ ");
    separator = "";
    return this;
  }

  @Override
  public ArrayVisitor visitArray() {
    builder.append(separator).append("[ ");
    separator = "";
    return this;
  }

  @Override
  public Void visitValue(JsonValue value) {
    builder.append(separator).append(value);
    separator = ", ";
    return null;
  }

  @Override
  public Object visitEndArray(Object result) {
    builder.append(" ]");
    separator = ", ";
    return result;
  }
}