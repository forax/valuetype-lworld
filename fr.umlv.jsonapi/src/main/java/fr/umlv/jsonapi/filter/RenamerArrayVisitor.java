package fr.umlv.jsonapi.filter;

import static java.util.Objects.requireNonNull;

import fr.umlv.jsonapi.ArrayVisitor;
import fr.umlv.jsonapi.ObjectVisitor;
import fr.umlv.jsonapi.JsonValue;
import java.util.function.UnaryOperator;

public class RenamerArrayVisitor implements ArrayVisitor {
  private final ArrayVisitor delegate;
  private final UnaryOperator<String> renamer;

  public RenamerArrayVisitor(ArrayVisitor delegate, UnaryOperator<String> renamer) {
    this.delegate = requireNonNull(delegate);
    this.renamer = requireNonNull(renamer);
  }

  @Override
  public ObjectVisitor visitObject() {
    return new RenamerObjectVisitor(delegate.visitObject(), renamer);
  }

  @Override
  public ArrayVisitor visitArray() {
    return new RenamerArrayVisitor(delegate.visitArray(), renamer);
  }

  @Override
  public void visitValue(JsonValue value) {
    delegate.visitValue(value);
  }

  @Override
  public Object visitEndArray() {
    return delegate.visitEndArray();
  }
}