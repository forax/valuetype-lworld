package fr.umlv.jsonapi.bind;

import static java.lang.invoke.MethodType.methodType;
import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

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
  static SpecFinder associate(Class<?> type, Spec spec) {
    return t -> Optional.of(t).filter(type::equals).map(__ -> spec);
  }
}