package fr.umlv.jsonapi;

import java.util.stream.Stream;

public interface StreamVisitor extends ArrayVisitor {
  Object visitEndArray(Stream<Object> stream);

  @Override
  Object visitValue(JsonValue value);

  @Override
  @SuppressWarnings("unchecked")
  default Object visitEndArray(Object result) {
    return visitEndArray((Stream<Object>) result);
  }
}
