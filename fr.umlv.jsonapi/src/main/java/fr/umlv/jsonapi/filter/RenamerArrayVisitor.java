package fr.umlv.jsonapi.filter;

import static java.util.Objects.requireNonNull;

import fr.umlv.jsonapi.ArrayVisitor;
import fr.umlv.jsonapi.ObjectVisitor;
import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.VisitorMode;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public final class RenamerArrayVisitor implements ArrayVisitor {
  private final ArrayVisitor delegate;
  private final UnaryOperator<String> renamer;

  public RenamerArrayVisitor(ArrayVisitor delegate, UnaryOperator<String> renamer) {
    this.delegate = requireNonNull(delegate);
    this.renamer = requireNonNull(renamer);
  }

  @Override
  public VisitorMode mode() {
    return delegate.mode();
  }

  @Override
  public Object visitStream(Stream<Object> stream) {
    return delegate.visitStream(stream);
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
  public Object visitValue(JsonValue value) {
    return delegate.visitValue(value);
  }

  @Override
  public Object visitEndArray() {
    return delegate.visitEndArray();
  }
}