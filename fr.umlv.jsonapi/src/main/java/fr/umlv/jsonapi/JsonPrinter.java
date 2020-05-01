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
  public VisitorMode visitStartObject() {
    builder.append("{ ");
    separator = "";
    return VisitorMode.PUSH;
  }

  @Override
  public JsonPrinter visitMemberObject(String name) {
    Objects.requireNonNull(name);
    builder.append(separator);
    appendText(builder, name);
    builder.append(": ");
    return this;
  }

  @Override
  public JsonPrinter visitMemberArray(String name) {
    Objects.requireNonNull(name);
    builder.append(separator);
    appendText(builder, name);
    builder.append(": ");
    return this;
  }

  @Override
  public Object visitMemberValue(String name, JsonValue value) {
    Objects.requireNonNull(name);
    builder.append(separator);
    appendText(builder, name);
    builder.append(": ").append(value);
    separator = ", ";
    return null;
  }

  @Override
  public StringBuilder visitEndObject() {
    builder.append(" }");
    separator = ", ";
    return builder;
  }

  @Override
  public VisitorMode visitStartArray() {
    builder.append("[ ");
    separator = "";
    return VisitorMode.PUSH;
  }

  @Override
  public JsonPrinter visitObject() {
    builder.append(separator);
    return this;
  }

  @Override
  public JsonPrinter visitArray() {
    builder.append(separator);
    return this;
  }

  @Override
  public Void visitValue(JsonValue value) {
    builder.append(separator).append(value);
    separator = ", ";
    return null;
  }

  @Override
  public StringBuilder visitEndArray() {
    builder.append(" ]");
    separator = ", ";
    return builder;
  }
}