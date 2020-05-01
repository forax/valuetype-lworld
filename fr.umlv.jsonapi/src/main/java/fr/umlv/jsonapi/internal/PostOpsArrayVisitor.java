package fr.umlv.jsonapi.internal;

import static fr.umlv.jsonapi.VisitorMode.PULL_INSIDE;
import static java.util.Objects.requireNonNull;

import fr.umlv.jsonapi.ArrayVisitor;
import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.ObjectVisitor;
import fr.umlv.jsonapi.VisitorMode;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class PostOpsArrayVisitor<E> implements ArrayVisitor {
  private final ArrayVisitor delegate;
  private final Consumer<Object> postOp;

  @SuppressWarnings("unchecked")
  public PostOpsArrayVisitor(ArrayVisitor delegate, Consumer<E> postOp) {
    this.delegate = requireNonNull(delegate);
    this.postOp = (Consumer<Object>) postOp;
  }

  @Override
  public VisitorMode visitStartArray() {
    return delegate.visitStartArray();
  }

  @Override
  public Object visitStream(Stream<Object> stream) {
    var result = delegate.visitStream(stream);
    if (delegate.visitStartArray() == PULL_INSIDE) {
      postOp.accept(result);
    }
    return result;
  }

  @Override
  public ObjectVisitor visitObject() {
    return delegate.visitObject();
  }

  @Override
  public ArrayVisitor visitArray() {
    return delegate.visitArray();
  }

  @Override
  public Object visitValue(JsonValue value) {
    return delegate.visitValue(value);
  }

  @Override
  public Object visitEndArray() {
    var result = delegate.visitEndArray();
    if (delegate.visitStartArray() != PULL_INSIDE) {
      postOp.accept(result);
    }
    return result;
  }
}
