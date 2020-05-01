package fr.umlv.jsonapi.bind;

import static fr.umlv.jsonapi.bind.Binder.IN_ARRAY;
import static fr.umlv.jsonapi.bind.Binder.IN_OBJECT;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import fr.umlv.jsonapi.JsonReader;
import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.bind.Binder.BindingException;
import fr.umlv.jsonapi.bind.Spec.ClassLayout;
import fr.umlv.jsonapi.bind.Spec.Converter;
import fr.umlv.jsonapi.builder.BuilderConfig;
import java.net.URI;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  /*
  @Test
  public void writeAnOpaqueType() {
    var binder = new Binder(lookup());
    var map = new LinkedHashMap<String, Object>();
    map.put("id", 2475);
    map.put("date", LocalDate.of(2020, 2, 14));
    var text = binder.write(map);
    assertEquals("""
        { "id": 2475, "date": "2020-2-14" }\
        """, map);
  }*/

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
}