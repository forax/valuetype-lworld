package fr.umlv.jsonapi;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

public class JsonObjectVisitorTest {
  @Test
  public void testJsonReaderObjectVisitor() {
    var text = """
        {
          "firstName": "John",
          "isAlive": true,
          "age": 27,
          "weight": 87.3,
          "spouse": null,
          "address": { "skipped1": "skipped2" },
          "phoneNumbers": [ "skipped3", "skipped4", "skipped5" ]
        }
        """;
    var methods = new ArrayList<String>();
    var visitor = new ObjectVisitor() {
      @Override
      public ObjectVisitor visitMemberObject(String name) {
        methods.add("visitMemberObject");
        assertEquals("address", name);
        return null;
      }

      @Override
      public ArrayVisitor visitMemberArray(String name) {
        methods.add("visitMemberArray");
        assertEquals("phoneNumbers", name);
        return null;
      }

      @Override
      public void visitMemberValue(String name, JsonValue value) {
        methods.add("visitMemberValue+" + value.kind());
        switch (value.kind()) {
          case NULL -> {
            assertEquals("spouse", name);
            assertEquals(JsonValue.nullValue(), value);
            assertTrue(value.isNull());
          }
          case TRUE -> {
            assertEquals("isAlive", name);
            assertEquals(JsonValue.trueValue(), value);
            assertTrue(value.booleanValue());
            assertTrue(value.isTrue());
          }
          case INT -> {
            assertEquals("age", name);
            assertEquals(JsonValue.from(27), value);
            assertEquals(27, value.intValue());
            assertTrue(value.isInt());
          }
          case DOUBLE -> {
            assertEquals("weight", name);
            assertEquals(JsonValue.from(87.3), value);
            assertEquals(87.3, value.doubleValue());
            assertTrue(value.isDouble());
          }
          case STRING -> {
            assertEquals("firstName", name);
            assertEquals(JsonValue.from("John"), value);
            assertEquals("John", value.stringValue());
            assertTrue(value.isString());
          }
          case FALSE, LONG, BIG_INTEGER, BIG_DECIMAL -> fail();
        }
      }

      @Override
      public Object visitEndObject() {
        methods.add("visitEndObject");
        return null;
      }
    };

    JsonReader.parse(text, visitor);
    assertEquals(
        List.of("visitMemberValue+STRING", "visitMemberValue+TRUE", "visitMemberValue+INT",
            "visitMemberValue+DOUBLE", "visitMemberValue+NULL", "visitMemberObject",
            "visitMemberArray", "visitEndObject"),
        methods);
  }

  @Test
  public void testJsonReaderArrayVisitor() {
    var text = """
        [
          "Jane", false, 72, 37.8, null,
          { "skipped1": "skipped2" }, [ "skipped3", "skipped4" ]
        ]
        """;
    var methods = new ArrayList<String>();
    var visitor = new ArrayVisitor() {
          @Override
          public ObjectVisitor visitObject() {
            methods.add("visitObject");
            return null;
          }

          @Override
          public ArrayVisitor visitArray() {
            methods.add("visitArray");
            return null;
          }

          @Override
          public Object visitValue(JsonValue value) {
            methods.add("visitValue+" + value.kind());
            switch(value.kind()) {
              case NULL -> assertEquals(JsonValue.nullValue(), value);
              case FALSE -> {
                assertEquals(JsonValue.falseValue(), value);
                assertFalse(value.booleanValue());
                assertTrue(value.isFalse());
              }
              case INT -> {
                assertEquals(JsonValue.from(72), value);
                assertEquals(72, value.intValue());
                assertTrue(value.isInt());
              }
              case DOUBLE -> {
                assertEquals(JsonValue.from(37.8), value);
                assertEquals(37.8, value.doubleValue());
                assertTrue(value.isDouble());
              }
              case STRING -> {
                assertEquals(JsonValue.from("Jane"), value);
                assertEquals("Jane", value.stringValue());
                assertTrue(value.isString());
              }
              case TRUE, LONG, BIG_INTEGER, BIG_DECIMAL -> fail();
            }
            return null;
          }

          @Override
          public Object visitEndArray() {
            methods.add("visitEndArray");
            return null;
          }
        };

    JsonReader.parse(text, visitor);
    assertEquals(
        List.of("visitValue+STRING", "visitValue+FALSE", "visitValue+INT",
            "visitValue+DOUBLE", "visitValue+NULL", "visitObject", "visitArray",
            "visitEndArray"),
        methods);
  }

  @Test
  public void testJsonReaderStreamVisitor() {
    var text = """
        [
          "Jane", false, 72, 37.8, null,
          { "skipped1": "skipped2" }, [ "skipped3", "skipped4" ]
        ]
        """;
    var methods = new ArrayList<String>();
    var visitor = new StreamVisitor() {
      @Override
      public ObjectVisitor visitObject() {
        methods.add("visitObject");
        return null;
      }

      @Override
      public ArrayVisitor visitArray() {
        methods.add("visitArray");
        return null;
      }

      @Override
      public Object visitStream(Stream<Object> stream) {
        methods.add("visitStream");
        return stream.collect(toList());
      }

      @Override
      public Object visitValue(JsonValue value) {
        methods.add("visitValue+" + value.kind());
        switch(value.kind()) {
          case NULL -> assertEquals(JsonValue.nullValue(), value);
          case FALSE -> {
            assertEquals(JsonValue.falseValue(), value);
            assertFalse(value.booleanValue());
            assertTrue(value.isFalse());
          }
          case INT -> {
            assertEquals(JsonValue.from(72), value);
            assertEquals(72, value.intValue());
            assertTrue(value.isInt());
          }
          case DOUBLE -> {
            assertEquals(JsonValue.from(37.8), value);
            assertEquals(37.8, value.doubleValue());
            assertTrue(value.isDouble());
          }
          case STRING -> {
            assertEquals(JsonValue.from("Jane"), value);
            assertEquals("Jane", value.stringValue());
            assertTrue(value.isString());
          }
          case TRUE, LONG, BIG_INTEGER, BIG_DECIMAL -> fail();
        }
        return value.asObject();
      }
    };

    var result = JsonReader.parse(text, visitor);
    assertEquals(Arrays.asList("Jane", false, 72, 37.8, null), result);
    assertEquals(
        List.of("visitStream", "visitValue+STRING", "visitValue+FALSE",
            "visitValue+INT", "visitValue+DOUBLE", "visitValue+NULL", "visitObject",
            "visitArray"),
        methods);
  }

  @Test
  public void testJsonReaderShortCircuitStreamVisitor() {
    var text = """
        [ "foo", "bar", 456 ]
        """;
    var visitor = new StreamVisitor() {
      @Override
      public Object visitStream(Stream<Object> stream) {
        return stream.findFirst().orElseThrow();
      }

      @Override
      public ObjectVisitor visitObject() {
        return null;
      }
      @Override
      public ArrayVisitor visitArray() {
        return null;
      }
      @Override
      public Object visitValue(JsonValue value) {
        return value.asObject();
      }
    };
    var result = JsonReader.parse(text, visitor);
    assertEquals("foo", result);
  }

  @Test
  public void testSimpleObjectParsingToMap() {
    var text = """
        {
          "foo": 3,
          "bar": "whizz"
        }
        """;
    var builder = new ObjectBuilder();
    JsonReader.parse(text, builder);
    assertEquals(
        Map.of("foo", 3, "bar", "whizz"),
        builder.toMap());
  }

  @Test
  public void testSimpleObjectRenaming() {
    var text = """
        {
          "firstName": "John",
          "lastName": "Smith",
          "age": 27,
          "personal-address": {
            "streetAddress": "21 2nd Street",
            "city": "New York",
            "state": "NY",
            "postalCode": "10021-3100"
          }
        }
        """;
    var builder = new ObjectBuilder();
    JsonReader.parse(text, builder.mapName(name -> switch (name) {
      case "personal-address" -> "address";
      default -> name;
    }));
    assertEquals(
        Map.of("streetAddress", "21 2nd Street", "city", "New York", "state", "NY", "postalCode", "10021-3100"),
        builder.toMap().get("address"));
  }

  @Test
  public void testSimpleObjectFiltering() {
    var text = """
        {
          "firstName": "John",
          "lastName": "Smith",
          "age": 27,
          "address": {
            "streetAddress": "21 2nd Street",
            "city": "New York",
            "state": "NY",
            "postalCode": "10021-3100"
          }
        }
        """;
    var builder = new ObjectBuilder();
    JsonReader.parse(text, builder.filterName(name -> switch (name) {
      case "address", "firstName" -> false;
      default -> true;
    }));
    assertEquals(
        Map.of("lastName", "Smith", "age", 27),
        builder.toMap());
  }

  @Test
  public void testSimpleArrayParsingToList() {
    var text = """
        [ "foo", 42, { "bar": 66.6 } ]
        """;
    var builder = new ArrayBuilder(HashMap::new, Map::copyOf, ArrayList::new, List::copyOf);
    JsonReader.parse(text, builder);
    assertEquals(
        List.of("foo", 42, Map.of("bar", 66.6)),
        builder.toList());
  }

  @Test
  public void testSimpleJSonObjectToMap() {
    var builder = new ObjectBuilder()
        .add("firstName", "Bob")
        .add("age", 21);
    var object2 = new ObjectBuilder();
    builder.accept(() -> object2);
    assertEquals(
        Map.of("firstName", "Bob", "age", 21),
        object2.toMap());
  }

  @Test
  public void testSimpleJSonObjectToSortedMap() {
    var builder = new ObjectBuilder()
        .add("firstName", "Bob")
        .add("age", 21)
        .add("weight", 70.2)
        .add("spouse", null)
        .add("children", true);
    var object2 = new ObjectBuilder(TreeMap::new, ArrayList::new);
    builder.accept(() -> object2);
    assertEquals(
        List.of("age", "children", "firstName", "spouse", "weight"),
        new ArrayList<>(object2.toMap().keySet()));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testSimpleStreamOfMap() {
    var text = """
        [ { "x": 4, "y": 7 }, { "x": 14, "y": 71 } ]
        """;
    var visitor = new StreamVisitor() {
      @Override
      public Object visitStream(Stream<Object> stream) {
        return stream.map(v -> (Map<String, Integer>) v).filter(p -> p.get("y") < 10).collect(toUnmodifiableList());
      }

      @Override
      public ObjectVisitor visitObject() {
        return new ObjectBuilder();
      }
      @Override
      public ArrayVisitor visitArray() {
        return null;
      }
      @Override
      public Object visitValue(JsonValue value) {
        return value.asObject();
      }
    };
    var result = (List<Map<String, Integer>>) JsonReader.parse(text, visitor);
    assertEquals(List.of(Map.of("x", 4, "y", 7)), result);
  }

  @Test
  public void testParseAsStreamOfString() {
    var text = """
        [ "foo", "bar", "baz", "whizz" ]
        """;

    var stream = JsonReader.stream(text, new ArrayVisitor() {
      @Override
      public ObjectVisitor visitObject() {
        return null;
      }
      @Override
      public ArrayVisitor visitArray() {
        return null;
      }
      @Override
      public Object visitValue(JsonValue value) {
        return value.asObject();
      }
      @Override
      public Object visitEndArray() {
        return null;
      }
    });
    var list = stream.limit(2).collect(toList());
    assertEquals(List.of("foo", "bar"), list);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testParseAsStreamOfObject() {
    var text = """
        [ { "x": 4, "y": 7 }, { "x": 14, "y": 71 } ]
        """;

    var stream = JsonReader.stream(text, new ArrayVisitor() {
          @Override
          public ObjectVisitor visitObject() {
            return new ObjectBuilder();
          }
          @Override
          public ArrayVisitor visitArray() {
            throw new AssertionError();
          }
          @Override
          public Object visitValue(JsonValue value) {
            throw new AssertionError();
          }
          @Override
          public Object visitEndArray() {
            return null;
          }
        });
    var point = stream.map(o -> (Map<String, Object>) o).findFirst().orElseThrow();
    assertEquals(Map.of("x", 4, "y", 7), point);
  }
}