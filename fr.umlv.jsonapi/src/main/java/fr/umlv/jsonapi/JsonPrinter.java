package fr.umlv.jsonapi;

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
    builder.append('"').append(text).append('"'); //FIXME escape ?
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
  public void visitMemberText(String name, JsonText text) {
    Objects.requireNonNull(name);
    builder.append(separator);
    appendText(builder, name);
    builder.append(": ");
    appendText(builder, text.value());
    separator = ", ";
  }

  @Override
  public void visitMemberNumber(String name, JsonNumber number) {
    Objects.requireNonNull(name);
    builder.append(separator);
    appendText(builder, name);
    builder.append(": ");
    if (number.isDouble()) {
      builder.append(number.doubleValue());
    } else {
      builder.append(number.longValue());
    }
    separator = ", ";
  }

  @Override
  public void visitMemberConstant(String name, JsonConstant constant) {
    Objects.requireNonNull(constant);
    Objects.requireNonNull(name);
    builder.append(separator);
    appendText(builder, name);
    builder.append(": ").append(constant);
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
  public void visitText(JsonText text) {
    builder.append(separator);
    appendText(builder, text.value());
    separator = ", ";
  }

  @Override
  public void visitNumber(JsonNumber number) {
    builder.append(separator);
    if (number.isDouble()) {
      builder.append(number.doubleValue());
    } else {
      builder.append(number.longValue());
    }
    separator = ", ";
  }

  @Override
  public void visitConstant(JsonConstant constant) {
    Objects.requireNonNull(constant);
    builder.append(separator).append(constant);
    separator = ", ";
  }

  @Override
  public void visitEndArray() {
    builder.append(" ]");
    separator = ", ";
  }
}