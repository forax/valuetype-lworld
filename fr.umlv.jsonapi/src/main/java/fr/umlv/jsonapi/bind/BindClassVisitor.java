package fr.umlv.jsonapi.bind;

import fr.umlv.jsonapi.ArrayVisitor;
import fr.umlv.jsonapi.BuilderConfig;
import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.ObjectVisitor;
import fr.umlv.jsonapi.VisitorMode;
import fr.umlv.jsonapi.bind.Spec.ClassInfo;
import fr.umlv.jsonapi.bind.Specs.ClassSpec;
import java.util.function.Consumer;

final class BindClassVisitor implements ObjectVisitor {
  private final ClassSpec spec;
  private final BuilderConfig config;
  private Object builder;
  private final Consumer<Object> postOp;

  BindClassVisitor(ClassSpec spec, BuilderConfig config, Consumer<Object> postOp) {
    this.spec = spec;
    this.config = config;
    this.postOp = postOp;
    this.builder = spec.classInfo().newBuilder();
  }

  BindClassVisitor(ClassSpec spec, BuilderConfig config) {
    this(spec, config, __ -> { /* empty */ });
  }

  @Override
  public VisitorMode mode() {
    return VisitorMode.PUSH_MODE;
  }

  @Override
  public ObjectVisitor visitMemberObject(String name) {
    var filter = spec.filter();
    if (filter != null && !filter.test(name)) {
      return null;
    }
    @SuppressWarnings("unchecked")
    var classInfo = (ClassInfo<Object>) spec.classInfo();
    return spec.newMemberObject(name, config, o -> builder = classInfo.addObject(builder, name, o));
  }

  @Override
  public ArrayVisitor visitMemberArray(String name) {
    var filter = spec.filter();
    if (filter != null && !filter.test(name)) {
      return null;
    }
    @SuppressWarnings("unchecked")
    var classInfo = (ClassInfo<Object>) spec.classInfo();
    return spec.newMemberArray(name, config, a -> builder = classInfo.addArray(builder, name, a));
  }

  @Override
  public Void visitMemberValue(String name, JsonValue value) {
    var filter = spec.filter();
    if (filter != null && !filter.test(name)) {
      return null;
    }
    @SuppressWarnings("unchecked")
    var classInfo = (ClassInfo<Object>) spec.classInfo();
    var converted = Specs.convert(spec, name, value);
    builder = classInfo.addValue(builder, name, converted);
    return null;
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
