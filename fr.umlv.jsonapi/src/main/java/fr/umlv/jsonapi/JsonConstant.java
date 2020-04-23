package fr.umlv.jsonapi;

public enum JsonConstant implements JsonValue {
  TRUE(true, "true"),
  FALSE(false, "false"),
  NULL(null, "null");

  private final Boolean value;
  private final String text;

  JsonConstant(Boolean value, String text) {
    this.value = value;
    this.text = text;
  }

  @Override
  public String toString() {
    return text;
  }

  @Override
  public Boolean value() {
    return value;
  }

  public boolean fitsInBoolean() {
    return this != NULL;
  }

  public boolean convertToBoolean() {
    var value = this.value;
    if (value == null) {
      throw new IllegalStateException("this is NULL");
    }
    return value;
  }
}
