package fr.umlv.jsonapi.filter;

import static java.util.Objects.requireNonNull;

import fr.umlv.jsonapi.ArrayVisitor;
import fr.umlv.jsonapi.ObjectVisitor;
import fr.umlv.jsonapi.JsonValue;
import java.util.function.Predicate;

public class FilterObjectVisitor implements ObjectVisitor {
  private final ObjectVisitor delegate;
  private final Predicate<? super String> predicate;

  public FilterObjectVisitor(ObjectVisitor delegate, Predicate<? super String> predicate) {
    this.delegate = requireNonNull(delegate);
    this.predicate = requireNonNull(predicate);
  }

  @Override
  public ObjectVisitor visitMemberObject(String name) {
    if (!predicate.test(name)) {
      return null;
    }
    return new FilterObjectVisitor(delegate.visitMemberObject(name), predicate);
  }

  @Override
  public ArrayVisitor visitMemberArray(String name) {
    if (!predicate.test(name)) {
      return null;
    }
    return new FilterArrayVisitor(delegate.visitMemberArray(name), predicate);
  }

  @Override
  public void visitMemberValue(String name, JsonValue value) {
    if (predicate.test(name)) {
      delegate.visitMemberValue(name, value);
    }
  }

  @Override
  public Object visitEndObject() {
    return delegate.visitEndObject();
  }
}