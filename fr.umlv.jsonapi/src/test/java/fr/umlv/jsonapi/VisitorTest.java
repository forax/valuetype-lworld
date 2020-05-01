package fr.umlv.jsonapi;

import static fr.umlv.jsonapi.VisitorMode.PULL;
import static fr.umlv.jsonapi.VisitorMode.PULL_INSIDE;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

public class VisitorTest {
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
      public VisitorMode visitStartObject() {
        return VisitorMode.PUSH;
      }

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
      public Object visitMemberValue(String name, JsonValue value) {
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
          case FALSE, LONG, BIG_INTEGER -> fail();
        }
        return null;
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
      public VisitorMode visitStartArray() {
        return VisitorMode.PUSH;
      }

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
          case TRUE, LONG, BIG_INTEGER -> fail();
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
    var visitor = new ArrayVisitor() {
      @Override
      public VisitorMode visitStartArray() {
        return PULL_INSIDE;
      }

      @Override
      public Object visitStream(Stream<Object> stream) {
        methods.add("visitStream");
        return stream.collect(toList());
      }
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
          case TRUE, LONG, BIG_INTEGER -> fail();
        }
        return value.asObject();
      }

      @Override
      public Object visitEndArray() {
        return null;
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
    var visitor = new ArrayVisitor() {
      @Override
      public VisitorMode visitStartArray() {
        return PULL_INSIDE;
      }
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
      @Override
      public Object visitEndArray() {
        return null;
      }
    };
    var result = JsonReader.parse(text, visitor);
    assertEquals("foo", result);
  }

  public void testParseAsStreamOfString() {
    var text = """
        [ "foo", "bar", "baz", "whizz" ]
        """;

    var stream = JsonReader.stream(text, new ArrayVisitor() {
      @Override
      public VisitorMode visitStartArray() {
        return PULL;
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
      @Override
      public Object visitEndArray() {
        return null;
      }
    });
    var list = stream.limit(2).collect(toList());
    assertEquals(List.of("foo", "bar"), list);
  }
}