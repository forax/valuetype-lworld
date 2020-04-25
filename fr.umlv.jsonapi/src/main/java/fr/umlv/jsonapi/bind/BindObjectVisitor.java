package fr.umlv.jsonapi.bind;

import fr.umlv.jsonapi.ArrayVisitor;
import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.ObjectBuilder;
import fr.umlv.jsonapi.ObjectVisitor;
import fr.umlv.jsonapi.bind.Binder.ObjectSpec;
import java.util.Map;
import java.util.function.Consumer;

public final class BindObjectVisitor implements ObjectVisitor {
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

  public Map<String, Object> toMap() {
    return objectBuilder.toMap();
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
  public Object visitEndObject() {
    var array = objectBuilder.visitEndObject();
    postOp.accept(array);
    return array;
  }
}
