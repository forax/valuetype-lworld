package fr.umlv.jsonapi;

import java.io.Reader;
import java.util.stream.Stream;

/**
 * Used when a JSON array is visited. This class is a subclass of {@link ArrayVisitor} that exposes
 * an array as a Stream. Unlike using {@link JsonReader#stream(Reader, ArrayVisitor)}, this visitor
 * can be used inside any nested data structure and not only at top level. The processing is done
 * lazily so if not all the values are necessary to finish the computation of the stream the data
 * not used will be skipped by the reader.
 *
 * <p>At the beginning the method {@link #visitStream(Stream)} is called to transfer the control to
 * the stream, then each time a value of the stream is need, one of these 3 methods is called
 *
 * <ul>
 *   <li>if the value is an object, {@link #visitObject()} is called, return a visitor and the
 *       return of {@link ObjectVisitor#visitEndObject()} called on that visitor is inserted in the
 *       stream.
 *   <li>if the value is an array, {@link #visitArray()} is called, return a visitor and the return
 *       of {@link ArrayVisitor#visitEndArray()} called on that visitor is inserted in the stream.
 *   <li>if the value is a value {@link #visitValue(JsonValue)} is called
 * </ul>
 * when all the values of the array have been visited, the method {@link #visitEndArray()} is
 * called.
 *
 * <p>Example, using {@link JsonReader#parse(java.io.Reader, Object)}
 *
 * <pre>
 * var text = """
 *   [ "Jolene", "Joleene", "Joleeeene" ]
 *   """;
 * var StreamVisitor = new StreamVisitor() {
 *   public Object visitStream(Stream<=&lt;Object&gt; stream) {
 *     return stream.skip(1).findFirst().orElseThrow();
 *   }
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
 * };
 * Object result = JsonReader.parse(text, visitor);
 * assertEquals("Joleene", result);
 * </pre>
 *
 * <p>This visitor has an immutable semantics, so no mutable field should be used.
 *
 * @see ArrayVisitor
 * @see ObjectVisitor
 * @see JsonReader#parse(java.io.Reader, Object)
 */
public interface StreamVisitor extends ArrayVisitor {
  @Override
  default VisitorMode mode() { return VisitorMode.PULL_MODE; }

  @Override
  default Void visitEndArray() {
    return null;
  }

  /**
   * This method is called first with a Stream, consuming the stream will called the methods
   * {@link #visitObject()}, {@link #visitArray()} and {@link #visitValue(JsonValue)}
   * (in pull mode) depending on the kind of values in the array and the returned value
   * will be inserted in the stream.
   *
   * This stream
   *
   * @param stream
   * @return
   */
  Object visitStream(Stream<Object> stream);

  @Override
  Object visitValue(JsonValue value);
}
