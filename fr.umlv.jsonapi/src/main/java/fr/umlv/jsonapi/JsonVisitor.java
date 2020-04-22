package fr.umlv.jsonapi;

public interface JsonVisitor {
  JsonObjectVisitor visitObject();
  JsonArrayVisitor visitArray();
}
