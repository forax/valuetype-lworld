package fr.umlv.jsonapi.filter;

import static java.util.Objects.requireNonNull;

import fr.umlv.jsonapi.ArrayVisitor;
import fr.umlv.jsonapi.ObjectVisitor;
import fr.umlv.jsonapi.JsonValue;
import java.util.function.UnaryOperator;

public class RenamerObjectVisitor implements ObjectVisitor {
  private final ObjectVisitor delegate;
  private final UnaryOperator<String> renamer;

  public RenamerObjectVisitor(ObjectVisitor delegate, UnaryOperator<String> renamer) {
    this.delegate = requireNonNull(delegate);
    this.renamer = requireNonNull(renamer);
  }

  @Override
  public ObjectVisitor visitMemberObject(String name) {
    return new RenamerObjectVisitor(delegate.visitMemberObject(renamer.apply(name)), renamer);
  }

  @Override
  public ArrayVisitor visitMemberArray(String name) {
    return new RenamerArrayVisitor(delegate.visitMemberArray(renamer.apply(name)), renamer);
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