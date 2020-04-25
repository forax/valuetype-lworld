package fr.umlv.jsonapi.bind;

import static fr.umlv.jsonapi.bind.Binder.ARRAY;
import static fr.umlv.jsonapi.bind.Binder.OBJECT;
import static java.lang.invoke.MethodHandles.lookup;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;
import java.util.Map;
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
    Object o = Binder.read(json, any.object().array());
    assertEquals(
        List.of(
            Map.of("color", "red", "lines", 3),
            Map.of("color", "blue", "lines", 1)),
        o);
  }
}