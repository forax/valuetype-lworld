package fr.umlv.jsonapi;

public final class JsonDocument implements JsonVisitor {
  private Object element;

  @Override
  public JsonObjectVisitor visitObject() {
    var object = new JsonObject();
    element = object;
    return object;
  }

  @Override
  public JsonArrayVisitor visitArray() {
    var array = new JsonArray<>();
    element = array;
    return array;
  }

  public Object toElement() {
    return element;
  }
}