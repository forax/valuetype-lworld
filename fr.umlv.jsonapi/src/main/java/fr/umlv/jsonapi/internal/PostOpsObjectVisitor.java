package fr.umlv.jsonapi.internal;

import static java.util.Objects.requireNonNull;

import fr.umlv.jsonapi.ArrayVisitor;
import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.ObjectVisitor;
import fr.umlv.jsonapi.VisitorMode;
import java.util.function.Consumer;

public class PostOpsObjectVisitor<E> implements ObjectVisitor {
  private final ObjectVisitor delegate;
  private final Consumer<Object> postOp;

  @SuppressWarnings("unchecked")
  public PostOpsObjectVisitor(ObjectVisitor delegate, Consumer<E> postOp) {
    this.delegate = requireNonNull(delegate);
    this.postOp = (Consumer<Object>) postOp;
  }

  @Override
  public VisitorMode visitStartObject() {
    return delegate.visitStartObject();
  }

  @Override
  public ObjectVisitor visitMemberObject(String name) {
    return delegate.visitMemberObject(name);
  }

  @Override
  public ArrayVisitor visitMemberArray(String name) {
    return delegate.visitMemberArray(name);
  }

  @Override
  public Object visitMemberValue(String name, JsonValue value) {
    return delegate.visitMemberValue(name, value);
  }

  @Override
  public Object visitEndObject() {
    var result = delegate.visitEndObject();
    postOp.accept(result);
    return result;
  }
}
