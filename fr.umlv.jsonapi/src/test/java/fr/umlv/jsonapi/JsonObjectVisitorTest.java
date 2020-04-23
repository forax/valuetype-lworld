package fr.umlv.jsonapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
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
    var visitor = new JsonVisitor() {
      private final ArrayList<String> methods = new ArrayList<>();

      @Override
      public JsonObjectVisitor visitObject() {
        return new JsonObjectVisitor() {
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
          public void visitMemberText(String name, JsonText text) {
            methods.add("visitMemberText");
            assertEquals("firstName", name);
            assertEquals("John", text.value());
          }

          @Override
          public void visitMemberNumber(String name, JsonNumber number) {
            methods.add("visitMemberNumber");
            switch(name) {
              case "weight" -> {
                assertTrue(number.isDouble());
                assertFalse(number.isLong());
                assertEquals(87.3, number.doubleValue());
              }
              case "age" -> {
                assertFalse(number.isDouble());
                assertTrue(number.isLong());
                assertEquals(27L, number.longValue());
                assertEquals(27, number.convertToInt());
              }
              default -> fail(name);
            }
          }

          @Override
          public void visitMemberConstant(String name, JsonConstant constant) {
            methods.add("visitMemberConstant");
            switch (name) {
              case "isAlive" -> assertEquals(JsonConstant.TRUE, constant);
              case "spouse" -> assertEquals(JsonConstant.NULL, constant);
              default -> fail(name);
            }
          }

          @Override
          public void visitEndObject() {
            methods.add("visitEndObject");
          }
        };
      }

      @Override
      public JsonArrayVisitor visitArray() {
        fail();
        throw null;
      }
    };
    JsonReader.parse(text, visitor);
    assertEquals(
        List.of("visitMemberText", "visitMemberConstant", "visitMemberNumber",
            "visitMemberNumber",  "visitMemberConstant", "visitMemberObject",
            "visitMemberArray", "visitEndObject"),
        visitor.methods);  // each method above is called once
  }

  @Test
  public void testJsonReaderArrayVisitor() {
    var text = """
        [
          "Jane", false, 72, 37.8, null,
          { "skipped1": "skipped2" }, [ "skipped3", "skipped4" ]
        ]
        """;
    var visitor = new JsonVisitor() {
      private final ArrayList<String> methods = new ArrayList<>();

      @Override
      public JsonArrayVisitor visitArray() {
        return new JsonArrayVisitor() {
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
          public void visitText(JsonText text) {
            methods.add("visitText");
            assertEquals("Jane", text.value());
            assertEquals("\"Jane\"", text.toString());
          }

          @Override
          public void visitNumber(JsonNumber number) {
            methods.add("visitNumber");
            switch (number.toString()) {
              case "72" -> {
                assertTrue(number.isLong());
                assertFalse(number.isDouble());
                assertEquals(72L, number.longValue());
                assertEquals(72, number.convertToInt());
              }
              case "37.8" -> {
                assertFalse(number.isLong());
                assertTrue(number.isDouble());
                assertEquals(37.8, number.doubleValue());
              }
              default -> fail(number.toString());
            }
          }

          @Override
          public void visitConstant(JsonConstant constant) {
            methods.add("visitConstant");
            switch(constant.toString()) {
              case "false" -> assertFalse(constant.value());
              case "null" -> assertNull(constant.value());
              default -> fail(constant.toString());
            }
          }

          @Override
          public void visitEndArray() {
            methods.add("visitEndArray");
          }
        };
      }

      @Override
      public JsonObjectVisitor visitObject() {
        fail();
        throw null;
      }
    };
    JsonReader.parse(text, visitor);
    assertEquals(
        List.of("visitText", "visitConstant", "visitNumber", "visitNumber",
            "visitConstant", "visitObject", "visitArray", "visitEndArray"),
        visitor.methods);  // each method above is called once
  }

  /*
  @Test
  public void testSimpleJSonObjectEquals() {
    var object = new JsonObject()
        .adding("firstName", "Bob")
        .adding("age", 21);
    assertEquals(
        Map.of("firstName", "bob", "age", 21),
        object);
  }

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