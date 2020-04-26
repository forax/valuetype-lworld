package fr.umlv.jsonapi;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

public final class JsonWriter implements ObjectVisitor, ArrayVisitor, Closeable {
  private final JsonGenerator generator;

  public JsonWriter(Writer writer) throws IOException {
    Objects.requireNonNull(writer);
    generator = new JsonFactory().createGenerator(writer);
  }

  public JsonWriter(Path path) throws IOException {
    Objects.requireNonNull(path);
    var writer = Files.newBufferedWriter(path);
    try {
      generator = new JsonFactory().createGenerator(writer);
    } catch(IOException | OutOfMemoryError e) {
      writer.close();
      throw e;
    }
  }

  @Override
  public void close() throws IOException {
    generator.close();
  }

  @Override
  public JsonWriter visitMemberObject(String name) {
    Objects.requireNonNull(name);
    try {
      generator.writeFieldName(name);
      generator.writeStartObject();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return this;
  }

  @Override
  public JsonWriter visitMemberArray(String name) {
    Objects.requireNonNull(name);
    try {
      generator.writeFieldName(name);
      generator.writeStartArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return this;
  }

  @Override
  public void visitMemberValue(String name, JsonValue value) {
    Objects.requireNonNull(name);
    try {
      switch(value.kind()) {
        case NULL -> generator.writeNullField(name);
        case TRUE, FALSE -> generator.writeBooleanField(name, value.booleanValue());
        case INT -> generator.writeNumberField(name, value.intValue());
        case LONG -> generator.writeNumberField(name, value.longValue());
        case DOUBLE -> generator.writeNumberField(name, value.doubleValue());
        case BIG_INTEGER -> { generator.writeFieldName(name); generator.writeNumber(value.bigIntegerValue()); }
        case BIG_DECIMAL -> generator.writeNumberField(name, value.bigDecimalValue());
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public Void visitEndObject() {
    try {
      generator.writeEndObject();
      return null;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public JsonWriter visitObject() {
    try {
      generator.writeStartObject();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return this;
  }

  @Override
  public JsonWriter visitArray() {
    try {
      generator.writeStartArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return this;
  }

  @Override
  public Void visitValue(JsonValue value) {
    try {
      switch(value.kind()) {
        case NULL -> generator.writeNull();
        case TRUE, FALSE -> generator.writeBoolean(value.booleanValue());
        case INT -> generator.writeNumber(value.intValue());
        case LONG -> generator.writeNumber(value.longValue());
        case DOUBLE -> generator.writeNumber(value.doubleValue());
        case BIG_INTEGER -> generator.writeNumber(value.bigIntegerValue());
        case BIG_DECIMAL -> generator.writeNumber(value.bigDecimalValue());
      }
      return null;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public Void visitEndArray(Object result) {
    try {
      generator.writeEndArray();
      return null;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}