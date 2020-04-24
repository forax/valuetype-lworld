package fr.umlv.jsonapi;

public interface JsonArrayVisitor {
  JsonObjectVisitor visitObject();
  JsonArrayVisitor visitArray();
  void visitValue(JsonValue value);
  Object visitEndArray();
}