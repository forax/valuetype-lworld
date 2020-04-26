package fr.umlv.jsonapi.bind;

import fr.umlv.jsonapi.ArrayVisitor;
import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.ObjectBuilder;
import fr.umlv.jsonapi.ObjectVisitor;
import fr.umlv.jsonapi.bind.Binder.ObjectSpec;
import java.util.Map;
import java.util.function.Consumer;

final class BindObjectVisitor implements ObjectVisitor {
  private final ObjectSpec spec;
  private final ObjectBuilder objectBuilder;
  private final Consumer<Object> postOp;

  BindObjectVisitor(ObjectSpec spec, ObjectBuilder objectBuilder, Consumer<Object> postOp) {
    this.spec = spec;
    this.objectBuilder = objectBuilder;
    this.postOp = postOp;
  }

  BindObjectVisitor(ObjectSpec spec, ObjectBuilder objectBuilder) {
    this(spec, objectBuilder, __ -> { /* empty */ });
  }

  @Override
  public ObjectVisitor visitMemberObject(String name) {
    return spec.newMemberObjectFrom(name, objectBuilder);
  }

  @Override
  public ArrayVisitor visitMemberArray(String name) {
    return spec.newMemberArrayFrom(name, objectBuilder);
  }

  @Override
  public void visitMemberValue(String name, JsonValue value) {
    objectBuilder.visitMemberValue(name, Binder.convert(spec, name, value));
  }

  @Override
  public Map<String, Object> visitEndObject() {
    var map = objectBuilder.visitEndObject();
    postOp.accept(map);
    return map;
  }
}
