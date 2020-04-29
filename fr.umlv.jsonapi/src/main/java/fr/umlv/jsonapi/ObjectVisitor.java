package fr.umlv.jsonapi;

import fr.umlv.jsonapi.filter.FilterObjectVisitor;
import fr.umlv.jsonapi.filter.RenamerObjectVisitor;
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
 * <p>Example
 * <pre>
 * String text = """
 *   {
 *     "name": "Mr Robot",
 *     "children": [ "Elliot", "Darlene" ]
 *   }
 *   """;
 * ObjectVisitor visitor = new ObjectVisitor() {
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
 *     return "end !";  // send result;
 *   }
 * };
 * Object result = JsonReader.parse(text, visitor);
 * assertEquals(result, "end !");
 * </pre>
 *
 * Both {@link #visitMemberObject(String)} and {@link #visitMemberArray(String)} may return null to
 * indicate that the visitor is not interested to a peculiar object/array, in that case the {@link
 * JsonReader} will skip all the values until the end of the object/array is seen.
 *
 * <p>This visitor has a builder semantics, if it want to remember the values seen, they have to be
 * stored in fields of the class implementing the visitor.
 *
 * @see ArrayVisitor
 * @see ObjectBuilder
 * @see JsonReader#parse(String, ObjectVisitor, ArrayVisitor)
 */
public interface ObjectVisitor {
  ObjectVisitor visitMemberObject(String name);
  ArrayVisitor visitMemberArray(String name);
  void visitMemberValue(String name, JsonValue value);
  Object visitEndObject();

  default ObjectVisitor mapName(UnaryOperator<String> renamer) {
    return new RenamerObjectVisitor(this, renamer);
  }

  default ObjectVisitor filterName(Predicate<? super String> predicate) {
    return new FilterObjectVisitor(this, predicate);
  }
}