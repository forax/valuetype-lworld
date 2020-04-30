package fr.umlv.jsonapi.bind;

import fr.umlv.jsonapi.ArrayVisitor;
import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.ObjectVisitor;
import fr.umlv.jsonapi.VisitorMode;
import fr.umlv.jsonapi.bind.Specs.ArraySpec;
import fr.umlv.jsonapi.builder.ArrayBuilder;

import java.util.List;
import java.util.function.Consumer;

final class BindArrayVisitor implements ArrayVisitor {
  private final ArraySpec spec;
  private final ArrayBuilder arrayBuilder;
  private final Consumer<Object> postOp;

  BindArrayVisitor(ArraySpec spec, ArrayBuilder arrayBuilder, Consumer<Object> postOp) {
    this.spec = spec;
    this.arrayBuilder = arrayBuilder;
    this.postOp = postOp;
  }

  BindArrayVisitor(ArraySpec spec, ArrayBuilder arrayBuilder) {
    this(spec, arrayBuilder, __ -> { /* empty */ });
  }

  @Override
  public VisitorMode mode() {
    return VisitorMode.PUSH;
  }

  @Override
  public ObjectVisitor visitObject() {
    return spec.newObjectFrom(arrayBuilder);
  }

  @Override
  public ArrayVisitor visitArray() {
    return spec.newArrayFrom(arrayBuilder);
  }

  @Override
  public Void visitValue(JsonValue value) {
    arrayBuilder.visitValue(Specs.convert(spec, value));
    return null;
  }

  @Override
  public List<Object> visitEndArray() {
    var list = arrayBuilder.visitEndArray();
    postOp.accept(list);
    return list;
  }
}
