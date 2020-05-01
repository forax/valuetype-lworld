package fr.umlv.jsonapi.internal;

import static java.util.Objects.requireNonNull;

import fr.umlv.jsonapi.ArrayVisitor;
import fr.umlv.jsonapi.ObjectVisitor;
import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.VisitorMode;
import java.util.function.Predicate;

public final class FilterObjectVisitor implements ObjectVisitor {
  private final ObjectVisitor delegate;
  private final Predicate<? super String> predicate;

  public FilterObjectVisitor(ObjectVisitor delegate, Predicate<? super String> predicate) {
    this.delegate = requireNonNull(delegate);
    this.predicate = requireNonNull(predicate);
  }

  @Override
  public VisitorMode visitStartObject() {
    return delegate.visitStartObject();
  }

  @Override
  public ObjectVisitor visitMemberObject(String name) {
    if (!predicate.test(name)) {
      return null;
    }
    var objectVisitor = this.delegate.visitMemberObject(name);
    return new FilterObjectVisitor(objectVisitor, predicate);
  }

  @Override
  public ArrayVisitor visitMemberArray(String name) {
    if (!predicate.test(name)) {
      return null;
    }
    return new FilterArrayVisitor(delegate.visitMemberArray(name), predicate);
  }

  @Override
  public Object visitMemberValue(String name, JsonValue value) {
    if (predicate.test(name)) {
      return delegate.visitMemberValue(name, value);
    }
    return null;
  }

  @Override
  public Object visitEndObject() {
    return delegate.visitEndObject();
  }
}