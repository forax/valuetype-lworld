package fr.umlv.jsonapi;

import static com.fasterxml.jackson.core.JsonToken.END_ARRAY;
import static com.fasterxml.jackson.core.JsonToken.END_OBJECT;
import static com.fasterxml.jackson.core.JsonToken.FIELD_NAME;
import static com.fasterxml.jackson.core.JsonToken.START_ARRAY;
import static com.fasterxml.jackson.core.JsonToken.START_OBJECT;
import static com.fasterxml.jackson.core.JsonToken.VALUE_TRUE;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public final class JsonReader {
  private JsonReader() {
    throw new AssertionError();
  }

  public static void parse(Path path, JsonVisitor visitor) throws IOException {
    Objects.requireNonNull(path);
    Objects.requireNonNull(visitor);
    try(var reader = Files.newBufferedReader(path)) {
      parse(reader, visitor);
    }
  }
  public static void parse(Path path, JsonObjectVisitor visitor) throws IOException {
    Objects.requireNonNull(path);
    Objects.requireNonNull(visitor);
    parse(path, asJsonVisitor(visitor));
  }
  public static void parse(String text, JsonVisitor visitor) {
    Objects.requireNonNull(text);
    Objects.requireNonNull(visitor);
    try {
      parse(new StringReader(text), visitor);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
  public static void parse(String text, JsonObjectVisitor visitor) {
    Objects.requireNonNull(text);
    Objects.requireNonNull(visitor);
    parse(text, asJsonVisitor(visitor));
  }
  public static void parse(Reader reader, JsonVisitor visitor) throws IOException {
    Objects.requireNonNull(reader);
    Objects.requireNonNull(visitor);
    try(var parser = new JsonFactory().createParser(reader)) {
      parseJson(parser, visitor);
    }
  }
  public static void parse(Reader reader, JsonObjectVisitor visitor) throws IOException {
    Objects.requireNonNull(reader);
    Objects.requireNonNull(visitor);
    parse(reader, asJsonVisitor(visitor));
  }

  private static JsonVisitor asJsonVisitor(JsonObjectVisitor objectVisitor) {
    return new JsonVisitor() {
      @Override
      public JsonObjectVisitor visitObject() {
        return objectVisitor;
      }

      @Override
      public JsonArrayVisitor visitArray() {
        throw new IllegalStateException("only a json object is supported");
      }
    };
  }

  private static void parseJson(JsonParser parser, JsonVisitor visitor) throws IOException {
    var stack = new ArrayDeque<JsonToken>();
    var token = parser.nextToken();
    if (token == START_OBJECT) {
      parseOrSkipObject(parser, visitor.visitObject(), stack);
      return;
    }
    if (token == START_ARRAY) {
      parseOrSkipArray(parser, visitor.visitArray(), stack);
      return;
    }
    throw new IOException("invalid token " + token);
  }

  private static void skipUntil(JsonParser parser, JsonToken endToken, ArrayDeque<JsonToken> stack) throws IOException {
    stack.push(endToken);
    for(;;) {
      var token = parser.nextToken();
      switch(token) {
        case START_OBJECT -> { stack.push(END_OBJECT); continue; }
        case START_ARRAY -> { stack.push(END_ARRAY); continue; }
        case FIELD_NAME, VALUE_STRING, VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT,
            VALUE_TRUE, VALUE_FALSE, VALUE_NULL -> { continue; }
        case END_ARRAY, END_OBJECT -> {
          if (stack.pop() == token) {
            if (stack.isEmpty()) {
              return;
            }
            continue;
          }
          throw new IOException("invalid token " + token);
        }
        default -> throw new IOException("invalid token " + token);
      }
    }
  }

  private static void parseOrSkipObject(JsonParser parser, JsonObjectVisitor objectVisitor, ArrayDeque<JsonToken> stack) throws IOException {
    if (objectVisitor == null) {
      skipUntil(parser, END_OBJECT, stack);
    } else {
      readObject(parser, objectVisitor, stack);
    }
  }
  private static void parseOrSkipArray(JsonParser parser, JsonArrayVisitor arrayVisitor, ArrayDeque<JsonToken> stack) throws IOException {
    if (arrayVisitor == null) {
      skipUntil(parser, END_ARRAY, stack);
    } else {
      readArray(parser, arrayVisitor, stack);
    }
  }

  private static void readArray(JsonParser parser, JsonArrayVisitor visitor, ArrayDeque<JsonToken> stack) throws IOException {
    for(;;) {
      var token = parser.nextToken();
      switch(token) {
        case START_OBJECT -> parseOrSkipObject(parser, visitor.visitObject(), stack);
        case START_ARRAY -> parseOrSkipArray(parser, visitor.visitArray(), stack);
        case VALUE_STRING -> visitor.visitString(parser.getValueAsString());
        case VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT -> {
          switch(parser.getNumberType()) {
            case INT -> visitor.visitNumber(parser.getValueAsInt());
            case LONG -> visitor.visitNumber(parser.getValueAsLong());
            case BIG_INTEGER -> visitor.visitNumber(new BigInteger(parser.getValueAsString()));
            case FLOAT, DOUBLE -> visitor.visitNumber(parser.getValueAsDouble());
            default -> throw new IOException("invalid number " + parser.getValueAsString());
          }
        }
        case VALUE_TRUE, VALUE_FALSE -> visitor.visitBoolean(token == VALUE_TRUE);
        case VALUE_NULL -> visitor.visitNull();
        case END_ARRAY -> { visitor.visitEndArray(); return; }
        default -> throw new IOException("invalid token " + token);
      }
    }
  }

  private static void readObject(JsonParser parser, JsonObjectVisitor visitor, ArrayDeque<JsonToken> stack) throws IOException {
    for(;;) {
      var token = parser.nextToken();
      if (token == END_OBJECT) {
        visitor.visitEndObject();
        return;
      }
      if (token != FIELD_NAME) {
        throw new IOException("invalid token " + token);
      }
      var name = parser.getCurrentName();
      token = parser.nextToken();
      switch(token) {
        case START_OBJECT -> parseOrSkipObject(parser, visitor.visitMemberObject(name), stack);
        case START_ARRAY -> parseOrSkipArray(parser, visitor.visitMemberArray(name), stack);
        case VALUE_STRING -> visitor.visitMemberString(name, parser.getValueAsString());
        case VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT -> {
          switch(parser.getNumberType()) {
            case INT -> visitor.visitMemberNumber(name, parser.getValueAsInt());
            case LONG -> visitor.visitMemberNumber(name, parser.getValueAsLong());
            case BIG_INTEGER -> visitor.visitMemberNumber(name, new BigInteger(parser.getValueAsString()));
            case FLOAT, DOUBLE -> visitor.visitMemberNumber(name, parser.getValueAsDouble());
            default -> throw new IOException("invalid number " + parser.getValueAsString());
          }
        }
        case VALUE_TRUE, VALUE_FALSE -> visitor.visitMemberBoolean(name, token == VALUE_TRUE);
        case VALUE_NULL -> visitor.visitMemberNull(name);
        default -> throw new IOException("invalid token " + token);
      }
    }
  }
}