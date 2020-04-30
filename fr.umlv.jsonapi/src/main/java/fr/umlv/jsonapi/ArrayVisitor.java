package fr.umlv.jsonapi;

import java.io.Reader;
import java.util.stream.Stream;

import fr.umlv.jsonapi.builder.ArrayBuilder;

/**
 * Used when a JSON array is visited.
 *
 * <p>For each value of the array,
 *
 * <ul>
 *   <li>if the value is an object, {@link #visitObject()} is called
 *   <li>if the value is an array, {@link #visitArray()} is called
 *   <li>if the value is a value {@link #visitValue(JsonValue)} is called
 * </ul>
 *
 * when all the values of the array have been visited, the method {@link #visitEndArray()} is
 * called.
 *
 * <p>* Both {@link #visitObject()} and {@link #visitArray()} may return null to indicate that the *
 * visitor is not interested by a peculiar object/array, in that case the {@link JsonReader} will *
 * skip all the values until the end of the object/array is seen.
 *
 * <p>This visitor can work in three different {@link VisitorMode mode}s.
 *
 * <ul>
 *   <li>in {@link VisitorMode#PUSH}, the data flows from the {@link JsonReader reader} to the
 *       visitor and to an eventual {@link JsonWriter writer}. The return value of {@link
 *       #visitValue(JsonValue)} is not used. The return value of {@link #visitEndArray()} is
 *       returned by the the method {@link JsonReader#parse(Reader, Object) parse} of the reader.
 *   <li>in {@link VisitorMode#PULL}, the data are pulled from the reader, by example using the
 *       method {@link JsonReader#stream(java.io.Reader, ArrayVisitor)}, so the return value of
 *       {@link #visitValue(JsonValue)} is sent to the stream. The return value of the method {@link
 *       #visitEndArray()} is ignored.
 *   <li>in {@link VisitorMode#PULL_INSIDE}, instead of having the values pulled by the {@link
 *       JsonReader reader} from outside the visitor, the vales are pulled from inside the visitor
 *       by calling the method {@link #visitStream(Stream)} first. The semantics of {@link
 *       #visitValue(JsonValue)} and {@link #visitEndArray()} is the same as in pull mode.
 * </ul>
 *
 * <p>Example in {@link VisitorMode#PULL push mode}, using {@link JsonReader#parse(java.io.Reader,
 * Object)}
 *
 * <pre>
 * String text = """
 *   [ "Jolene", "Joleene", "Joleeeene" ]
 *   """;
 * ArrayVisitor visitor = new ArrayVisitor() {
 *   public ObjectVisitor visitObject() {
 *     return null;  // skip it
 *   }
 *   public ArrayVisitor visitArray() {
 *     return null;  // skip it
 *   }
 *   public Object visitValue(JsonValue value) {
 *     assertTrue(value.stringValue().startsWith("Jole"));
 *     return null;  // used in push mode
 *   }
 *   public Object visitEndArray() {
 *     return "end !";  // send result;
 *   }
 * };
 * Object result = JsonReader.parse(text, visitor);
 * assertEquals(result, "end !");
 * </pre>
 *
 * <p>Example in {@link VisitorMode#PULL pull mode}, using {@link JsonReader#stream(java.io.Reader,
 * ArrayVisitor)}
 *
 * <pre>
 * String text = """
 *   [ "Jolene", "Joleene", "Joleeeene" ]
 *   """;
 * ArrayVisitor visitor = new ArrayVisitor() {
 *   public ObjectVisitor visitObject() {
 *     return null;  // skip it
 *   }
 *   public ArrayVisitor visitArray() {
 *     return null;  // skip it
 *   }
 *   public Object visitValue(JsonValue value) {
 *     assertTrue(value.stringValue().startsWith("Jole"));
 *     return value.asObject();  // used in pull mode
 *   }
 *   public Object visitEndArray() {
 *     return null;  // return value ignored
 *   }
 * };
 * try(Stream&lt;Object&gt; stream = JsonReader.stream(text, visitor)) {
 *   assertEquals("Joleene", stream.skip(1).findFirst().orElseThrow());
 * }
 * </pre>
 *
 * <p>Example in {@link VisitorMode#PULL_INSIDE pull inside mode},
 * using {@link JsonReader#parse(java.io.Reader, Object)}
 *
 * <pre>
 * String text = """
 *   [ "Jolene", "Joleene", "Joleeeene" ]
 *   """;
 * ArrayVisitor visitor = new ArrayVisitor() {
 *   public VisitorMode mode() {
 *     return VisitorMode.PULL_INSIDE;
 *   }
 *   public Object visitStream(Stream&lt;Object&gt; stream) {
 *     return stream.skip(1).findFirst().orElseThrow();
 *   }
 *   public Object visitValue(JsonValue value) {
 *     assertTrue(value.stringValue().startsWith("Jole"));
 *     return value.asObject(); // used in pull mode
 *   }
 *   public ObjectVisitor visitObject() {
 *     return null; // skip it
 *   }
 *   public ArrayVisitor visitArray() {
 *     return null; // skip it
 *   }
 *   public Object visitEndArray() {
 *     return null; // return value ignored
 *   }
 * };
 * Object result = JsonReader.parse(text, visitor);
 * assertEquals("Joleene", result);
 * </pre>
 *
 * <p>This visitor has a builder semantics, if it want to remember the values seen, they have to be
 * stored in fields of the class implementing the visitor.
 *
 * @see ObjectVisitor
 * @see ArrayBuilder
 * @see JsonReader#parse(java.io.Reader, Object)
 * @see JsonReader#stream(java.io.Reader, ArrayVisitor)
 */
public interface ArrayVisitor {
  /**
   * Returns the visitor mode of this visitor.
   * The visitor mode is used by the {@link JsonReader} to know which methods of the API
   * to call and in which order.
   *
   * @return the visitor mode of this visitor.
   */
  VisitorMode mode();

  /**
   * In {@link VisitorMode#PULL_INSIDE pull inside mode}, this method is called before any other
   * method. When the value of the Stream will be consumed, the method {@link #visitObject()},
   * {@link #visitArray()} and {@link #visitValue(JsonValue)} will be called to transform
   * the JSON values.
   *
   * @param stream the stream that will be used to control the parsing, this stream should not
   *               escape the method.
   * @return the result of the parsing, this value will be propagated as return value of
   *         {@link JsonReader#parse(Reader, Object)}.
   */
  default Object visitStream(Stream<Object> stream) {
    return null;
  }

  /**
   * Called when the {@link JsonReader reader} see the start of an object, the current visitor
   * can choose to either provide a new {@link ObjectVisitor object visitor} to visit the object
   * or return {@code null} to skip the parsing of that object.
   *
   * @return an object visitor or null.
   */
  ObjectVisitor visitObject();

  /**
   * Called when the {@link JsonReader reader} see the start of an array, the current visitor
   * can choose to either provide a new array visitor to visit the array
   * or return {@code null} to skip the parsing of that array.
   *
   * @return an array visitor or null.
   */
  ArrayVisitor visitArray();

  /**
   * Called when the {@link JsonReader reader} see a value of type either null, boolean, int, long,
   * double, String, BigInteger or BigDecimal.
   * <ul>
   *   <li>In {@link VisitorMode#PUSH}, the return value of this method is ignored. *
   *   <li>In VisitMode#PULL_MODE, the return value of this method is inserted in the stream.
   * </ul>
   *
   * @param value an array value
   * @return null in push mode and the value that will be seen in the stream in pull mode.
   * @see JsonValue
   */
  Object visitValue(JsonValue value);

  /**
   * Called when all the values of the array have been parsed.
   *
   * <ul>
   *   <li>In {@link VisitorMode#PUSH}, the return value of is propagated as return value
   *   of {@link JsonReader#parse(java.io.Reader, Object)}.
   *   <li>In {@link VisitorMode#PULL}, the return value of this method is ignored.
   * </ul>
   *
   * @return the result of the parsing in push mode or null in pull mode.
   */
  Object visitEndArray();
}