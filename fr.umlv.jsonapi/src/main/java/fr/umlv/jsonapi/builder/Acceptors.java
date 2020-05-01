package fr.umlv.jsonapi.builder;

import fr.umlv.jsonapi.ArrayVisitor;
import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.ObjectVisitor;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

final class Acceptors {
  private Acceptors() {
    throw new AssertionError();
  }

  private static void acceptValue(Object element, ArrayVisitor visitor) {
    if (element == null) {
      visitor.visitValue(JsonValue.nullValue());
      return;
    }
    if (element instanceof Iterable<?> iterable) {
      var arrayVisitor = visitor.visitArray();
      if (arrayVisitor == null) {
        acceptIterable(iterable, arrayVisitor);
      }
      return;
    }
    if (element instanceof Iterator<?> iterator) {
      var arrayVisitor = visitor.visitArray();
      if (arrayVisitor != null) {
        acceptIterator(iterator, arrayVisitor);
      }
      return;
    }
    if (element instanceof Stream<?> stream) {
      var arrayVisitor = visitor.visitArray();
      if (arrayVisitor != null) {
        acceptStream(stream, arrayVisitor);
      }
      return;
    }
    if (element instanceof Map<?,?> map) {
      var objectVisitor = visitor.visitObject();
      if (objectVisitor != null) {
        acceptMap(map, objectVisitor);
      }
      return;
    }
    var value = JsonValue.fromAny(element);
    visitor.visitValue(value);
  }

  private static void acceptMember(String name, Object element, ObjectVisitor visitor) {
    if (element == null) {
      visitor.visitMemberValue(name, JsonValue.nullValue());
      return;
    }
    if (element instanceof Iterable<?> iterable) {
      var arrayVisitor = visitor.visitMemberArray(name);
      if (arrayVisitor != null) {
        acceptIterable(iterable, arrayVisitor);
      }
      return;
    }
    if (element instanceof Iterator<?> iterator) {
      var arrayVisitor = visitor.visitMemberArray(name);
      if (arrayVisitor != null) {
        acceptIterator(iterator, arrayVisitor);
      }
      return;
    }
    if (element instanceof Stream<?> stream) {
      var arrayVisitor = visitor.visitMemberArray(name);
      if (arrayVisitor != null) {
        acceptStream(stream, arrayVisitor);
      }
      return;
    }
    if (element instanceof Map<?,?> _map) {
      var objectVisitor = visitor.visitMemberObject(name);
      if (objectVisitor != null) {
        acceptMap(_map, objectVisitor);
      }
      return;
    }
    var value = JsonValue.fromAny(element);
    visitor.visitMemberValue(name, value);
  }

  static Object acceptIterable(Iterable<?> iterable, ArrayVisitor arrayVisitor) {
    arrayVisitor.visitStartArray();
    for(Object item: iterable) {
      acceptValue(item, arrayVisitor);
    }
    return arrayVisitor.visitEndArray();
  }

  private static Object acceptIterator(Iterator<?> iterator, ArrayVisitor arrayVisitor) {
    arrayVisitor.visitStartArray();
    while(iterator.hasNext()) {
      acceptValue(iterator.next(), arrayVisitor);
    }
    return arrayVisitor.visitEndArray();
  }

  private static Object acceptStream(Stream<?> stream, ArrayVisitor arrayVisitor) {
    arrayVisitor.visitStartArray();
    stream.forEach(item -> {
      acceptValue(item, arrayVisitor);
    });
    return arrayVisitor.visitEndArray();
  }

  static Object acceptMap(Map<?,?> map, ObjectVisitor objectVisitor) {
    objectVisitor.visitStartObject();
    for(var entry: map.entrySet()) {
      var name = entry.getKey().toString();
      var value = entry.getValue();
      acceptMember(name, value, objectVisitor);
    }
    return objectVisitor.visitEndObject();
  }
}
