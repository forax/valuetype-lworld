package fr.umlv.jsonapi.bind;

import static fr.umlv.jsonapi.bind.Binder.ARRAY;
import static fr.umlv.jsonapi.bind.Binder.OBJECT;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.function.Predicate.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.umlv.jsonapi.BuilderConfig;
import fr.umlv.jsonapi.JsonReader;
import fr.umlv.jsonapi.bind.Binder.SpecFinder;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class BinderTest {
  @Test
  public void noDefaults() {
    var binder = Binder.noDefaults();
    assertTrue(binder.findSpec(int.class).isPresent());
    assertTrue(binder.findSpec(String.class).isPresent());
    assertTrue(binder.findSpec(URI.class).isEmpty());
  }

  /*@Test
  public void register() {
    var binder = new Binder(lookup());
    binder.register(SpecFinder.from(Map.of(
        LocalDate.class, Spec.valueClass("LocalDate", v -> v))
    ));
    record Data(LocalDate date) { }
    var json = """
        [ { "date": "1234" } ]
        """;
    List<Data> list = binder.read(json, Data.class, ARRAY);
    assertEquals(List.of(new Data(LocalDate.now())), list);
  }*/

  @Test
  public void findSpec() {
    var binder = new Binder(lookup());
    record Point(int x, int y) { }
    var spec = binder.findSpec(Point.class).orElseThrow();
    assertEquals("Point", spec.toString());
  }

  @Test
  public void read() {
    var binder = new Binder(lookup());
    var json = """
        { "x": 42, "y": 33 }
        """;
    record Point(int x, int y) { }
    Point point = binder.read(json, Point.class);
    assertEquals(new Point(42, 33), point);
  }

  @Test
  public void readRecordOfRecord() {
    var binder = new Binder(lookup());
    record Point(int x, int y) { }
    record Circle(Point center, double surface) { }
    var json = """
        { "center": { "x": 7, "y": -4 }, "surface": 34.2 }
        """;
    Circle circle = binder.read(json, Circle.class);
    assertEquals(new Circle(new Point(7, -4), 34.2), circle);
  }

  @Test
  public void readObject() {
    var binder = Binder.noDefaults();
    var json = """
        { "firstName": "John", "lastName": "Boijoly" }
        """;
    Map<String, String> map = binder.read(json, String.class, OBJECT);
    assertEquals(Map.of("firstName", "John", "lastName", "Boijoly"), map);
  }

  @Test
  public void readArrayInt() {
    var binder = Binder.noDefaults();
    var json = """
        [ 1, 2, 3, 4, 10 ]
        """;
    List<Integer> array = binder.read(json, int.class, ARRAY);
    assertEquals(List.of(1, 2, 3, 4, 10), array);
  }

  @Test
  public void readArrayOfRecord() {
    var binder = new Binder(lookup());
    var json = """
        [ { "color": "red", "lines": 3 }, { "color": "blue", "lines": 1 } ]
        """;
    record Shape(String color, int lines) { }
    List<Shape> array = binder.read(json, Shape.class, ARRAY);
    assertEquals(List.of(new Shape("red", 3), new Shape("blue", 1)), array);
  }

  @Test
  public void readSpec() {
    var binder = new Binder(lookup());
    var json = """
        [ { "color": "red", "lines": 3 }, { "color": "blue", "lines": 1 } ]
        """;
    var any = binder.findSpec(Object.class).orElseThrow();
    Object o = Binder.read(json, any.object().array(), new BuilderConfig());
    assertEquals(
        List.of(
            Map.of("color", "red", "lines", 3),
            Map.of("color", "blue", "lines", 1)),
        o);
  }

  @Test
  public void readSecurity() {
    record Authorized() { }
    record Unauthorized() { }
    var binder = Binder.noDefaults();  // no finder registered !
    var recordFinder = SpecFinder.recordFinder(lookup(), binder);
    var restrictedSet = Set.of(Authorized.class);
    // register the finder filtered !
    binder.register(recordFinder.filter(restrictedSet::contains));

    var json = "{}";
    var authorized = binder.read(json, Authorized.class);
    assertEquals(new Authorized(), authorized);

    assertThrows(IllegalStateException.class, () -> binder.read(json, Unauthorized.class));
  }

  @Test
  public void readAndFilter() {
    var binder = new Binder(lookup());
    var json = """
        {
          "name": "James Joyce",
          "age": 38,
          "books": [
            "Finnegans Wake"
          ]
        }
        """;
    record Author(String name, List<String> books) { }
    var authorSpec = binder.findSpec(Author.class).orElseThrow();
    var classVisitor = authorSpec.createBindVisitor(BindClassVisitor.class);
    var author = (Author)JsonReader.parse(json, classVisitor.filterName(not("age"::equals)));
    assertEquals(new Author("James Joyce", List.of("Finnegans Wake")), author);
  }

  @Test
  public void readImmutable() {
    var binder = new Binder(lookup());
    var json = """
        {
          "name": "James Joyce",
          "age": 38,
          "books": [
            "Finnegans Wake"
          ]
        }
        """;
    record Author(String name, int age, List<String> books) { }
    var config = new BuilderConfig(HashMap::new, Map::copyOf, ArrayList::new, List::copyOf);
    var author = binder.read(json, Author.class, config);
    assertThrows(UnsupportedOperationException.class, () -> author.books().add("foo"));
  }
}