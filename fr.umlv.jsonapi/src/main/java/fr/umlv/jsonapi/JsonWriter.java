package fr.umlv.jsonapi;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.Objects;

/**
 * A visitor implementing bot {@link ObjectVisitor} and {@link ArrayVisitor} interfaces able to
 * encode a series of visit calls to JSON fragments to an IO Writer.
 *
 * <p>Given that a json writer store an IO writer, it should be used in a try-with-resources
 * to avoid leaking IO system descriptor
 * <pre>
 *   Path path = ...
 *   try(var writer = Files.newBufferedWriter(path);
 *       var jsonWriter = new JsonWriter(writer)) {
 *       ...
 *   }
 * </pre>
 *
 * @see JsonPrinter
 */
public final class JsonWriter implements ObjectVisitor, ArrayVisitor, Closeable {
  private final Writer writer;
  private String separator = "";

  /**
   * Creates a JSON writer using an underlying  {@code writer}
   * @param writer an IO writer to write the JSON fragments
   */
  public JsonWriter(Writer writer) {
    Objects.requireNonNull(writer);
    this.writer = writer;
  }

  /**
   * Close the underlying {@code written}
   * @throws IOException if an IO error occurs
   */
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