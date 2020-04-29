package fr.umlv.jsonapi;

import fr.umlv.jsonapi.filter.FilterObjectVisitor;
import fr.umlv.jsonapi.filter.RenamerObjectVisitor;
import java.io.Reader;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Used when a JSON object is visited.
 *
 * <p>For each member of the object,
 *
 * <ul>
 *   <li>if the member is an object, {@link #visitMemberObject(String)} is called
 *   <li>if the member is an array, {@link #visitMemberArray(String)} is called
 *   <li>if the member is a value {@link #visitMemberValue(String, JsonValue)} is called
 * </ul>
 * when all the elements of the object have been visited, the method {@link #visitEndObject()} is
 * called.
 *
 * <p>Both {@link #visitMemberObject(String)} and {@link #visitMemberArray(String)} may return
 * null to indicate that the visitor is not interested by a peculiar object/array, in that case
 * the {@link JsonReader} will skip all the values until the end of the object/array is seen.
 *
 * <p>This visitor can work in two different {@link VisitorMode mode},
 * the semantics of the two methods {@link #visitMemberValue(String, JsonValue)} and
 * {@link #visitEndObject()} is different.
 *
 * <ul>
 *   <li>in {@link VisitorMode#PUSH}, the data flows from the {@link JsonReader reader} to
 *       the visitor and to an eventual {@link JsonWriter writer}. The return value of
 *       {@link #visitMemberValue(String, JsonValue)} is not used. The return value of
 *       {@link #visitEndObject()}
 *       is returned by the the method {@link JsonReader#parse(Reader, Object) parse} of the reader.
 *   <li>in {@link VisitorMode#PULL}, the data are pulled from the reader, by example using
 *       the method {@link JsonReader#stream(java.io.Reader, ArrayVisitor)}, so the return value
 *       of {@link #visitMemberValue(String, JsonValue)} is sent to the stream.
 *       The return value of the method {@link #visitEndObject()} is ignored.
 * </ul>
 *
 * <p>Example using the {@link VisitorMode#PUSH}
 * <pre>
 * String text = """
 *   {
 *     "name": "Mr Robot",
 *     "children": [ "Elliot", "Darlene" ]
 *   }
 *   """;
 * ObjectVisitor visitor = new ObjectVisitor() {
 *   public VisitorMode mode() {
 *     return VisitorMode.PUSH_MODE;
 *   }
 *   public ObjectVisitor visitMemberObject(String name) {
 *     return null;  // skip it
 *   }
 *   public ArrayVisitor visitMemberArray(String name) {
 *     assertEquals("children", name);
 *     return null;  // skip it
 *   }
 *   public void visitMemberValue(String name, JsonValue value) {
 *     assertEquals("Mr Robot", value.stringValue());
 *   }
 *   public Object visitEndObject() {
 *     return "end !";  // send result
 *   }
 * };
 * Object result = JsonReader.parse(text, visitor);
 * assertEquals(result, "end !");
 * </pre>
 *
 * <p>Example using the {@link VisitorMode#PULL}
 * <pre>
 * var text = """
 *   {
 *     "name": "Mr Robot",
 *     "children": [ "Elliot", "Darlene" ]
 *   }
 *   """;
 * BuilderConfig builderConfig =  BuilderConfig.defaults();
 * ObjectVisitor visitor = builderConfig.newObjectBuilder(new ObjectVisitor() {
 *   public VisitorMode mode() {
 *     return VisitorMode.PULL_MODE;
 *   }
 *   public ObjectVisitor visitMemberObject(String name) {
 *     return null;  // skip it
 *   }
 *   public ArrayVisitor visitMemberArray(String name) {
 *     assertEquals("children", name);
 *     return null;  // skip it
 *   }
 *   public Object visitMemberValue(String name, JsonValue value) {
 *     assertEquals("Mr Robot", value.stringValue());
 *     return "Mrs Robot";
 *   }
 *   public Object visitEndObject() {
 *     return null;  // result ignored
 *   }
 * });
 * Object map = JsonReader.parse(text, visitor);
 * assertEquals(Map.of("name", "Mrs Robot"), map);
 * </pre>
 *
 * <p>This visitor has a builder semantics, if it want to remember the values seen, they have to be
 * stored in fields of the class implementing the visitor.
 *
 * @see ArrayVisitor
 * @see ObjectBuilder
 * @see JsonReader#parse(String, ObjectVisitor, ArrayVisitor)
 */
public interface ObjectVisitor {
  /**
   * Returns the visitor mode of this visitor.
   * @return the visitor mode of this visitor.
   */
  VisitorMode mode();

  /**
   * Called when the {@link JsonReader reader} see the start of an object, the current visitor
   * can choose to either provide a new object visitor to visit the object
   * or return {@code null} to skip the parsing of that object.
   *
   * @return an object visitor or null.
   */
  ObjectVisitor visitMemberObject(String name);

  /**
   * Called when the {@link JsonReader reader} see the start of an array, the current visitor
   * can choose to either provide a new {@link ArrayVisitor array visitor} to visit the array
   * or return {@code null} to skip the parsing of that array.
   *
   * @return an array visitor or null.
   */
  ArrayVisitor visitMemberArray(String name);

  /**
   * Called when the {@link JsonReader reader} see an object element (a pair string/type),
   * with the type being either null, boolean, int, long, double, String, BigInteger
   * or BigDecimal).
   *
   * <p>In {@link VisitorMode#PUSH}, the return value of this method is ignored.
   * <p>In VisitMode#PULL_MODE, the return value of this method is inserted in the stream.
   *
   * @param name the name of the element
   * @param value the value of the element
   * @return null in push mode and the value that will be seen in the stream in pull mode.
   * @see JsonValue
   */
  Object visitMemberValue(String name, JsonValue value);

  /**
   * Called when all the elements of the object have been parsed.
   *
   * <ul>
   *   <li>In {@link VisitorMode#PUSH}, the return value of is propagated as return value
   *   of {@link JsonReader#parse(java.io.Reader, Object)}.
   *   <li>In {@link VisitorMode#PULL}, the return value of this method is ignored.
   * </ul>
   *
   * @return the result of the parsing in push mode or null in pull mode.
   */
  Object visitEndObject();

  default ObjectVisitor mapName(UnaryOperator<String> renamer) {
    return new RenamerObjectVisitor(this, renamer);
  }

  default ObjectVisitor filterName(Predicate<? super String> predicate) {
    return new FilterObjectVisitor(this, predicate);
  }
}