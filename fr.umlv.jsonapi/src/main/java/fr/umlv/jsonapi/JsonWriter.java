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
  public void visitMemberText(String name, JsonText text) {
    Objects.requireNonNull(name);
    try {
      generator.writeStringField(name, text.value());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void visitMemberNumber(String name, JsonNumber number) {
    Objects.requireNonNull(name);
    try {
      if (number.isDouble()) {
        generator.writeNumberField(name, number.doubleValue());
        return;
      }
      generator.writeNumberField(name, number.longValue());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void visitMemberConstant(String name, JsonConstant constant) {
    Objects.requireNonNull(name);
    try {
      switch(constant) {  // implicit nullcheck
        case NULL -> generator.writeNull();
        case TRUE -> generator.writeBooleanField(name, true);
        case FALSE -> generator.writeBooleanField(name, false);
      }
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
  public void visitText(JsonText text) {
    try {
      generator.writeString(text.value());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void visitNumber(JsonNumber number) {
    try {
      if (number.isDouble()) {
        generator.writeNumber(number.doubleValue());
        return;
      }
      generator.writeNumber(number.longValue());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void visitConstant(JsonConstant constant) {
    try {
      switch(constant) {  // implicit nullcheck
        case NULL -> generator.writeNull();
        case TRUE -> generator.writeBoolean(true);
        case FALSE -> generator.writeBoolean(false);
      }
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