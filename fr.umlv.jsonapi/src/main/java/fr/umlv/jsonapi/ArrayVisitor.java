package fr.umlv.jsonapi;

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
 * <p>This interface can be used in
 *
 * <ul>
 *   <li>push mode: the {@link JsonReader} push all the values seen
 *   <li>pull mode: the {@link JsonReader} create a {@link JsonReader#stream(java.io.Reader,
 *       ArrayVisitor) stream} that will pull the values
 * </ul>
 *
 * See the method {@link #visitValue(JsonValue)} for more information.
 *
 * <p>Example in {@code push mode}, using {@link JsonReader#parse(java.io.Reader, Object)}
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
 *   public Object visitEndArray(Stream&lt;Object&gt; unused) {
 *     return "end !";  // send result;
 *   }
 * };
 * Object result = JsonReader.parse(text, visitor);
 * assertEquals(result, "end !");
 * </pre>
 *
 * <p>Example in {@code pull mode}, using {@link JsonReader#stream(java.io.Reader, ArrayVisitor)}
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
 *     return null;  // return value ignored;
 *   }
 * };
 * try(Stream&lt;Object&gt; stream = JsonReader.stream(text, visitor)) {
 *   assertEquals("Joleene", stream.skip(1).findFirst().orElseThrow());
 * }
 * </pre>
 *
 * Both {@link #visitObject()} and {@link #visitArray()} may return null to indicate that the
 * visitor is not interested to a peculiar object/array, in that case the {@link JsonReader} will
 * skip all the values until the end of the object/array is seen.
 *
 * <p>This visitor has a builder semantics, if it want to remember the values seen, they have to be
 * stored in fields of the class implementing the visitor.
 *
 * @see ObjectVisitor
 * @see StreamVisitor
 * @see ArrayBuilder
 * @see JsonReader#parse(java.io.Reader, Object)
 * @see JsonReader#stream(java.io.Reader, ArrayVisitor)
 */
public interface ArrayVisitor {
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
   * Called when the {@link JsonReader reader} see a value (a null, a boolean, an int, a long,
   * a double, a String, a BigInteger or a BigDecimal).
   *
   * <p>In push mode, using {@link JsonReader#parse(java.io.Reader, Object)},
   * the return value of this method is ignored.
   * <p>In pull mode, using {@link JsonReader#stream(java.io.Reader, ArrayVisitor)},
   * the return value of this method is inserted in the stream.
   *
   * @return null is push mode and the value that will be seen in the stream in pull mode.
   * @see JsonValue
   */
  Object visitValue(JsonValue value);

  /**
   * Called when all the values of the array have been parsed.
   *
   * <p>In push mode, using {@link JsonReader#parse(java.io.Reader, Object)},
   * the return value of this method is propagated as return value of
   * {@link JsonReader#parse(java.io.Reader, Object)}.
   * <p>In pull mode, using {@link JsonReader#stream(java.io.Reader, ArrayVisitor)},
   * the return value of this method is ignored.
   *
   * @return the result of the parsing in push mode or null in pull mode.
   */
  Object visitEndArray();
}