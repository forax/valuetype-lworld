package fr.umlv.jsonapi.bind;

import fr.umlv.jsonapi.ArrayVisitor;
import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.ObjectVisitor;
import fr.umlv.jsonapi.VisitorMode;
import fr.umlv.jsonapi.bind.Spec.ObjectLayout;
import fr.umlv.jsonapi.bind.Specs.ObjectSpec;
import java.util.function.Consumer;

final class BindObjectVisitor implements ObjectVisitor {
  private final ObjectSpec spec;
  private Object builder;
  private final Consumer<Object> postOp;

  BindObjectVisitor(ObjectSpec spec, Consumer<Object> postOp) {
    this.spec = spec;
    this.postOp = postOp;
    this.builder = spec.objectLayout().newBuilder();
  }

  BindObjectVisitor(ObjectSpec spec) {
    this(spec, __ -> { /* empty */ });
  }

  @Override
  public VisitorMode visitStartObject() {
    return VisitorMode.PUSH;
  }

  @Override
  public ObjectVisitor visitMemberObject(String name) {
    var filter = spec.filter();
    if (filter != null && !filter.test(name)) {
      return null;
    }
    @SuppressWarnings("unchecked")
    var objectLayout = (ObjectLayout<Object>) spec.objectLayout();
    return spec.newMemberObject(name, o -> builder = objectLayout.addObject(builder, name, o));
  }

  @Override
  public ArrayVisitor visitMemberArray(String name) {
    var filter = spec.filter();
    if (filter != null && !filter.test(name)) {
      return null;
    }
    @SuppressWarnings("unchecked")
    var objectLayout = (ObjectLayout<Object>) spec.objectLayout();
    return spec.newMemberArray(name, a -> builder = objectLayout.addArray(builder, name, a));
  }

  @Override
  public Void visitMemberValue(String name, JsonValue value) {
    var filter = spec.filter();
    if (filter != null && !filter.test(name)) {
      return null;
    }
    @SuppressWarnings("unchecked")
    var objectLayout = (ObjectLayout<Object>) spec.objectLayout();
    var converted = Specs.convert(spec, name, value);
    builder = objectLayout.addValue(builder, name, converted);
    return null;
  }

  @Override
  public Object visitEndObject() {
    @SuppressWarnings("unchecked")
    var objectLayout = (ObjectLayout<Object>) spec.objectLayout();
    var instance = objectLayout.build(builder);
    builder = null;
    postOp.accept(instance);
    return instance;
  }
}
