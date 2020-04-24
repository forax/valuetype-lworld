package fr.umlv.jsonapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    var visitor = new JsonObjectVisitor() {
      @Override
      public JsonObjectVisitor visitMemberObject(String name) {
        methods.add("visitMemberObject");
        assertEquals("address", name);
        return null;
      }

      @Override
      public JsonArrayVisitor visitMemberArray(String name) {
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
          }
          case TRUE -> {
            assertEquals("isAlive", name);
            assertEquals(JsonValue.trueValue(), value);
            assertTrue(value.booleanValue());
          }
          case INT -> {
            assertEquals("age", name);
            assertEquals(JsonValue.from(27), value);
            assertEquals(27, value.intValue());
          }
          case DOUBLE -> {
            assertEquals("weight", name);
            assertEquals(JsonValue.from(87.3), value);
            assertEquals(87.3, value.doubleValue());
          }
          case STRING -> {
            assertEquals("firstName", name);
            assertEquals(JsonValue.from("John"), value);
            assertEquals("John", value.stringValue());
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
    var visitor = new JsonArrayVisitor() {
          @Override
          public JsonObjectVisitor visitObject() {
            methods.add("visitObject");
            return null;
          }

          @Override
          public JsonArrayVisitor visitArray() {
            methods.add("visitArray");
            return null;
          }

          @Override
          public void visitValue(JsonValue value) {
            methods.add("visitValue+" + value.kind());
            switch(value.kind()) {
              case NULL -> assertEquals(JsonValue.nullValue(), value);
              case FALSE -> {
                assertEquals(JsonValue.falseValue(), value);
                assertFalse(value.booleanValue());
              }
              case INT -> {
                assertEquals(JsonValue.from(72), value);
                assertEquals(72, value.intValue());
              }
              case DOUBLE -> {
                assertEquals(JsonValue.from(37.8), value);
                assertEquals(37.8, value.doubleValue());
              }
              case STRING -> {
                assertEquals(JsonValue.from("Jane"), value);
                assertEquals("Jane", value.stringValue());
              }
              case TRUE, LONG, BIG_INTEGER, BIG_DECIMAL -> fail();
            }
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
  public void testSimpleJSonObjectToMap() {
    var object = new JsonObjectBuilder()
        .add("firstName", "Bob")
        .add("age", 21);
    var object2 = new JsonObjectBuilder();
    object.accept(object2);
    assertEquals(
        Map.of("firstName", "Bob", "age", 21),
        object2.toMap());
  }

  /*
  @Test
  public void testSimpleJSonArrayEquals() {
    var array = JsonArray.of(1, 5, 43, 7, 56);
    assertEquals(List.of(1, 5, 43, 7, 56), array);
  }

  private static final String DATA = """
        {
          "firstName": "John",
          "lastName": "Smith",
          "isAlive": true,
          "age": 27,
          "weight": 87.3,
          "balance": 123454554533, 
          "id": 1235345426364636494428583545,
          "address": {
            "streetAddress": "21 2nd Street",
            "city": "New York",
            "state": "NY",
            "postalCode": "10021-3100"
          },
          "phoneNumbers": [
            {
              "type": "home",
              "number": "212 555-1234"
            },
            {
              "type": "office",
              "number": "646 555-4567"
            }
          ],
          "children": [],
          "spouse": null
        }
        """;

  @Test
  public void testComplexJsonObject() {
    var jsonObject = new JsonObject();
    JsonReader.parse(DATA, jsonObject);
    assertAll(
        () -> assertEquals("John", jsonObject.get("firstName")),
        () -> assertEquals("Smith", jsonObject.get("lastName")),
        () -> assertTrue(jsonObject.getOrDefaultBoolean("isAlive", false)),
        () -> assertEquals(27, jsonObject.getOrDefaultInt("age", 0)),
        () -> assertEquals(123454554533L, jsonObject.getOrDefaultLong("balance", 0)),
        () -> assertEquals(87.3, jsonObject.getOrDefaultDouble("weight", 0)),
        () -> assertEquals(new BigInteger("1235345426364636494428583545"), jsonObject.getOrDefault("id", BigInteger.ZERO)),
        () -> assertEquals(
            Map.of("streetAddress", "21 2nd Street",
                   "city", "New York",
                   "state", "NY",
                   "postalCode", "10021-3100"),
            jsonObject.get("address")),
        () -> assertEquals(
            List.of(Map.of("type", "home", "number", "212 555-1234"),
                Map.of("type", "office", "number", "646 555-4567")),
            jsonObject.get("phoneNumbers")),
        () -> assertEquals(List.of(), jsonObject.get("children")),
        () -> assertTrue(jsonObject.isNull("spouse"))
    );
  }

  @Test
  public void testRecordJsonObject() {
    record DataPoint(String name, int x, int y) {}
    var dataPoint = new DataPoint("origin", 23, 7);
    var jsonObject = new JsonObject(dataPoint);
    assertAll(
        () -> assertEquals(3, jsonObject.size()),
        () -> assertEquals("origin", jsonObject.get("name")),
        () -> assertEquals(23, jsonObject.get("x")),
        () -> assertEquals(23, jsonObject.getOrDefaultInt("x", 0)),
        () -> assertEquals(7, jsonObject.get("y")),
        () -> assertEquals(7, jsonObject.getOrDefaultInt("y", 0)),
        () -> assertEquals("""
         { "name": "origin", "x": 23, "y": 7 }\
         """, jsonObject.toString())
    );
  }

  @Test
  public void testJsonObjectToRecord() {
    var jsonObject = new JsonObject()
        .adding("name", "origin")
        .adding("x", 23)
        .adding("y", 7);
    record DataPoint(String name, int x, int y) {}
    var dataPoint = jsonObject.toRecord(MethodHandles.lookup(), DataPoint.class);
    assertAll(
        () -> assertEquals("origin", dataPoint.name),
        () -> assertEquals(23, dataPoint.x),
        () -> assertEquals(7, dataPoint.y)
    );
  }*/
}