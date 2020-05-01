package fr.umlv.jsonapi;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class JsonWriter implements ObjectVisitor, ArrayVisitor, Closeable {
  private final Writer writer;
  private String separator = "";

  public JsonWriter(Writer writer) {
    Objects.requireNonNull(writer);
    this.writer = writer;
  }

  public JsonWriter(Path path) throws IOException {
    Objects.requireNonNull(path);
    writer = Files.newBufferedWriter(path);
  }

  @Override
  public void close() throws IOException {
    writer.close();
  }

  @Override
  public VisitorMode visitStartObject() {
    try {
      writer.write("{ ");
      separator = "";
      return VisitorMode.PUSH;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public JsonWriter visitMemberObject(String name) {
    Objects.requireNonNull(name);
    try {
      writer.write(separator + '"' + name + "\": ");
      return this;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public JsonWriter visitMemberArray(String name) {
    Objects.requireNonNull(name);
    try {
      writer.write(separator + '"' + name + "\": ");
      return this;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public Void visitMemberValue(String name, JsonValue value) {
    Objects.requireNonNull(name);
    try {
      writer.write(separator + '"' + name + "\": ");
      writer.write(value.toString());
      separator = ", ";
      return null;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public Void visitEndObject() {
    try {
      writer.write(" }");
      separator = ", ";
      return null;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public VisitorMode visitStartArray() {
    try {
      writer.write("[ ");
      separator = "";
      return VisitorMode.PUSH;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public JsonWriter visitObject() {
    return this;
  }

  @Override
  public JsonWriter visitArray() {
    return this;
  }

  @Override
  public Void visitValue(JsonValue value) {
    try {
      writer.write(separator);
      writer.write(value.toString());
      separator = ", ";
      return null;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public Void visitEndArray() {
    try {
      writer.write(" ]");
      separator = ", ";
      return null;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}