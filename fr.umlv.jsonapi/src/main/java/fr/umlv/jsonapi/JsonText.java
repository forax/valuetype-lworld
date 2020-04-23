package fr.umlv.jsonapi;

import java.util.Objects;

public final @__inline__ class JsonText implements JsonValue {
  private final String string;

  public JsonText(String string) {
    this.string = Objects.requireNonNull(string);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof JsonText text && string.equals(text.string);
  }

  @Override
  public int hashCode() {
    return string.hashCode();
  }

  @Override
  public String toString() {
    return '"' + string + '"';  //FIXME escape ?
  }

  @Override
  public String value() {
    return string;
  }
}
