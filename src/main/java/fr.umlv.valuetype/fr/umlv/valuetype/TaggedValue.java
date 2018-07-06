package fr.umlv.valuetype;

public final __ByValue class TaggedValue {
  private final int number;
  private final Object object;
  
  private TaggedValue() {
    number = 0;
    object = null;
    throw new AssertionError();
  }
  
  private static final Object NULL = new Object();
  
  public static TaggedValue from(Object o) {
    Object object = (o == null)? NULL: o;
    var tagged = __MakeDefault TaggedValue();
    return __WithField(tagged.object, object);
  }
  
  public static TaggedValue from(int i) {
    var tagged = __MakeDefault TaggedValue();
    return __WithField(tagged.number, i);
  }
  
  public static TaggedValue zero() {
    return __MakeDefault TaggedValue();
  }
  
  public static TaggedValue one() {
    return from(1);
  }
  
  private static Error newError(String message, TaggedValue tagged1) {
    return new Error("error " + message + ' ' + tagged1);
  }
  private static Error newError(String message, TaggedValue tagged1, TaggedValue tagged2) {
    return new Error("error " + message + ' ' + tagged1 + ' ' + tagged2);
  }
  
  public boolean isInt() {
    return object == null;
  }
  public boolean isNull() {
    return object == NULL;
  }
  
  public int asInt() {
    if (object != null) {
      throw newError("cast int", this);
    }
    return number;
  }
  
  public <T> T as(Class<T> type) {
    if (object == null) {
      throw newError("cast object", this);
    }
    return (object == NULL)? null: type.cast(object);
  }
  
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof TaggedValue)) {
      return false;
    }
    TaggedValue tagged = (TaggedValue)obj;
    if (object == null) {
      if (tagged.object != null) {
        return false;
      }
      return number == tagged.number;
    }
    return object.equals(tagged.object);
  }
  
  @Override
  public int hashCode() {
    return (object == null)? number: object.hashCode();
  }
  
  @Override
  public String toString() {
    return (object == null) ? "" + number: (object == NULL)? "null": object.toString();
  }
  
  public TaggedValue increment() {
    if (object != null) {
      throw newError("increment", this);
    }
    try {
      return TaggedValue.from(Math.incrementExact(number));
    } catch(ArithmeticException e) {  // overflow
      throw newError("overflow increment", this);
    }
  }
  
  public TaggedValue decrement() {
    if (object != null) {
      throw newError("decrement", this);
    }
    try {
      return TaggedValue.from(Math.decrementExact(number));
    } catch(ArithmeticException e) {  // overflow
      throw newError("overflow decrement", this);
    }
  }
  
  public TaggedValue negate() {
    if (object != null) {
      throw newError("negate", this);
    }
    try {
      return TaggedValue.from(Math.negateExact(number));
    } catch(ArithmeticException e) {  // overflow
      throw newError("overflow negate", this);
    }
  }
  
  public TaggedValue add(TaggedValue tagged) {
    if (object != null || tagged.object != null) {
      throw newError("add", this, tagged);
    }
    try {
      return TaggedValue.from(Math.addExact(number, tagged.number));
    } catch(ArithmeticException e) {  // overflow
      throw newError("overflow add", this, tagged);
    }
  }
  
  public TaggedValue subtract(TaggedValue tagged) {
    if (object != null || tagged.object != null) {
      throw newError("subtract", this, tagged);
    }
    try {
      return TaggedValue.from(Math.subtractExact(number, tagged.number));
    } catch(ArithmeticException e) {  // overflow
      throw newError("overflow subtract", this, tagged);
    }
  }
  
  public TaggedValue multiply(TaggedValue tagged) {
    if (object != null || tagged.object != null) {
      throw newError("multiply", this, tagged);
    }
    try {
      return TaggedValue.from(Math.multiplyExact(number, tagged.number));
    } catch(ArithmeticException e) {  // overflow
      throw newError("overflow multiply", this, tagged);
    }
  }
}
