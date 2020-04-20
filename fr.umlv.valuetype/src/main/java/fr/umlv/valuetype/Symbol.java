package fr.umlv.valuetype;

import java.util.Objects;

@__inline__
public /*inline*/ class Symbol {
  private final String name;

  private Symbol(String name) {
    this.name = name;
  }

  public static Symbol of(String name) {
    Objects.requireNonNull(name);
    return new Symbol(name);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Symbol)) {
      return false;
    }
    var symbol = (Symbol) obj;
    return name.equals(symbol.name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public String toString() {
    return name;
  }
}
