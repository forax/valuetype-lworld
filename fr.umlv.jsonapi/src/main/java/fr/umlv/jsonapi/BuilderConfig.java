package fr.umlv.jsonapi;

import static java.util.Objects.requireNonNull;
import static java.util.function.UnaryOperator.identity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public record BuilderConfig(Supplier<? extends Map<String, Object>> mapSupplier,
                            UnaryOperator<Map<String, Object>> transformMapOp,
                            Supplier<? extends List<Object>> listSupplier,
                            UnaryOperator<List<Object>> transformListOp) {

  static final BuilderConfig DEFAULT = new BuilderConfig();

  public BuilderConfig {
    requireNonNull(mapSupplier, "mapSupplier");
    requireNonNull(mapSupplier, "transformMapOp");
    requireNonNull(mapSupplier, "listSupplier");
    requireNonNull(mapSupplier, "transformListOp");
  }

  private BuilderConfig() {
    this(HashMap::new, ArrayList::new);
  }

  public BuilderConfig(Supplier<? extends Map<String, Object>> mapSupplier,
                       Supplier<? extends List<Object>> listSupplier) {
    this(mapSupplier, identity(), listSupplier, identity());
  }

  public static BuilderConfig defaults() {
    return DEFAULT;
  }

  public BuilderConfig withTransformOps(
      UnaryOperator<Map<String, Object>> transformMapOp,
      UnaryOperator<List<Object>> transformListOp) {
    return new BuilderConfig(mapSupplier, transformMapOp, listSupplier, transformListOp);
  }
  public BuilderConfig withTransformListOp(UnaryOperator<List<Object>> transformListOp) {
    return new BuilderConfig(mapSupplier, transformMapOp, listSupplier, transformListOp);
  }
  public BuilderConfig withTransformMapOp(UnaryOperator<Map<String, Object>> transformMapOp) {
    return new BuilderConfig(mapSupplier, transformMapOp, listSupplier, transformListOp);
  }

  public ObjectBuilder newObjectBuilder() {
    return new ObjectBuilder(this);
  }
  public ArrayBuilder newArrayBuilder() {
    return new ArrayBuilder(this);
  }

  public static BuilderConfig from(ObjectBuilder builder) {
    return builder.config;   // implicit nullcheck
  }
  public static BuilderConfig from(ArrayBuilder builder) {
    return builder.config;  // implicit nullcheck
  }
}
