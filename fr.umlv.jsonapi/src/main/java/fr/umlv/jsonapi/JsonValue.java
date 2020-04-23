package fr.umlv.jsonapi;

public /*sealed*/ interface JsonValue extends JsonElement {
  // permits JsonText, JsonNumber, JsonConstant

  Object value();
}
