package fr.umlv.jsonapi;

public interface JsonObjectVisitor {
  JsonObjectVisitor visitMemberObject(String name);
  JsonArrayVisitor visitMemberArray(String name);
  void visitMemberValue(String name, JsonValue value);
  Object visitEndObject();
}