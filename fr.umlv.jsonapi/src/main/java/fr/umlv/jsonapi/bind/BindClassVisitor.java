package fr.umlv.jsonapi.bind;

import fr.umlv.jsonapi.ArrayVisitor;
import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.ObjectVisitor;
import fr.umlv.jsonapi.bind.Binder.ClassInfo;
import fr.umlv.jsonapi.bind.Binder.ClassSpec;
import java.util.function.Consumer;

public final class BindClassVisitor implements ObjectVisitor {
  private final ClassSpec spec;
  private Object builder;
  private final Consumer<Object> postOp;

  BindClassVisitor(ClassSpec spec, Consumer<Object> postOp) {
    this.spec = spec;
    this.postOp = postOp;
    this.builder = spec.classInfo().newBuilder();
  }

  public BindClassVisitor(ClassSpec spec) {
    this(spec, __ -> {});
  }

  @Override
  public ObjectVisitor visitMemberObject(String name) {
    @SuppressWarnings("unchecked")
    var classInfo = (ClassInfo<Object>) spec.classInfo();
    return spec.newMemberObject(name, o -> builder = classInfo.addObject(builder, name, o));
  }

  @Override
  public ArrayVisitor visitMemberArray(String name) {
    @SuppressWarnings("unchecked")
    var classInfo = (ClassInfo<Object>) spec.classInfo();
    return spec.newMemberArray(name, a -> builder = classInfo.addArray(builder, name, a));
  }

  @Override
  public void visitMemberValue(String name, JsonValue value) {
    @SuppressWarnings("unchecked")
    var classInfo = (ClassInfo<Object>) spec.classInfo();
    builder = classInfo.addValue(builder, name, Binder.convert(spec, name, value));
  }

  @Override
  public Object visitEndObject() {
    @SuppressWarnings("unchecked")
    var classInfo = (ClassInfo<Object>) spec.classInfo();
    var instance = classInfo.build(builder);
    builder = null;
    postOp.accept(instance);
    return instance;
  }
}
