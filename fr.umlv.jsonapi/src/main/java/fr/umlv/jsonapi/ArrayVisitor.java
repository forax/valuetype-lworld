package fr.umlv.jsonapi;

public interface ArrayVisitor {
  ObjectVisitor visitObject();
  ArrayVisitor visitArray();
  Object visitValue(JsonValue value);
  Object visitEndArray(Object result);
}