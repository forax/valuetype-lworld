package fr.umlv.jsonapi;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

public final class JsonWriter implements JsonObjectVisitor, JsonArrayVisitor, Closeable {
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
  public JsonObjectVisitor visitMemberObject(String name) {
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
  public JsonArrayVisitor visitMemberArray(String name) {
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
  public void visitMemberString(String name, String value) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(value);
    try {
      generator.writeStringField(name, value);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void visitMemberNumber(String name, int value) {
    Objects.requireNonNull(name);
    try {
      generator.writeNumberField(name, value);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void visitMemberNumber(String name, long value) {
    Objects.requireNonNull(name);
    try {
      generator.writeNumberField(name, value);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void visitMemberNumber(String name, double value) {
    Objects.requireNonNull(name);
    try {
      generator.writeNumberField(name, value);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void visitMemberNumber(String name, BigInteger value) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(value);
    try {
      generator.writeFieldName(name);
      generator.writeNumber(value);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void visitMemberBoolean(String name, boolean value) {
    Objects.requireNonNull(name);
    try {
      generator.writeBooleanField(name, value);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void visitMemberNull(String name) {
    Objects.requireNonNull(name);
    try {
      generator.writeNullField(name);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void visitEndObject() {
    try {
      generator.writeEndObject();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public JsonObjectVisitor visitObject() {
    try {
      generator.writeStartObject();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return this;
  }

  @Override
  public JsonArrayVisitor visitArray() {
    try {
      generator.writeStartArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return this;
  }

  @Override
  public void visitString(String value) {
    Objects.requireNonNull(value);
    try {
      generator.writeString(value);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void visitNumber(int value) {
    try {
      generator.writeNumber(value);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void visitNumber(long value) {
    try {
      generator.writeNumber(value);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void visitNumber(double value) {
    try {
      generator.writeNumber(value);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void visitNumber(BigInteger value) {
    Objects.requireNonNull(value);
    try {
      generator.writeNumber(value);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void visitBoolean(boolean value) {
    try {
      generator.writeBoolean(value);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void visitNull() {
    try {
      generator.writeNull();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void visitEndArray() {
    try {
      generator.writeEndArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}