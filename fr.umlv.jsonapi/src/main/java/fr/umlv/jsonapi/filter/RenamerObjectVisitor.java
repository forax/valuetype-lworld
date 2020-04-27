package fr.umlv.jsonapi.filter;

import static java.util.Objects.requireNonNull;

import fr.umlv.jsonapi.ArrayVisitor;
import fr.umlv.jsonapi.ObjectVisitor;
import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.StreamVisitor;
import java.util.function.UnaryOperator;

public final class RenamerObjectVisitor implements ObjectVisitor {
  private final ObjectVisitor delegate;
  private final UnaryOperator<String> renamer;

  public RenamerObjectVisitor(ObjectVisitor delegate, UnaryOperator<String> renamer) {
    this.delegate = requireNonNull(delegate);
    this.renamer = requireNonNull(renamer);
  }

  @Override
  public ObjectVisitor visitMemberObject(String name) {
    var objectVisitor = delegate.visitMemberObject(renamer.apply(name));
    return new RenamerObjectVisitor(objectVisitor, renamer);
  }

  @Override
  public ArrayVisitor visitMemberArray(String name) {
    var arrayVisitor = delegate.visitMemberArray(renamer.apply(name));
    if (arrayVisitor instanceof StreamVisitor streamVisitor) {
      return new RenamerStreamVisitor(streamVisitor, renamer);
    }
    return new RenamerArrayVisitor(arrayVisitor, renamer);
  }

  @Override
  public void visitMemberValue(String name, JsonValue value) {
    delegate.visitMemberValue(renamer.apply(name), value);
  }

  @Override
  public Object visitEndObject() {
    return delegate.visitEndObject();
  }
}