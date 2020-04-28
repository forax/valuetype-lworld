package fr.umlv.jsonapi.filter;

import static java.util.Objects.requireNonNull;

import fr.umlv.jsonapi.ArrayVisitor;
import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.ObjectVisitor;
import fr.umlv.jsonapi.StreamVisitor;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class FilterStreamVisitor implements StreamVisitor {
  private final StreamVisitor delegate;
  private final Predicate<? super String> predicate;

  public FilterStreamVisitor(StreamVisitor delegate, Predicate<? super String> predicate) {
    this.delegate = requireNonNull(delegate);
    this.predicate = requireNonNull(predicate);
  }

  @Override
  public Object visitEndArray(Stream<Object> stream) {
    requireNonNull(stream);
    return delegate.visitEndArray(stream);
  }

  @Override
  public ObjectVisitor visitObject() {
    return new FilterObjectVisitor(delegate.visitObject(), predicate);
  }

  @Override
  public ArrayVisitor visitArray() {
    var arrayVisitor = delegate.visitArray();
    if (arrayVisitor instanceof StreamVisitor streamVisitor) {
      return new FilterStreamVisitor(streamVisitor, predicate);
    }
    return new FilterArrayVisitor(arrayVisitor, predicate);
  }

  @Override
  public Object visitValue(JsonValue value) {
    return delegate.visitValue(value);
  }
}