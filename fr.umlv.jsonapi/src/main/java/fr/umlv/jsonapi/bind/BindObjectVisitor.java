package fr.umlv.jsonapi.bind;

import fr.umlv.jsonapi.ArrayVisitor;
import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.ObjectVisitor;
import fr.umlv.jsonapi.VisitorMode;
import fr.umlv.jsonapi.bind.Specs.ObjectSpec;
import fr.umlv.jsonapi.builder.ObjectBuilder;

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
  public VisitorMode mode() {
    return VisitorMode.PUSH;
  }

  @Override
  public ObjectVisitor visitMemberObject(String name) {
    var filter = spec.filter();
    if (filter != null && !filter.test(name)) {
      return null;
    }
    return spec.newMemberObjectFrom(name, objectBuilder);
  }

  @Override
  public ArrayVisitor visitMemberArray(String name) {
    var filter = spec.filter();
    if (filter != null && !filter.test(name)) {
      return null;
    }
    return spec.newMemberArrayFrom(name, objectBuilder);
  }

  @Override
  public Void visitMemberValue(String name, JsonValue value) {
    var filter = spec.filter();
    if (filter != null && !filter.test(name)) {
      return null;
    }
    objectBuilder.visitMemberValue(name, Specs.convert(spec, name, value));
    return null;
  }

  @Override
  public Map<String, Object> visitEndObject() {
    var map = objectBuilder.visitEndObject();
    postOp.accept(map);
    return map;
  }
}
