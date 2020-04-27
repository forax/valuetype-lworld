package fr.umlv.jsonapi;

import static com.fasterxml.jackson.core.JsonToken.END_ARRAY;
import static com.fasterxml.jackson.core.JsonToken.END_OBJECT;
import static com.fasterxml.jackson.core.JsonToken.FIELD_NAME;
import static com.fasterxml.jackson.core.JsonToken.START_ARRAY;
import static com.fasterxml.jackson.core.JsonToken.START_OBJECT;
import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

public final class JsonReader {
  private record RootVisitor(int kind, ObjectVisitor objectVisitor, ArrayVisitor arrayVisitor) {
    private static final int OBJECT = 1;
    private static final int ARRAY = 2;
    private static final int BOTH = 3;

    public ObjectVisitor visitObject() {
      if ((kind & OBJECT) == 0) {
        throw new IllegalStateException("root is an object but expect an array");
      }
      return objectVisitor;
    }
    public ArrayVisitor visitArray() {
      if ((kind & ARRAY) == 0) {
        throw new IllegalStateException("root is an array but expect an object");
      }
      return arrayVisitor;
    }
  }

  private JsonReader() {
    throw new AssertionError();
  }

  public static Object parse(Path path, Object visitor) throws IOException{
    if (visitor instanceof ObjectVisitor objectVisitor) {
      return parseJson(path, new RootVisitor(RootVisitor.OBJECT, objectVisitor, null));
    }
    if (visitor instanceof ArrayVisitor arrayVisitor) {
      return parseJson(path, new RootVisitor(RootVisitor.ARRAY, null, arrayVisitor));
    }
    throw new IllegalArgumentException("unknown visitor type " + visitor);
  }
  public static Object parse(Path path, ObjectVisitor objectVisitor, ArrayVisitor arrayVisitor) throws IOException {
    requireNonNull(objectVisitor);
    requireNonNull(arrayVisitor);
    return parseJson(path, new RootVisitor(RootVisitor.BOTH, objectVisitor, arrayVisitor));
  }
  private static Object parseJson(Path path, RootVisitor rootVisitor) throws IOException {
    requireNonNull(path);
    try(var reader = Files.newBufferedReader(path)) {
      return parseJson(reader, rootVisitor);
    }
  }

  public static Object parse(String text, Object visitor) {
    if (visitor instanceof ObjectVisitor objectVisitor) {
      return parseJson(text, new RootVisitor(RootVisitor.OBJECT, objectVisitor, null));
    }
    if (visitor instanceof ArrayVisitor arrayVisitor) {
      return parseJson(text, new RootVisitor(RootVisitor.ARRAY, null, arrayVisitor));
    }
    throw new IllegalArgumentException("unknown visitor type " + visitor);
  }
  public static Object parse(String text, ObjectVisitor objectVisitor, ArrayVisitor arrayVisitor) {
    requireNonNull(objectVisitor);
    requireNonNull(arrayVisitor);
    return parseJson(text, new RootVisitor(RootVisitor.BOTH, objectVisitor, arrayVisitor));
  }
  private static Object parseJson(String text, RootVisitor rootVisitor) {
    requireNonNull(text);
    try {
      return parseJson(new StringReader(text), rootVisitor);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static Object parse(Reader reader, Object visitor) throws IOException{
    if (visitor instanceof ObjectVisitor objectVisitor) {
      return parseJson(reader, new RootVisitor(RootVisitor.OBJECT, objectVisitor, null));
    }
    if (visitor instanceof ArrayVisitor arrayVisitor) {
      return parseJson(reader, new RootVisitor(RootVisitor.ARRAY, null, arrayVisitor));
    }
    throw new IllegalArgumentException("unknown visitor type " + visitor);
  }
  public static Object parse(Reader reader, ObjectVisitor objectVisitor, ArrayVisitor arrayVisitor) throws IOException {
    requireNonNull(objectVisitor);
    requireNonNull(arrayVisitor);
    return parseJson(reader, new RootVisitor(RootVisitor.BOTH, objectVisitor, arrayVisitor));
  }
  private static Object parseJson(Reader reader, RootVisitor rootVisitor) throws IOException {
    requireNonNull(reader);
    try(var parser = new JsonFactory().createParser(reader)) {
      return parseJson(parser, rootVisitor);
    }
  }

  private static Object parseJson(JsonParser parser, RootVisitor visitor) throws IOException {
    var stack = new ArrayDeque<JsonToken>();
    var token = parser.nextToken();
    if (token == START_OBJECT) {
      return parseOrSkipObject(parser, visitor.visitObject(), stack);
    }
    if (token == START_ARRAY) {
      return parseOrSkipArray(parser, visitor.visitArray(), stack);
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

  private static Object parseOrSkipObject(JsonParser parser, ObjectVisitor objectVisitor, ArrayDeque<JsonToken> stack) throws IOException {
    if (objectVisitor == null) {
      skipUntil(parser, END_OBJECT, stack);
      return null;
    }
    return readObject(parser, objectVisitor, stack);
  }
  private static Object parseOrSkipArray(JsonParser parser, ArrayVisitor arrayVisitor, ArrayDeque<JsonToken> stack) throws IOException {
    if (arrayVisitor == null) {
      skipUntil(parser, END_ARRAY, stack);
      return null;
    }
    return readArray(parser, arrayVisitor, stack);
  }

  private static Object readArray(JsonParser parser, ArrayVisitor visitor, ArrayDeque<JsonToken> stack) throws IOException {
    if (visitor instanceof StreamVisitor streamVisitor) {
      return readStreamArray(parser, streamVisitor, stack);
    }
    return readPlainArray(parser, visitor, stack);
  }

  private static Object readPlainArray(JsonParser parser, ArrayVisitor visitor, ArrayDeque<JsonToken> stack) throws IOException {
    for(;;) {
      var token = parser.nextToken();
      switch(token) {
        case START_OBJECT -> parseOrSkipObject(parser, visitor.visitObject(), stack);
        case START_ARRAY -> parseOrSkipArray(parser, visitor.visitArray(), stack);
        case VALUE_STRING -> visitor.visitValue(JsonValue.from(parser.getValueAsString()));
        case VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT -> readNumericValue(parser, visitor);
        case VALUE_TRUE -> visitor.visitValue(JsonValue.trueValue());
        case VALUE_FALSE -> visitor.visitValue(JsonValue.falseValue());
        case VALUE_NULL -> visitor.visitValue(JsonValue.nullValue());
        case END_ARRAY -> { return visitor.visitEndArray(null); }
        default -> throw new IOException("invalid token " + token);
      }
    }
  }

  private static Object readNumericValue(JsonParser parser, ArrayVisitor visitor) throws IOException {
    return switch(parser.getNumberType()) {
      case INT -> visitor.visitValue(JsonValue.from(parser.getValueAsInt()));
      case LONG -> visitor.visitValue(JsonValue.from(parser.getValueAsLong()));
      case FLOAT, DOUBLE -> visitor.visitValue(JsonValue.from(parser.getValueAsDouble()));
      case BIG_INTEGER -> visitor.visitValue(JsonValue.from(new BigInteger(parser.getValueAsString())));
      case BIG_DECIMAL -> visitor.visitValue(JsonValue.from(new BigDecimal(parser.getValueAsString())));
    };
  }

  private static Object readStreamArray(JsonParser parser, StreamVisitor visitor, ArrayDeque<JsonToken> stack) throws IOException {
    var spliterator = new Spliterator<>() {
      private boolean ended;
      @Override
      public boolean tryAdvance(Consumer<? super Object> consumer) {
        try {
          for(;;) {
            var token = parser.nextToken();
            Object result;
            switch(token) {
              case START_OBJECT -> {
                var objectVisitor = visitor.visitObject();
                if (objectVisitor == null) {
                  skipUntil(parser, END_OBJECT, stack);
                  continue;
                }
                result = readObject(parser, objectVisitor, stack);
              }
              case START_ARRAY -> {
                var arrayVisitor = visitor.visitArray();
                if (arrayVisitor == null) {
                  skipUntil(parser, END_ARRAY, stack);
                  continue;
                }
                result = readArray(parser, arrayVisitor, stack);
              }
              case VALUE_STRING -> result = visitor.visitValue(JsonValue.from(parser.getValueAsString()));
              case VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT -> result = readNumericValue(parser, visitor);
              case VALUE_TRUE -> result = visitor.visitValue(JsonValue.trueValue());
              case VALUE_FALSE -> result = visitor.visitValue(JsonValue.falseValue());
              case VALUE_NULL -> result = visitor.visitValue(JsonValue.nullValue());
              case END_ARRAY -> { ended = true; return false; }
              default -> throw new UncheckedIOException(new IOException("invalid token " + token));
            }
            consumer.accept(result);
            return true;
          }
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }

      @Override
      public Spliterator<Object> trySplit() {
        return null;
      }
      @Override
      public long estimateSize() {
        return Long.MAX_VALUE;
      }
      @Override
      public int characteristics() {
        return ORDERED;
      }
    };
    var stream = StreamSupport.stream(spliterator, false);
    Object result;
    try {
      result = visitor.visitStream(stream);
    } catch(UncheckedIOException e) {
      throw e.getCause();
    }
    if (!spliterator.ended) {  // stream short-circuited !
      skipUntil(parser, END_ARRAY, stack);
    }
    return visitor.visitEndArray(result);
  }

  private static Object readObject(JsonParser parser, ObjectVisitor visitor, ArrayDeque<JsonToken> stack) throws IOException {
    for(;;) {
      var fieldToken = parser.nextToken();
      if (fieldToken == END_OBJECT) {
        return visitor.visitEndObject();
      }
      if (fieldToken != FIELD_NAME) {
        throw new IOException("invalid token " + fieldToken);
      }
      var name = parser.getCurrentName();
      var token = parser.nextToken();
      switch(token) {
        case START_OBJECT -> parseOrSkipObject(parser, visitor.visitMemberObject(name), stack);
        case START_ARRAY -> parseOrSkipArray(parser, visitor.visitMemberArray(name), stack);
        case VALUE_STRING -> visitor.visitMemberValue(name, JsonValue.from(parser.getValueAsString()));
        case VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT -> readMemberNumericValue(parser, visitor, name);
        case VALUE_TRUE -> visitor.visitMemberValue(name, JsonValue.trueValue());
        case VALUE_FALSE -> visitor.visitMemberValue(name, JsonValue.falseValue());
        case VALUE_NULL -> visitor.visitMemberValue(name, JsonValue.nullValue());
        default -> throw new IOException("invalid token " + token);
      }
    }
  }

  private static void readMemberNumericValue(JsonParser parser, ObjectVisitor visitor, String name) throws IOException {
    switch(parser.getNumberType()) {
      case INT -> visitor.visitMemberValue(name, JsonValue.from(parser.getValueAsInt()));
      case LONG -> visitor.visitMemberValue(name, JsonValue.from(parser.getValueAsLong()));
      case FLOAT, DOUBLE -> visitor.visitMemberValue(name, JsonValue.from(parser.getValueAsDouble()));
      case BIG_INTEGER -> visitor.visitMemberValue(name, JsonValue.from(new BigInteger(parser.getValueAsString())));
      case BIG_DECIMAL -> visitor.visitMemberValue(name, JsonValue.from(new BigDecimal(parser.getValueAsString())));
      default -> throw new IOException("invalid number " + parser.getValueAsString());
    }
  }
}