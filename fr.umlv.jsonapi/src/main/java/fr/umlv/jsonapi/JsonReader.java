package fr.umlv.jsonapi;

import static com.fasterxml.jackson.core.JsonToken.END_ARRAY;
import static com.fasterxml.jackson.core.JsonToken.END_OBJECT;
import static com.fasterxml.jackson.core.JsonToken.FIELD_NAME;
import static com.fasterxml.jackson.core.JsonToken.START_ARRAY;
import static com.fasterxml.jackson.core.JsonToken.START_OBJECT;
import static fr.umlv.jsonapi.VisitorMode.PULL;
import static fr.umlv.jsonapi.VisitorMode.PULL_INSIDE;
import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import fr.umlv.jsonapi.builder.ArrayBuilder;
import fr.umlv.jsonapi.builder.ObjectBuilder;
import fr.umlv.jsonapi.internal.RootVisitor;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A class that provides static methods to read the content of a JSON file/text
 * into method calls to two visitors {@link ObjectVisitor} if the content is a
 * JSON object or {@link ArrayVisitor} is the content is a JSON array.
 *
 * This API provides two different ways of reading a JSON content
 * <ul>
 *   <li>in {@link VisitorMode#PUSH push mode}, the JSON values are {@link #parse(Reader, Object)}
 *       by the reader and the visit methods are called, the visitor stores the information
 *       in its fields and at the end the method {@link ObjectVisitor#visitEndObject()}
 *       or {@link ArrayVisitor#visitEndArray()} return the value processed by the visitor.
 *   <li>in {@link VisitorMode#PULL pull mode}, the method {@link #stream(Reader, ArrayVisitor)}
 *       of the reader return a stream that consume the JSON values by pulling them from
 *       the visitors by calling the methods visit and using the values returned by these
 *       methods. The return value of {@link ObjectVisitor#visitEndObject()} or
 *       {@link ArrayVisitor#visitEndArray()} is ignored.
 * </ul>
 *
 * Moreover, for the method {@link ObjectVisitor#visitMemberObject(String)},
 * {@link ObjectVisitor#visitMemberArray(String)}, {@link ArrayVisitor#visitObject()} and
 * {@link ArrayVisitor#visitArray()}, if a visitor return {@code null}, the reader
 * will skip all the tokens until the end of the corresponding object/array.
 */
public final class JsonReader {
  private JsonReader() {
    throw new AssertionError();
  }

  /**
   * Parse the content of a file in JSON format and calls the visit methods of the visitor.
   * If the content is a JSON object, the visitor should be an {@link ObjectVisitor},
   * if the content is a JSON array, the visitor should be an {@link ArrayVisitor}.
   * If you don't know, use {0link {@link #parse(Path, ObjectVisitor, ArrayVisitor)}} instead.
   *
   * @param path the path to the file
   * @param visitor either an {@link ObjectVisitor} or an {@link ArrayVisitor}
   * @return the return value of {@link ObjectVisitor#visitEndObject()} or
   *         {@link ArrayVisitor#visitEndArray()}.
   * @throws IOException if an IO errors occurs
   * @throws IllegalStateException if the JSON contains an object but the visitor is
   *         an {@link ArrayVisitor}, if the JSON contains an array and the visitor
   *         is an {@link ObjectVisitor}, if one returned visitors is in
   *         {@link VisitorMode#PULL} mode
   *
   * @see #parse(Reader, ObjectVisitor, ArrayVisitor)
   */
  public static Object parse(Path path, Object visitor) throws IOException {
    requireNonNull(path);
    requireNonNull(visitor);
    return parseJson(path, RootVisitor.createFromOneVisitor(visitor));
  }

  /**
   * Parse the content of a file in JSON format and returns the result
   * as {@link Map&lt;String, Object&gt;}.
   *
   * @param path the path to the file
   * @param builder an object builder
   * @return a map containing the content of the file
   * @throws IOException if an IO errors occurs
   * @throws IllegalStateException if the JSON contains an array
   *
   * @see fr.umlv.jsonapi.builder.BuilderConfig
   * @see #parse(Path, Object)
   */
  @SuppressWarnings("unchecked")
  public static Map<String, Object> parse(Path path, ObjectBuilder builder) throws IOException {
    requireNonNull(path);
    requireNonNull(builder);
    return (Map<String, Object>) parse(path, (Object) builder);
  }

  /**
   * Parse the content of a file in JSON format and returns the result
   * as {@link List&lt;Object&gt;}.
   *
   * @param path the path to the file
   * @param builder an array builder
   * @return a list containing the content of the file
   * @throws IOException if an IO errors occurs
   * @throws IllegalStateException if the JSON contains an array
   *
   * @see fr.umlv.jsonapi.builder.BuilderConfig
   * @see #parse(Path, Object)
   */
  @SuppressWarnings("unchecked")
  public static List<Object> parse(Path path, ArrayBuilder builder) throws IOException {
    requireNonNull(path);
    requireNonNull(builder);
    return (List<Object>) parse(path, (Object) builder);
  }

  /**
   * Parse the content of a file in JSON format and calls the visit methods
   * of one of the visitors.
   * If the content is a JSON object, the {@code objectVisitor} will be called
   * if the content is a JSON array, the {@code arrayVisitor} will be called
   *
   * @param path path to the file
   * @param objectVisitor the visitor called if the content is an object
   * @param arrayVisitor the visitor called is the content is an array
   * @return the return value of {@link ObjectVisitor#visitEndObject()} or
   *         {@link ArrayVisitor#visitEndArray()}.
   * @throws IOException if an IO errors occurs
   * @throws IllegalStateException if one returned visitors is in
   *         {@link VisitorMode#PULL} mode
   */
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

  /**
   * Parse the content of a text in JSON format and calls the visit methods of the visitor.
   * If the text is a JSON object, the visitor should be an {@link ObjectVisitor},
   * if the text is a JSON array, the visitor should be an {@link ArrayVisitor}.
   * If you don't know, use {0link {@link #parse(String, ObjectVisitor, ArrayVisitor)}} instead.
   *
   * @param text the content in JSON format
   * @param visitor either an {@link ObjectVisitor} or an {@link ArrayVisitor}
   * @return the return value of {@link ObjectVisitor#visitEndObject()} or
   *         {@link ArrayVisitor#visitEndArray()}.
   * @throws IllegalStateException if the JSON contains an object but the visitor is
   *         an {@link ArrayVisitor}, if the JSON contains an array and the visitor
   *         is an {@link ObjectVisitor}, if one returned visitors is in
   *         {@link VisitorMode#PULL} mode
   *
   * @see #parse(Reader, ObjectVisitor, ArrayVisitor)
   */
  public static Object parse(String text, Object visitor) {
    requireNonNull(text);
    requireNonNull(visitor);
    return parseJson(text, RootVisitor.createFromOneVisitor(visitor));
  }

  /**
   * Parse the content of a text in JSON format and returns the result
   * as {@link Map&lt;String, Object&gt;}.
   *
   * @param text a text in JSON format
   * @param builder an object builder
   * @return a map containing the content of the text
   * @throws IllegalStateException if the JSON contains an array
   *
   * @see fr.umlv.jsonapi.builder.BuilderConfig
   * @see #parse(String, Object)
   */
  @SuppressWarnings("unchecked")
  public static Map<String, Object> parse(String text, ObjectBuilder builder) {
    requireNonNull(text);
    requireNonNull(builder);
    return (Map<String, Object>) parse(text, (Object) builder);
  }

  /**
   * Parse the content of a text in JSON format and returns the result
   * as {@link List&lt;Object&gt;}.
   *
   * @param text the content in JSON format
   * @param builder an array builder
   * @return a list containing the content of the text
   * @throws IllegalStateException if the JSON contains an array
   *
   * @see fr.umlv.jsonapi.builder.BuilderConfig
   * @see #parse(Reader, Object)
   */
  @SuppressWarnings("unchecked")
  public static List<Object> parse(String text, ArrayBuilder builder) {
    requireNonNull(text);
    requireNonNull(builder);
    return (List<Object>) parse(text, (Object) builder);
  }

  /**
   * Parse the content of a reader in JSON format and calls the visit methods
   * of one of the visitors.
   * If the content is a JSON object, the {@code objectVisitor} will be called
   * if the content is a JSON array, the {@code arrayVisitor} will be called
   *
   * @param text the text to parse in JSON format
   * @param objectVisitor the visitor called if the content is an object
   * @param arrayVisitor the visitor called is the content is an array
   * @return the return value of {@link ObjectVisitor#visitEndObject()} or
   *         {@link ArrayVisitor#visitEndArray()}.
   * @throws IllegalStateException if one returned visitors is in
   *         {@link VisitorMode#PULL} mode
   */
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

  /**
   * Parse the content of a reader in JSON format and calls the visit methods of the visitor.
   * If the content is a JSON object, the visitor should be an {@link ObjectVisitor},
   * if the content is a JSON array, the visitor should be an {@link ArrayVisitor}.
   * If you don't know, use {0link {@link #parse(Reader, ObjectVisitor, ArrayVisitor)}} instead.
   *
   * @param reader the content in JSON format
   * @param visitor either an {@link ObjectVisitor} or an {@link ArrayVisitor}
   * @return the return value of {@link ObjectVisitor#visitEndObject()} or
   *         {@link ArrayVisitor#visitEndArray()}.
   * @throws IOException if an IO errors occurs
   * @throws IllegalStateException if the JSON contains an object but the visitor is
   *         an {@link ArrayVisitor}, if the JSON contains an array and the visitor
   *         is an {@link ObjectVisitor}, if one returned visitors is in
   *         {@link VisitorMode#PULL} mode
   *
   * @see #parse(Reader, ObjectVisitor, ArrayVisitor)
   */
  public static Object parse(Reader reader, Object visitor) throws IOException {
    requireNonNull(reader);
    requireNonNull(visitor);
    return parseJson(reader, RootVisitor.createFromOneVisitor(visitor));
  }

  /**
   * Parse the content of a reader in JSON format and returns the result
   * as {@link Map&lt;String, Object&gt;}.
   *
   * @param reader the content in JSON format
   * @param builder an object builder
   * @return a map containing the content of the {@code reader}
   * @throws IOException if an IO errors occurs
   * @throws IllegalStateException if the JSON contains an array
   *
   * @see fr.umlv.jsonapi.builder.BuilderConfig
   * @see #parse(Reader, Object)
   */
  @SuppressWarnings("unchecked")
  public static Map<String, Object> parse(Reader reader, ObjectBuilder builder) throws IOException {
    requireNonNull(reader);
    requireNonNull(builder);
    return (Map<String, Object>) parse(reader, (Object) builder);
  }

  /**
   * Parse the content of a reader in JSON format and returns the result
   * as {@link List&lt;Object&gt;}.
   *
   * @param reader the content in JSON format
   * @param builder an array builder
   * @return a list containing the content of the {@code reader}
   * @throws IOException if an IO errors occurs
   * @throws IllegalStateException if the JSON contains an array
   *
   * @see fr.umlv.jsonapi.builder.BuilderConfig
   * @see #parse(Reader, Object)
   */
  @SuppressWarnings("unchecked")
  public static List<Object> parse(Reader reader, ArrayBuilder builder) throws IOException {
    requireNonNull(reader);
    requireNonNull(builder);
    return (List<Object>) parse(reader, (Object) builder);
  }

  /**
   * Parse the content of a reader in JSON format and calls the visit methods
   * of one of the visitors.
   * If the content is a JSON object, the {@code objectVisitor} will be called
   * if the content is a JSON array, the {@code arrayVisitor} will be called
   *
   * @param reader the content in JSON format
   * @param objectVisitor the visitor called if the content is an object
   * @param arrayVisitor the visitor called is the content is an array
   * @return the return value of {@link ObjectVisitor#visitEndObject()} or
   *         {@link ArrayVisitor#visitEndArray()}.
   * @throws IOException if an IO errors occurs
   * @throws IllegalStateException if one returned visitors is in
   *         {@link VisitorMode#PULL} mode
   */
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
    var mode = visitor.visitStartArray();
    if (mode == PULL_INSIDE) {
      return readStreamArray(parser, visitor, stack);
    }
    if (mode == PULL) {
      throw new IllegalArgumentException("ArrayVisitor pull mode not allowed");
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
        case END_ARRAY -> { return visitor.visitEndArray(); }
        default -> throw new IOException("invalid token " + token);
      }
    }
  }

  private static Object readNumericValue(JsonParser parser, ArrayVisitor visitor) throws IOException {
    return switch(parser.getNumberType()) {
      case INT -> visitor.visitValue(JsonValue.from(parser.getValueAsInt()));
      case LONG -> visitor.visitValue(JsonValue.from(parser.getValueAsLong()));
      case FLOAT, DOUBLE, BIG_DECIMAL -> visitor.visitValue(JsonValue.from(parser.getValueAsDouble()));
      case BIG_INTEGER -> visitor.visitValue(JsonValue.from(new BigInteger(parser.getValueAsString())));
    };
  }

  private static Object readStreamArray(JsonParser parser, ArrayVisitor visitor, ArrayDeque<JsonToken> stack) throws IOException {
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
    visitor.visitEndArray();
    return result;
  }

  private static Object readObject(JsonParser parser, ObjectVisitor visitor, ArrayDeque<JsonToken> stack) throws IOException {
    if (visitor.visitStartObject() == PULL) {
      throw new IllegalArgumentException("ObjectVisitor pull mode not allowed");
    }
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
      case FLOAT, DOUBLE, BIG_DECIMAL -> visitor.visitMemberValue(name, JsonValue.from(parser.getValueAsDouble()));
      case BIG_INTEGER -> visitor.visitMemberValue(name, JsonValue.from(new BigInteger(parser.getValueAsString())));
      default -> throw new IOException("invalid number " + parser.getValueAsString());
    }
  }


  /**
   * Returns a Stream that will trigger the reading of the content of the file
   * each time the Stream need an object.
   * In details, when an item of the Stream is needed, the reader will read
   * the necessary tokens on the reader, call the {@link ArrayVisitor} and
   * use the return value to send the value to the Stream.
   *
   * @param path the for to the file
   * @param arrayVisitor an {@link ArrayVisitor} in {@link VisitorMode#PULL pull mode}
   * @return a stream that can be used to pull the JSON value from the file
   * @throws IOException if an IO errors occurs. Note that if an IO exception occurs when the
   *         Stream is already returned, an {@link UncheckedIOException} will be thrown
   *         by the {@link Stream} methods.
   * @throws IllegalStateException if the JSON contains an object, if the visitor is not in
   *         {@link VisitorMode#PULL pull mode} or one of the subsequent returned visitors is in
   *         {@link VisitorMode#PULL pull mode}.
   */
  public static Stream<Object> stream(Path path, ArrayVisitor arrayVisitor) throws IOException {
    requireNonNull(path);
    requireNonNull(arrayVisitor);
    var reader = Files.newBufferedReader(path);
    try {
      return stream(reader, arrayVisitor);
    } catch(RuntimeException | Error | IOException e) { // don't leak the reader
      reader.close();
      throw e;
    }
  }

  /**
   * Returns a Stream that will trigger the reading of the content of the text
   * each time the Stream need an object.
   * In details, when an item of the Stream is needed, this JSON reader will read
   * the necessary tokens of the text, calls the {@link ArrayVisitor} and
   * use the return value to send the value to the Stream.
   *
   * @param text the text in JSON format
   * @param arrayVisitor an {@link ArrayVisitor} in {@link VisitorMode#PULL pull mode}
   * @return a stream that can be used to pull the JSON value from the text
   * @throws IllegalStateException if the JSON contains an object, if the visitor is not in
   *         {@link VisitorMode#PULL pull mode} or one of the subsequent returned visitors is in
   *         {@link VisitorMode#PULL pull mode}.
   */
  public static Stream<Object> stream(String text, ArrayVisitor arrayVisitor) {
    requireNonNull(text);
    requireNonNull(arrayVisitor);
    try {
      return stream(new StringReader(text), arrayVisitor);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Returns a Stream that will trigger the reading of the content of the reader
   * each time the Stream need an object.
   * In details, when an item of the Stream is needed, the reader will read
   * the necessary tokens on the reader, call the {@link ArrayVisitor} and
   * use the return value to send the value to the Stream.
   *
   * @param reader the content in JSON format
   * @param visitor an {@link ArrayVisitor} in {@link VisitorMode#PULL pull mode}
   * @return a stream that can be used to pull the JSON value from the reader
   * @throws IOException if an IO errors occurs. Note that if an IO exception occurs when the
   *         Stream is already returned, an {@link UncheckedIOException} will be thrown
   *         by the {@link Stream} methods.
   * @throws IllegalStateException if the JSON contains an object, if the visitor is not in
   *         {@link VisitorMode#PULL pull mode} or one of the subsequent returned visitors is in
   *         {@link VisitorMode#PULL pull mode}.
   */
  public static Stream<Object> stream(Reader reader, ArrayVisitor visitor) throws IOException {
    requireNonNull(reader);
    requireNonNull(visitor);
    if (visitor.visitStartArray() != PULL) {
      throw new IllegalArgumentException("only pull mode visitors are allowed");
    }
    var parser = new JsonFactory().createParser(reader);
    try {
      var stack = new ArrayDeque<JsonToken>();
      var token = parser.nextToken();
      if (token == START_OBJECT) {
        throw new IllegalStateException("root is an object but expect an array");
      }
      if (token != START_ARRAY) {
        throw new IllegalStateException("invalid token " + token);
      }
      var spliterator = new Spliterator<>() {
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
                case END_ARRAY -> { visitor.visitEndArray(); return false; }
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
      return stream.onClose(() -> {
        try {
          parser.close();
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      });
    } catch(RuntimeException | Error | IOException e) {  // don't leak the parser
      parser.close();
      throw e;
    }
  }
}