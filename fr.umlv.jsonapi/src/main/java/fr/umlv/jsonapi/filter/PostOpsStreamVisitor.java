package fr.umlv.jsonapi.filter;

import static java.util.Objects.requireNonNull;

import fr.umlv.jsonapi.ArrayVisitor;
import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.ObjectVisitor;
import fr.umlv.jsonapi.StreamVisitor;
import fr.umlv.jsonapi.VisitorMode;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class PostOpsStreamVisitor<E> implements StreamVisitor {
  private final StreamVisitor delegate;
  private final Consumer<Object> postOp;

  @SuppressWarnings("unchecked")
  public PostOpsStreamVisitor(StreamVisitor delegate, Consumer<E> postOp) {
    this.delegate = requireNonNull(delegate);
    this.postOp = (Consumer<Object>) postOp;
  }

  @Override
  public VisitorMode mode() {
    return delegate.mode();
  }

  @Override
  public Object visitStream(Stream<Object> stream) {
    var result = delegate.visitStream(stream);
    postOp.accept(result);
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
  public Void visitEndArray() {
    delegate.visitEndArray();
    return null;
  }
}
