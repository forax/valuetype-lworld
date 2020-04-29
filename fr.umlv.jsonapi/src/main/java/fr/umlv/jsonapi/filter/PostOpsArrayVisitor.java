package fr.umlv.jsonapi.filter;

import static java.util.Objects.requireNonNull;

import fr.umlv.jsonapi.ArrayVisitor;
import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.ObjectVisitor;
import fr.umlv.jsonapi.VisitorMode;
import java.util.function.Consumer;

public class PostOpsArrayVisitor<E> implements ArrayVisitor {
  private final ArrayVisitor delegate;
  private final Consumer<Object> postOp;

  @SuppressWarnings("unchecked")
  public PostOpsArrayVisitor(ArrayVisitor delegate, Consumer<E> postOp) {
    this.delegate = requireNonNull(delegate);
    this.postOp = (Consumer<Object>) postOp;
  }

  @Override
  public VisitorMode mode() {
    return delegate.mode();
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
    postOp.accept(result);
    return result;
  }
}
