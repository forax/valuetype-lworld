package fr.umlv.jsonapi.builder;

import fr.umlv.jsonapi.ArrayVisitor;
import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.ObjectVisitor;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

final class Replays {
  private Replays() {
    throw new AssertionError();
  }

  private static void replayValue(Object element, ArrayVisitor visitor) {
    if (element == null) {
      visitor.visitValue(JsonValue.nullValue());
      return;
    }
    if (element instanceof Iterable<?> iterable) {
      var arrayVisitor = visitor.visitArray();
      if (arrayVisitor == null) {
        replayIterable(iterable, arrayVisitor);
      }
      return;
    }
    if (element instanceof Iterator<?> iterator) {
      var arrayVisitor = visitor.visitArray();
      if (arrayVisitor != null) {
        replayIterator(iterator, arrayVisitor);
      }
      return;
    }
    if (element instanceof Stream<?> stream) {
      var arrayVisitor = visitor.visitArray();
      if (arrayVisitor != null) {
        replayStream(stream, arrayVisitor);
      }
      return;
    }
    if (element instanceof Map<?,?> map) {
      var objectVisitor = visitor.visitObject();
      if (objectVisitor != null) {
        replayMap(map, objectVisitor);
      }
      return;
    }
    var value = JsonValue.fromAny(element);
    visitor.visitValue(value);
  }

  private static void replayMember(String name, Object element, ObjectVisitor visitor) {
    if (element == null) {
      visitor.visitMemberValue(name, JsonValue.nullValue());
      return;
    }
    if (element instanceof Iterable<?> iterable) {
      var arrayVisitor = visitor.visitMemberArray(name);
      if (arrayVisitor != null) {
        replayIterable(iterable, arrayVisitor);
      }
      return;
    }
    if (element instanceof Iterator<?> iterator) {
      var arrayVisitor = visitor.visitMemberArray(name);
      if (arrayVisitor != null) {
        replayIterator(iterator, arrayVisitor);
      }
      return;
    }
    if (element instanceof Stream<?> stream) {
      var arrayVisitor = visitor.visitMemberArray(name);
      if (arrayVisitor != null) {
        replayStream(stream, arrayVisitor);
      }
      return;
    }
    if (element instanceof Map<?,?> _map) {
      var objectVisitor = visitor.visitMemberObject(name);
      if (objectVisitor != null) {
        replayMap(_map, objectVisitor);
      }
      return;
    }
    var value = JsonValue.fromAny(element);
    visitor.visitMemberValue(name, value);
  }

  static Object replayIterable(Iterable<?> iterable, ArrayVisitor arrayVisitor) {
    arrayVisitor.visitStartArray();
    for(Object item: iterable) {
      replayValue(item, arrayVisitor);
    }
    return arrayVisitor.visitEndArray();
  }

  private static Object replayIterator(Iterator<?> iterator, ArrayVisitor arrayVisitor) {
    arrayVisitor.visitStartArray();
    while(iterator.hasNext()) {
      replayValue(iterator.next(), arrayVisitor);
    }
    return arrayVisitor.visitEndArray();
  }

  private static Object replayStream(Stream<?> stream, ArrayVisitor arrayVisitor) {
    arrayVisitor.visitStartArray();
    stream.forEach(item -> {
      replayValue(item, arrayVisitor);
    });
    return arrayVisitor.visitEndArray();
  }

  static Object replayMap(Map<?,?> map, ObjectVisitor objectVisitor) {
    objectVisitor.visitStartObject();
    for(var entry: map.entrySet()) {
      var name = entry.getKey().toString();
      var value = entry.getValue();
      replayMember(name, value, objectVisitor);
    }
    return objectVisitor.visitEndObject();
  }
}
