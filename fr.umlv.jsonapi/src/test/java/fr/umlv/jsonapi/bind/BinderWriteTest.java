package fr.umlv.jsonapi.bind;

import static java.lang.invoke.MethodHandles.lookup;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

public class BinderWriteTest {
  @Test
  public void writeAList() {
    var binder = new Binder(lookup());
    var list = List.of(1, "foo", 4.3);
    var text = binder.write(list);
    assertEquals("""
        [ 1, "foo", 4.3 ]\
        """, text);
  }

  @Test
  public void writeASet() {
    var binder = new Binder(lookup());
    var set = new LinkedHashSet<>(List.of(1, "foo", 4.3));
    var text = binder.write(set);
    assertEquals("""
        [ 1, "foo", 4.3 ]\
        """, text);
  }

  @Test
  public void writeAStream() {
    var binder = new Binder(lookup());
    var stream = Stream.of(1, "foo", 4.3);
    var text = binder.write(stream);
    assertEquals("""
        [ 1, "foo", 4.3 ]\
        """, text);
  }

  @Test
  public void writeAnIterator() {
    var binder = new Binder(lookup());
    var iterator = List.of(1, "foo", 4.3).iterator();
    var text = binder.write(iterator);
    assertEquals("""
        [ 1, "foo", 4.3 ]\
        """, text);
  }

  @Test
  public void writeAMap() {
    var binder = new Binder(lookup());
    var map = new LinkedHashMap<String, Object>();
    map.put("x", 3);
    map.put("y", 5.6);
    var text = binder.write(map);
    assertEquals("""
        { "x": 3, "y": 5.6 }\
        """, text);
  }

  @Test
  public void writeAnOpaqueType() {
    var binder = new Binder(lookup());
    binder.register(SpecFinder.newAnyTypesAsStringFinder());
    var map = new LinkedHashMap<String, Object>();
    map.put("id", 2475);
    map.put("date", LocalDate.of(2020, 2, 14));
    var text = binder.write(map);
    assertEquals("""
        { "id": 2475, "date": "2020-02-14" }\
        """, text);
  }

  @Test
  public void writeARecord() {
    var binder = new Binder(lookup());
    record Person(String name, int age, boolean bald) { }
    var person = new Person("Doctor X", 23, false);
    var text = binder.write(person);
    assertEquals("""
        { "name": "Doctor X", "age": 23, "bald": false }\
        """, text);
  }

  @Test
  public void writeARecordOfRecord() {
    var binder = new Binder(lookup());
    record Point(double x, double y) { }
    record Circle(Point center, double radius) { }
    var circle = new Circle(new Point(0, 0), 3);
    var text = binder.write(circle);
    assertEquals("""
        { "center": { "x": 0.0, "y": 0.0 }, "radius": 3.0 }\
        """, text);
  }

  @Test
  public void writeAListOfRecord() {
    var binder = new Binder(lookup());
    record Book(long id, String title) { }
    record Library(List<Book> books) { public Library { books = List.copyOf(books); } }
    var library = new Library(List.of(new Book(12, "Black Ice"), new Book(17, "The Poet")));
    var text = binder.write(library);
    assertEquals("""
        { "books": [ { "id": 12, "title": "Black Ice" }, { "id": 17, "title": "The Poet" } ] }\
        """, text);
  }

  @Test
  public void writeAMapOfRecord() {
    var binder = new Binder(lookup());
    record Person(String name, Map<String, Boolean> trueFriends) { }
    var bob = new Person("Bob", Map.of());
    var ana = new Person("Ana", Map.of("Bob", true));
    var text = binder.write(List.of(bob, ana));
    assertEquals("""
        [\
         { "name": "Bob", "trueFriends": {  } },\
         { "name": "Ana", "trueFriends": { "Bob": true } } \
        ]\
        """, text);
  }
}