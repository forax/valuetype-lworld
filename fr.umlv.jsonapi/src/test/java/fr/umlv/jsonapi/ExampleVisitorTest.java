package fr.umlv.jsonapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

public class ExampleVisitorTest {
  @Test
  public void testSimpleObjectPushMode() {
    var text = """
        {
          "name": "Mr Robot",
          "children": [ "Elliot", "Darlene" ]
        }
        """;
    var visitor = new ObjectVisitor() {
      @Override
      public VisitorMode mode() {
        return VisitorMode.PUSH;
      }
      @Override
      public ObjectVisitor visitMemberObject(String name) {
        return null;  // skip it
      }
      @Override
      public ArrayVisitor visitMemberArray(String name) {
        assertEquals("children", name);
        return null;  // skip it
      }
      @Override
      public Object visitMemberValue(String name, JsonValue value) {
        assertEquals("Mr Robot", value.stringValue());
        return null;
      }
      @Override
      public Object visitEndObject() {
        return "end !";  // send result
      }
    };
    var result = JsonReader.parse(text, visitor);
    assertEquals(result, "end !");
  }

  @Test
  public void testSimpleObjectPullMode() {
    var text = """
        {
          "name": "Mr Robot",
          "children": [ "Elliot", "Darlene" ]
        }
        """;
    var builderConfig =  BuilderConfig.defaults();
    var visitor = builderConfig.newObjectBuilder(new ObjectVisitor() {
      @Override
      public VisitorMode mode() {
        return VisitorMode.PULL;
      }
      @Override
      public ObjectVisitor visitMemberObject(String name) {
        return null;  // skip it
      }
      @Override
      public ArrayVisitor visitMemberArray(String name) {
        assertEquals("children", name);
        return null;  // skip it
      }
      @Override
      public Object visitMemberValue(String name, JsonValue value) {
        assertEquals("Mr Robot", value.stringValue());
        return "Mrs Robot";
      }
      @Override
      public Object visitEndObject() {
        return null;  // result ignored
      }
    });
    var map = JsonReader.parse(text, visitor);
    assertEquals(Map.of("name", "Mrs Robot"), map);
  }

  @Test
  public void testSimpleArrayPushMode() {
    var text = """
        [ "Jolene", "Joleene", "Joleeeene" ]
        """;
    var visitor = new ArrayVisitor() {
      @Override
      public VisitorMode mode() {
        return VisitorMode.PUSH;
      }
      @Override
      public ObjectVisitor visitObject() {
        return null;  // skip it
      }
      @Override
      public ArrayVisitor visitArray() {
        return null;  // skip it
      }
      @Override
      public Object visitValue(JsonValue value) {
        assertTrue(value.stringValue().startsWith("Jole"));
        return null;  // used in push mode
      }
      @Override
      public Object visitEndArray() {
        return "end !";  // send result;
      }
    };
    var result = JsonReader.parse(text, visitor);
    assertEquals(result, "end !");
  }

  @Test
  public void testSimpleArrayPullMode() {
    var text = """
        [ "Jolene", "Joleene", "Joleeeene" ]
        """;
    var visitor = new ArrayVisitor() {
      @Override
      public VisitorMode mode() {
        return VisitorMode.PULL;
      }
      @Override
      public ObjectVisitor visitObject() {
        return null;  // skip it
      }
      @Override
      public ArrayVisitor visitArray() {
        return null;  // skip it
      }
      @Override
      public Object visitValue(JsonValue value) {
        assertTrue(value.stringValue().startsWith("Jole"));
        return value.asObject();  // used in pull mode
      }
      @Override
      public Object visitEndArray() {
        return null;  // return value ignored;
      }
    };
    try(var stream = JsonReader.stream(text, visitor)) {
      assertEquals("Joleene", stream.skip(1).findFirst().orElseThrow());
    }
  }

  @Test
  public void testSimpleArrayStreamVisitor() {
    var text = """
        [ "Jolene", "Joleene", "Joleeeene" ]
        """;
    var visitor = new ArrayVisitor() {
      @Override
      public VisitorMode mode() {
        return VisitorMode.PULL_INSIDE;
      }
      @Override
      public Object visitStream(Stream<Object> stream) {
        return stream.skip(1).findFirst().orElseThrow();
      }
      @Override
      public Object visitValue(JsonValue value) {
        assertTrue(value.stringValue().startsWith("Jole"));
        return value.asObject();  // used in pull mode
      }

      @Override
      public ObjectVisitor visitObject() {
        return null;  // skip it
      }
      @Override
      public ArrayVisitor visitArray() {
        return null;  // skip it
      }
      @Override
      public Object visitEndArray() {
        return null;  // unused
      }
    };
    var result = JsonReader.parse(text, visitor);
    assertEquals("Joleene", result);
  }


  @Test
  public void testSimpleArrayBuilderParse() {
    var text = """
        [ "Jolene", "Joleene", "Joleeeene" ]
        """;
    var arrayBuilder = new ArrayBuilder();
    var list = JsonReader.parse(text, arrayBuilder);
    assertEquals(List.of("Jolene", "Joleene", "Joleeeene"), list);
  }

  @Test
  public void testSimpleArrayBuilderAccept() {
    var arrayBuilder = new ArrayBuilder()
        .add("Jolene")
        .addAll("Joleene", "Joleeeene");
    var printer = new JsonPrinter();
    arrayBuilder.accept(printer::visitArray);
    assertEquals("""  
        [ "Jolene", "Joleene", "Joleeeene" ]\
        """, printer.toString());
  }

  @Test
  public void testSimpleObjectBuilderParse() {
    var text = """
        { "name": "Franky", "address": {  "street": "3rd", "city": "NY" } }
        """;
    var objectBuilder = new ObjectBuilder();
    var map = JsonReader.parse(text, objectBuilder);
    assertEquals(
        Map.of("name", "Franky",
               "address", Map.of("street", "3rd", "city", "NY")),
        map);
  }

  @Test
  public void testSimpleObjectBuilderAccept() {
    var objectBuilder = new BuilderConfig(LinkedHashMap::new, ArrayList::new)
        .newObjectBuilder()
        .add("name", "Franky")
        .with("address", b -> b
            .add("street", "3rd")
            .add("city", "NY"));
    var printer = new JsonPrinter();
    objectBuilder.accept(printer::visitObject);
    assertEquals("""
        { "name": "Franky", "address": { "street": "3rd", "city": "NY" } }\
        """, printer.toString());
  }
}
