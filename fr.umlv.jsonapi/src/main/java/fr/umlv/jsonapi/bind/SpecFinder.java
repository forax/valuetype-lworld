package fr.umlv.jsonapi.bind;

import fr.umlv.jsonapi.bind.Spec.Converter;
import fr.umlv.jsonapi.bind.Spec.ObjectLayout;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A spec finder is a function that associate for a Java class a corresponding {@link Spec}.
 * It can return {code Optional.empty()} to indicate that the finder doesn't now
 * how to turn a specific type to a spec.
 *
 * <pre>
 * record Authorized() { }
 * record Unauthorized() { }
 * Binder binder = Binder.noDefaults();  // no finder registered !
 * SpecFinder recordFinder = SpecFinder.newRecordFinder(lookup(), binder::spec);
 * Set&lt;Class&lt;?&gt;&gt; restrictedSet = Set.of(Authorized.class);
 * // register the finder filtered !
 * binder.register(recordFinder.filter(restrictedSet::contains));
 * </pre>
 *
 * @see Binder#register(SpecFinder)
 * @see Binder#spec(Type)
 */
@FunctionalInterface
public interface SpecFinder {

  /**
   * Returns the spec corresponding to the class passed as argument
   * if the finder knows how to deal with the class.
   *
   * @param type a Java class
   * @return a spec describing the class either as a
   *         {@link Spec#newTypedObject(String, ObjectLayout) JSON object} or as a
   *         {@link Spec#newTypedValue(String, Converter) JSON value} or
   *         {@code Optional.empty} otherwise.
   */
  Optional<Spec> findSpec(Class<?> type);

  /**
   * Higher order function that allows to restrict the classes that are available
   * to be described as a spec.
   * This allow to implement security by only allowing some specific classes
   * to be read/write from/to JSON.
   *
   * @param predicate a function that return true for a class if the current finder is
   *                  allowed to try to find the spec of that class
   * @return a new finder with the filtering in place
   */
  default SpecFinder filter(Predicate<? super Class<?>> predicate) {
    return type -> {
      if (!predicate.test(type)) {
        return Optional.empty();
      }
      return findSpec(type);
    };
  }

  /**
   * Creates a spec finder to only recognize one class and return the corresponding spec.
   *
   * @param type the class recognized
   * @param spec the corresponding spec
   * @return a spec finder able to recognize the type
   */
  static SpecFinder associate(Class<?> type, Spec spec) {
    return t -> Optional.of(t).filter(type::equals).map(__ -> spec);
  }

  /**
   * Creates a spec finder able to recognized all the types that are keys of the {@code specMap} and
   * associate them with the value of the map
   *
   * @param specMap the map containing the association between a type and a spec
   * @return a spec finder able to recognized all the types that are keys of * the {@code specMap}
   */
  static SpecFinder from(Map<? super Class<?>, ? extends Spec> specMap) {
    return type -> Optional.ofNullable(specMap.get(type));
  }

  /**
   * Creates a spec finder able to read/write any records.
   *
   * @param lookup the security context that will be used to access the record constructor
   *               and methods
   * @return a spec finder able to read/write records.
   *
   * @see Binder#Binder(Lookup)
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