package fr.umlv.jsonapi.builder;

import static fr.umlv.jsonapi.VisitorMode.PULL;
import static fr.umlv.jsonapi.VisitorMode.PULL_INSIDE;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import fr.umlv.jsonapi.ArrayVisitor;
import fr.umlv.jsonapi.JsonReader;
import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.ObjectVisitor;
import fr.umlv.jsonapi.VisitorMode;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

public class BuilderTest {
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
        [ null, false, true, 2, 2222222222222222, 2.2, 2222222222222222222222222222, "2" ]
        """;
    var builder = new ArrayBuilder();
    JsonReader.parse(text, builder);
    assertEquals(
        Arrays.asList(null, false, true, 2, 2222222222222222L, 2.2,
            new BigInteger("2222222222222222222222222222"),  "2"),
        builder.toList());
  }

  @Test
  public void testSimpleArrayParsingWithObjectToList() {
    var text = """
        [ "foo", 42, { "bar": 66.6 } ]
        """;
    var builder = BuilderConfig.defaults()
        .withTransformListOp(List::copyOf)
        .newArrayBuilder();
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
    var builder2 = new ObjectBuilder();
    builder.accept(builder2);
    assertEquals(
        Map.of("firstName", "Bob", "age", 21),
        builder2.toMap());
  }

  @Test
  public void testSimpleJSonObjectToSortedMap() {
    var builder = new ObjectBuilder()
        .add("firstName", "Bob")
        .add("age", 21)
        .add("weight", 70.2)
        .add("spouse", null)
        .add("children", true);
    var builder2 = new BuilderConfig(TreeMap::new, ArrayList::new).newObjectBuilder();
    builder.accept(builder2);
    assertEquals(
        List.of("age", "children", "firstName", "spouse", "weight"),
        new ArrayList<>(builder2.toMap().keySet()));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void readUnOrderedImmutable() {
    var json = """
        {
          "name": "James Joyce",
          "age": 38,
          "books": [
            "Finnegans Wake"
          ]
        }
        """;
    var config = new BuilderConfig(LinkedHashMap::new, ArrayList::new)
        .withTransformOps(Map::copyOf, List::copyOf);
    var author = (Map<String, Object>) JsonReader.parse(json, config.newObjectBuilder());
    var books = (List<String>) author.get("books");
    assertThrows(UnsupportedOperationException.class, () -> author.put("name", "Jane Austin"));
    assertThrows(UnsupportedOperationException.class, () -> books.add("foo"));
    assertEquals(Set.of("name", "age", "books"), author.keySet());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void readOrderedUnmodifiable() {
    var json = """
        {
          "name": "James Joyce",
          "age": 38,
          "books": [
            "Finnegans Wake"
          ]
        }
        """;
    var config = new BuilderConfig(LinkedHashMap::new, ArrayList::new)
        .withTransformOps(Collections::unmodifiableMap, Collections::unmodifiableList);
    var author = (Map<String, Object>) JsonReader.parse(json, config.newObjectBuilder());
    var books = (List<String>) author.get("books");
    assertThrows(UnsupportedOperationException.class, () -> author.put("name", "Jane Austin"));
    assertThrows(UnsupportedOperationException.class, () -> books.add("foo"));
    assertEquals(List.of("name", "age", "books"), new ArrayList<>(author.keySet()));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testSimpleStreamOfMap() {
    var text = """
        [ { "x": 4, "y": 7 }, { "x": 14, "y": 71 } ]
        """;
    var visitor = new ArrayVisitor() {
      @Override
      public VisitorMode visitStartArray() {
        return PULL_INSIDE;
      }

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
      @Override
      public Object visitEndArray() {
        return null;
      }
    };
    var result = (List<Map<String, Integer>>) JsonReader.parse(text, visitor);
    assertEquals(List.of(Map.of("x", 4, "y", 7)), result);
  }

  @Test
  public void testParseAsStreamAndDelegate() {
    var text = """
        {
          "dataset": "kanga-3",
          "data": [ 34, 1, 64, 1 ]
        }
        """;
    var visitor = BuilderConfig.defaults().newObjectBuilder(new ObjectVisitor() {
      @Override
      public VisitorMode visitStartObject() {
        return PULL;
      }
      @Override
      public ObjectVisitor visitMemberObject(String name) {
        return null;
      }
      @Override
      public ArrayVisitor visitMemberArray(String name) {
        return new ArrayVisitor() {
          @Override
          public VisitorMode visitStartArray() {
            return PULL_INSIDE;
          }
          @Override
          public Object visitStream(Stream<Object> stream) {
            return stream.mapToInt(o -> (int) o).sum();
          }
          @Override
          public Object visitValue(JsonValue value) {
            return value.intValue();
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
          public Object visitEndArray() {
            return null;
          }
        };
      }
      @Override
      public Object visitMemberValue(String name, JsonValue value) {
        return value.asObject();
      }
      @Override
      public Object visitEndObject() {
        return null;
      }
    });
    var map = JsonReader.parse(text, visitor);
    assertEquals(Map.of("dataset", "kanga-3", "data", 100), map);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testParseAsStreamOfObject() {
    var text = """
        [ { "x": 4, "y": 7 }, { "x": 14, "y": 71 } ]
        """;

    var stream = JsonReader.stream(text, new ArrayVisitor() {
      @Override
      public VisitorMode visitStartArray() {
        return PULL;
      }
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