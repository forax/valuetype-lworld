package fr.umlv.jsonapi.bind;

import static fr.umlv.jsonapi.bind.Binder.IN_ARRAY;
import static fr.umlv.jsonapi.bind.Binder.IN_OBJECT;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.function.Predicate.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import fr.umlv.jsonapi.BuilderConfig;
import fr.umlv.jsonapi.JsonReader;
import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.bind.Binder.SpecNoFoundException;
import fr.umlv.jsonapi.bind.Spec.Converter;
import java.net.URI;
import java.time.LocalDate;
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
    assertNotNull(binder.spec(int.class));
    assertNotNull(binder.spec(String.class));
    assertThrows(SpecNoFoundException.class, () -> binder.spec(URI.class));
  }

  @Test
  public void findSpec() {
    var binder = new Binder(lookup());
    record Point(int x, int y) { }
    var spec = binder.spec(Point.class);
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
    Map<String, String> map = binder.read(json, String.class, IN_OBJECT);
    assertEquals(Map.of("firstName", "John", "lastName", "Boijoly"), map);
  }

  @Test
  public void readArrayInt() {
    var binder = Binder.noDefaults();
    var json = """
        [ 1, 2, 3, 4, 10 ]
        """;
    List<Integer> array = binder.read(json, int.class, IN_ARRAY);
    assertEquals(List.of(1, 2, 3, 4, 10), array);
  }

  @Test
  public void readArrayOfRecord() {
    var binder = new Binder(lookup());
    var json = """
        [ { "color": "red", "lines": 3 }, { "color": "blue", "lines": 1 } ]
        """;
    record Shape(String color, int lines) { }
    List<Shape> array = binder.read(json, Shape.class, IN_ARRAY);
    assertEquals(List.of(new Shape("red", 3), new Shape("blue", 1)), array);
  }

  @Test
  public void readBinderRegisterLocalDate() {
    var binder = new Binder(lookup());
    var stringSpec = binder.spec(String.class);
    var localDateSpec = stringSpec.convert(new Converter() {
      @Override
      public JsonValue convertTo(JsonValue value) {
        return JsonValue.fromOpaque(LocalDate.parse(value.stringValue()));
      }
    });
    binder.register(SpecFinder.from(Map.of(LocalDate.class, localDateSpec)));
    record Order(LocalDate date) { }
    var json = """
        { "date": "2007-12-03" }
        """;
    Order order = binder.read(json, Order.class);
    assertEquals(new Order(LocalDate.of(2007, 12, 3)), order);
  }

  @Test
  public void readSpec() {
    var binder = new Binder(lookup());
    var json = """
        [ { "color": "red", "lines": 3 }, { "color": "blue", "lines": 1 } ]
        """;
    var any = binder.spec(Object.class);
    Object array = Binder.read(json, any.object().array(), new BuilderConfig());
    assertEquals(
        List.of(
            Map.of("color", "red", "lines", 3),
            Map.of("color", "blue", "lines", 1)),
        array);
  }

  @Test
  public void readStreamSpec() {
    var binder = new Binder(lookup());
    var json = """
        [ { "color": "red", "lines": 3 }, { "color": "blue", "lines": 1 } ]
        """;
    record Shape(String color, int lines) { }
    var spec = binder.spec(Shape.class).stream(s -> s.findFirst().orElseThrow());
    var shape = (Shape) Binder.read(json, spec, new BuilderConfig());
    assertEquals(new Shape("red", 3), shape);
  }

  @Test
  public void readSecurity() {
    record Authorized() { }
    record Unauthorized() { }
    var binder = Binder.noDefaults();  // no finder registered !
    var recordFinder = binder.newRecordSpecFinder(lookup());
    var restrictedSet = Set.of(Authorized.class);
    // register the finder filtered !
    binder.register(recordFinder.filter(restrictedSet::contains));

    var json = "{}";
    var authorized = binder.read(json, Authorized.class);
    assertEquals(new Authorized(), authorized);

    assertThrows(SpecNoFoundException.class, () -> binder.read(json, Unauthorized.class));
  }

  @Test
  public void readAndFilterClassVisitor() {
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
    var authorSpec = binder.spec(Author.class);
    var classVisitor = authorSpec.createBindVisitor(BindClassVisitor.class);
    var author = (Author)JsonReader.parse(json, classVisitor.filterName(not("age"::equals)));
    assertEquals(new Author("James Joyce", List.of("Finnegans Wake")), author);
  }

  @Test
  public void readAndFilterSpec() {
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
    var authorSpec = binder.specFinder().findSpec(Author.class).orElseThrow();
    binder.register(SpecFinder.from(Map.of(Author.class, authorSpec.filter(not("age"::equals)))));
    var author = binder.read(json, Author.class);
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