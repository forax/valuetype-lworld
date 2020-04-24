package fr.umlv.jsonapi.filter;

import static java.util.Objects.requireNonNull;

import fr.umlv.jsonapi.ArrayVisitor;
import fr.umlv.jsonapi.ObjectVisitor;
import fr.umlv.jsonapi.JsonValue;
import java.util.function.Predicate;

public class FilterArrayVisitor implements ArrayVisitor {
  private final ArrayVisitor delegate;
  private final Predicate<? super String> predicate;

  public FilterArrayVisitor(ArrayVisitor delegate, Predicate<? super String> predicate) {
    this.delegate = requireNonNull(delegate);
    this.predicate = requireNonNull(predicate);
  }

  @Override
  public ObjectVisitor visitObject() {
    return new FilterObjectVisitor(delegate.visitObject(), predicate);
  }

  @Override
  public ArrayVisitor visitArray() {
    return new FilterArrayVisitor(delegate.visitArray(), predicate);
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