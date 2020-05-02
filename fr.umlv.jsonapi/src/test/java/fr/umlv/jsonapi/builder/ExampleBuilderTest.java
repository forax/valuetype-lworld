package fr.umlv.jsonapi.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import fr.umlv.jsonapi.JsonPrinter;
import fr.umlv.jsonapi.JsonReader;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ExampleBuilderTest {
  @Test
  public void parseBuilderToMap() {
    var text = """
      {
        "foo": 3,
        "bar": "whizz"
      }
      """;
    var builder = new ObjectBuilder();
    Map<String, Object> map = JsonReader.parse(text, builder);
    assertEquals(Map.of("foo", 3, "bar", "whizz"), map);
  }

  @Test
  public void parseBuilderToMap2() {
    var text = """
      { "name": "Franky", "address": {  "street": "3rd", "city": "NY" }  }
      """;
    var objectBuilder = new ObjectBuilder();
    Map<String, Object> map = JsonReader.parse(text, objectBuilder);
    assertEquals(
        Map.of("name", "Franky", "address", Map.of("street", "3rd", "city", "NY")),
        map);
  }

  @Test
  public void parseBuilderToList() {
    var text = """
      [ "foo", 42, 66.6 ]
      """;
    var builder = BuilderConfig
        .defaults()
        .withTransformListOp(List::copyOf)
        .newArrayBuilder();
    List<Object> list = JsonReader.parse(text, builder);
    assertEquals(List.of("foo", 42, 66.6), list);
  }

  @Test
  public void parseBuilderToList2() {
    var text = """
      [ "Jolene", "Joleene", "Joleeeene" ]
      """;
    var arrayBuilder = new ArrayBuilder();
    List<Object> list = JsonReader.parse(text, arrayBuilder);
    assertEquals(List.of("Jolene", "Joleene", "Joleeeene"), list);
  }

  @Test
  public void arrayBuilderToPrinter() {
    var arrayBuilder = new ArrayBuilder()
        .addAll("Jolene", "Joleene", "Joleeeene");
    var printer = new JsonPrinter();
    String text = arrayBuilder.replay(printer).toString();
    assertEquals("""
        [ "Jolene", "Joleene", "Joleeeene" ]\
        """, text);
  }

  @Test
  public void builderAddAndWith() {
    var builder = new ObjectBuilder()
        .add("id", 145)
        .withObject("info", b -> b
            .add("generator", 6))
        .withArray("data", b -> b
            .addAll(4, 7, 3, 78))
        .add("allowed", true);

    var text = builder.replay(new JsonPrinter()).toString();
    assertEquals("""
        { "id": 145, "info": { "generator": 6 }, "data": [ 4, 7, 3, 78 ], "allowed": true }\
        """, text);
  }

  @Test
  public void builderReplayToBuilder() {
    var builder = new ObjectBuilder()
        .add("name", "Franky")
        .withObject("address", b -> b
             .add("street", "3rd")
             .add("city", "NY"));
    var builder2 = new ObjectBuilder();
    builder.replay(builder2);
    assertEquals("""
        { "name": "Franky", "address": { "street": "3rd", "city": "NY" } }\
        """, builder2.toString());
  }
}