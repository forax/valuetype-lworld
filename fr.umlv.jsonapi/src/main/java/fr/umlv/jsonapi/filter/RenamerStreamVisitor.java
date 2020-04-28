package fr.umlv.jsonapi.filter;

import static java.util.Objects.requireNonNull;

import fr.umlv.jsonapi.ArrayVisitor;
import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.ObjectVisitor;
import fr.umlv.jsonapi.StreamVisitor;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public final class RenamerStreamVisitor implements StreamVisitor {
  private final StreamVisitor delegate;
  private final UnaryOperator<String> renamer;

  public RenamerStreamVisitor(StreamVisitor delegate, UnaryOperator<String> renamer) {
    this.delegate = requireNonNull(delegate);
    this.renamer = requireNonNull(renamer);
  }

  @Override
  public Object visitEndArray(Stream<Object> stream) {
    requireNonNull(stream);
    return delegate.visitEndArray(stream);
  }

  @Override
  public ObjectVisitor visitObject() {
    return new RenamerObjectVisitor(delegate.visitObject(), renamer);
  }

  @Override
  public ArrayVisitor visitArray() {
    var arrayVisitor = delegate.visitArray();
    if (arrayVisitor instanceof StreamVisitor streamVisitor) {
      return new RenamerStreamVisitor(streamVisitor, renamer);
    }
    return new RenamerArrayVisitor(arrayVisitor, renamer);
  }

  @Override
  public Object visitValue(JsonValue value) {
    return delegate.visitValue(value);
  }
}