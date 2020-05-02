package fr.umlv.jsonapi.bind;

import fr.umlv.jsonapi.ArrayVisitor;
import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.ObjectVisitor;
import fr.umlv.jsonapi.VisitorMode;
import fr.umlv.jsonapi.bind.Spec.ArrayLayout;
import fr.umlv.jsonapi.bind.Specs.CollectionSpec;
import java.util.function.Consumer;

final class BindCollectionVisitor implements ArrayVisitor {
  private final CollectionSpec spec;
  private Object builder;
  private final Consumer<Object> postOp;

  BindCollectionVisitor(CollectionSpec spec, Consumer<Object> postOp) {
    this.spec = spec;
    this.postOp = postOp;
    this.builder = spec.arrayLayout().newBuilder();
  }

  BindCollectionVisitor(CollectionSpec spec) {
    this(spec, __ -> { /* empty */ });
  }

  @Override
  public VisitorMode visitStartArray() {
    return VisitorMode.PUSH;
  }

  @Override
  public ObjectVisitor visitObject() {
    @SuppressWarnings("unchecked")
    var arrayLayout = (ArrayLayout<Object>) spec.arrayLayout();
    return spec.newObject(o -> builder = arrayLayout.addObject(builder, o));
  }

  @Override
  public ArrayVisitor visitArray() {
    @SuppressWarnings("unchecked")
    var arrayLayout = (ArrayLayout<Object>) spec.arrayLayout();
    return spec.newArray(a -> builder = arrayLayout.addArray(builder, a));
  }

  @Override
  public Void visitValue(JsonValue value) {
    @SuppressWarnings("unchecked")
    var arrayLayout = (ArrayLayout<Object>) spec.arrayLayout();
    var converted = Specs.convert(spec, value);
    builder = arrayLayout.addValue(builder, converted);
    return null;
  }

  @Override
  public Object visitEndArray() {
    @SuppressWarnings("unchecked")
    var arrayLayout = (ArrayLayout<Object>) spec.arrayLayout();
    var instance = arrayLayout.build(builder);
    builder = null;
    postOp.accept(instance);
    return instance;
  }
}
