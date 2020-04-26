package fr.umlv.jsonapi;

import java.util.stream.Stream;

public interface StreamVisitor extends ArrayVisitor {
  Object visitStream(Stream<Object> stream);

  default Object visitValue(JsonValue value) {
    return value.asObject();
  }

  @Override
  default Object visitEndArray(Object result) {
    return result;
  }
}
