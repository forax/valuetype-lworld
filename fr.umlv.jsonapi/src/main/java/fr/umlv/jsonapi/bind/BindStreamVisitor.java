package fr.umlv.jsonapi.bind;

import fr.umlv.jsonapi.ArrayVisitor;
import fr.umlv.jsonapi.BuilderConfig;
import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.ObjectVisitor;
import fr.umlv.jsonapi.StreamVisitor;
import fr.umlv.jsonapi.bind.Specs.StreamSpec;
import java.util.function.Consumer;
import java.util.stream.Stream;

final class BindStreamVisitor implements StreamVisitor {
  private final StreamSpec spec;
  private final BuilderConfig config;

  private final Consumer<Object> postOp;

  BindStreamVisitor(StreamSpec spec, BuilderConfig config, Consumer<Object> postOp) {
    this.spec = spec;
    this.config = config;
    this.postOp = postOp;
  }

  BindStreamVisitor(StreamSpec spec, BuilderConfig config) {
    this(spec, config, __ -> { /* empty */ });
  }

  @Override
  public ObjectVisitor visitObject() {
    return spec.newObjectFrom(config);
  }

  @Override
  public ArrayVisitor visitArray() {
    return spec.newArrayFrom(config);
  }

  @Override
  public Object visitStream(Stream<Object> stream) {
    return spec.aggregator().apply(stream);
  }

  @Override
  public Object visitValue(JsonValue value) {
    return Specs.convert(spec, value).asObject();
  }

  @Override
  public Object visitEndArray(Object result) {
    postOp.accept(result);
    return result;
  }
}
