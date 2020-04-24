package fr.umlv.jsonapi;

public interface ArrayVisitor {
  ObjectVisitor visitObject();
  ArrayVisitor visitArray();
  void visitValue(JsonValue value);
  Object visitEndArray();
}