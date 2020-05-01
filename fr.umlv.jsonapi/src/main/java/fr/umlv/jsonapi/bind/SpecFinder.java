package fr.umlv.jsonapi.bind;

import fr.umlv.jsonapi.builder.BuilderConfig;
import java.io.Reader;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 *
 *
 * <pre>
 * record Authorized() { }
 * record Unauthorized() { }
 * Binder binder = Binder.noDefaults();  // no finder registered !
 * SpecFinder recordFinder = binder.newRecordSpecFinder(lookup());
 * Set&lt;Class&lt;?&gt;&gt; restrictedSet = Set.of(Authorized.class);
 * // register the finder filtered !
 * binder.register(recordFinder.filter(restrictedSet::contains));
 * </pre>
 */
@FunctionalInterface
public interface SpecFinder {
  Optional<Spec> findSpec(Class<?> type);

  default SpecFinder filter(Predicate<? super Class<?>> predicate) {
    return type -> {
      if (!predicate.test(type)) {
        return Optional.empty();
      }
      return findSpec(type);
    };
  }

  static SpecFinder from(Map<? super Class<?>, ? extends Spec> specMap) {
    return type -> Optional.ofNullable(specMap.get(type));
  }
  static SpecFinder associate(Class<?> type, Spec spec) {
    return t -> Optional.of(t).filter(type::equals).map(__ -> spec);
  }

  /**
   * Creates a spec finder able to read/write any records.
   *
   * @param lookup the security context that will be used to load the class necessary when
   *               {@link #read(Reader, Spec, BuilderConfig) reading} a JSON fragment.
   * @return a spec finder able to read/write records.
   *
   * @see Binder#register(SpecFinder)
   */
  static SpecFinder newRecordFinder(Lookup lookup, Function<? super Type, ? extends Spec> downwardFinder) {
    return SpecFinders.newRecordFinder(lookup, downwardFinder);
  }

  /**
   * Creates a spec finder that will returns
   * {@link fr.umlv.jsonapi.JsonValue#fromOpaque(Object) opaque} value spec for any types.
   *
   * This spec finder should be the last registered, any finder registered later
   * will never be called.
   *
   * @return a spec finder that consider any type as an opaque type.
   *
   * @see Binder#register(SpecFinder)
   */
  static SpecFinder newAnyTypesAsStringFinder() {
    return SpecFinders.newAnyTypesAsStringFinder();
  }
}