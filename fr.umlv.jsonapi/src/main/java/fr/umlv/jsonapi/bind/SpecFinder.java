package fr.umlv.jsonapi.bind;

import static java.lang.invoke.MethodType.methodType;
import static java.util.Objects.requireNonNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.bind.Spec.ClassInfo;

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

  static SpecFinder from(Map<Class<?>, ? extends Spec> specMap) {
    return type -> Optional.ofNullable(specMap.get(type));
  }
}