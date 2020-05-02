package fr.umlv.jsonapi.bind;

import static fr.umlv.jsonapi.bind.Binder.IN_ARRAY;
import static fr.umlv.jsonapi.bind.Binder.IN_OBJECT;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import fr.umlv.jsonapi.JsonReader;
import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.bind.Binder.BindingException;
import fr.umlv.jsonapi.bind.Spec.Converter;
import fr.umlv.jsonapi.bind.Spec.ObjectLayout;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

public class BinderReadTest {
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

      @Override
      public JsonValue convertFrom(JsonValue object) {
        // convert as String
        return JsonValue.from(object.toString());
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
    Object array = Binder.read(json, any.object().array());
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
    var shape = (Shape) Binder.read(json, spec);
    assertEquals(new Shape("red", 3), shape);
  }

  @Test
  public void readSecurity() {
    record Authorized() { }
    record Unauthorized() { }
    var binder = Binder.noDefaults();  // no finder registered !
    var recordFinder = SpecFinder.newRecordFinder(lookup(), binder::spec);
    var restrictedSet = Set.of(Authorized.class);
    // register the finder filtered !
    binder.register(recordFinder.filter(restrictedSet::contains));

    var json = "{}";
    var authorized = binder.read(json, Authorized.class);
    assertEquals(new Authorized(), authorized);

    assertThrows(BindingException.class, () -> binder.read(json, Unauthorized.class));
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
    var classVisitor = authorSpec.createBindVisitor(BindObjectVisitor.class);
    var author = (Author)JsonReader.parse(json, classVisitor.filterName(not("age"::equals)));
    assertEquals(new Author("James Joyce", List.of("Finnegans Wake")), author);
  }

  @Test
  public void readAndFilterSpec() {
    var binder = Binder.noDefaults();
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
    var recordFilter = SpecFinder.newRecordFinder(lookup(), binder::spec);
    var authorSpec = recordFilter.findSpec(Author.class).orElseThrow();
    binder.register(SpecFinder.associate(Author.class, authorSpec.filterName(not("age"::equals))));
    var author = binder.read(json, Author.class);
    assertEquals(new Author("James Joyce", List.of("Finnegans Wake")), author);
  }

  @Test
  public void readAndMapLayoutSpec() {
    var binder = Binder.noDefaults();
    var json = """
        {
          "name": "James Joyce",
          "age": 38,
          "books": [
            "Finnegans Wake"
          ]
        }
        """;
    record Author(String firstName, int age, List<String> books) { }
    var recordFinder = SpecFinder.newRecordFinder(lookup(), binder::spec);
    var authorSpec = recordFinder.findSpec(Author.class).orElseThrow();
    binder.register(SpecFinder.associate(Author.class, authorSpec.mapLayout(
        layout -> new ObjectLayout<>() {
          private String rename(String name) {
            return "name".equals(name)? "firstName": name;
          }
          private String reverseRename(String name) {
            return "firstName".equals(name)? "name": name;
          }

          @Override
          public Spec memberSpec(String memberName) {
            return layout.memberSpec(rename(memberName));
          }

          @Override
          public Object newBuilder() {
            return layout.newBuilder();
          }
          @Override
          public Object addObject(Object builder, String memberName, Object object) {
            return layout.addObject(builder, rename(memberName), object);
          }
          @Override
          public Object addArray(Object builder, String memberName, Object array) {
            return layout.addArray(builder, rename(memberName), array);
          }
          @Override
          public Object addValue(Object builder, String memberName, JsonValue value) {
            return layout.addValue(builder, rename(memberName), value);
          }
          @Override
          public Object build(Object builder) {
            return layout.build(builder);
          }

          @Override
          public void replay(Object object, MemberVisitor memberVisitor) {
            layout.replay(object, (name, value) -> memberVisitor
                .visitMember(reverseRename(name), value));
          }
        }
    )));
    var author = binder.read(json, Author.class);
    assertEquals(new Author("James Joyce", 38, List.of("Finnegans Wake")), author);
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
    var author = binder.read(json, Author.class);
    assertThrows(UnsupportedOperationException.class, () -> author.books().add("foo"));
  }

  @Test
  public void readInMapLayout() {
    var binder = Binder.noDefaults();
    var json = """
        {
          "x": 1,
          "y": 3,
          "color": "red"
        }
        """;
    class PixelObjectLayout implements ObjectLayout<Map<String, Object>> {
      @Override
      public Spec memberSpec(String memberName) {
        return switch(memberName) {
          case "x", "y" -> binder.spec(int.class);
          case "color" -> binder.spec(String.class);
          default -> throw new AssertionError();
        };
      }

      @Override
      public Map<String, Object> newBuilder() {
        return new HashMap<>();
      }
      @Override
      public Map<String, Object> addObject(Map<String, Object> builder, String memberName, Object object) {
        throw new AssertionError();
      }
      @Override
      public Map<String, Object> addArray(Map<String, Object> builder, String memberName, Object array) {
        throw new AssertionError();
      }
      @Override
      public Map<String, Object> addValue(Map<String, Object> builder, String memberName, JsonValue value) {
        builder.put(memberName, value.asObject());
        return builder;
      }
      @Override
      public Object build(Map<String, Object> builder) {
        return Map.copyOf(builder);  // make immutable
      }

      @Override
      public void replay(Object object, MemberVisitor memberVisitor) {
        throw new AssertionError();
      }
    }

    var pixelSpec = Spec.newTypedObject("Pixel", new PixelObjectLayout());
    @SuppressWarnings("unchecked")
    var pixel = (Map<String,Object>) Binder.read(json, pixelSpec);
    assertEquals(Map.of("x", 1, "y", 3, "color", "red"), pixel);
    assertThrows(UnsupportedOperationException.class, () -> pixel.put("x", 100));
  }

  @Test
  public void streamRecord() {
    var binder = new Binder(lookup());
    var json = """
        [ { "color": "red", "lines": 3 }, { "color": "blue", "lines": 1 } ]
        """;
    record Shape(String color, int lines) {}
    Stream<Shape> stream = binder.stream(json, Shape.class);
    assertEquals(
        List.of(new Shape("red", 3), new Shape("blue", 1)),
        stream.collect(toList()));
  }

  @Test
  public void streamOfStream() {
    var binder = new Binder(lookup());
    var json = """
        [ [ 1, 2, 3, 4, 5 ], [ 4, 6, 8, 10 ] ]
        """;
    var streamSpec = binder.spec(Object.class).stream(s -> s.mapToInt(o -> (int) o).toArray());
    Stream<int[]> stream = Binder.stream(json, streamSpec).map(o -> (int[]) o);
    assertArrayEquals(
        new int[] { 1, 2, 3, 4, 5, 4, 6, 8, 10 },
        stream.flatMapToInt(Arrays::stream).toArray());
  }
}