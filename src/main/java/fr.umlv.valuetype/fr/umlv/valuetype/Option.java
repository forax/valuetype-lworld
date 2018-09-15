package fr.umlv.valuetype;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final __ByValue class Option<E> {
  private final E value;
  
  private Option() {
    value = null;
    throw new AssertionError();
  }
  
  public static <E> Option<E> empty() {
    return Option.default;
  }
  
  public static <E> Option<E> of(E value) {
    Objects.requireNonNull(value);
    Option<E> option = Option.default;
    option = __WithField(option.value, value);
    return option;
  }
  
  public static <E> Option<E> ofNullable(E value) {
    return (value == null)? empty(): of(value);
  }
  
  public boolean isPresent() {
    return value != null;
  }
  public boolean isAbsent() {
    return value == null;
  }
  
  public void ifPresent(Consumer<? super E> consumer) {
    Objects.requireNonNull(consumer);
    if (value != null) {
      consumer.accept(value);
    }
  }
  
  public E orElse(E defaultValue) {
    return (value == null)? defaultValue: value;
  }
  
  public E orElseGet(Supplier<? extends E> supplier) {
    Objects.requireNonNull(supplier);
    return (value == null)? supplier.get(): value;
  }
  
  public Option<E> or(Supplier<? extends Option<E>> supplier) {
    Objects.requireNonNull(supplier);
    return (value == null)? supplier.get(): this;
  }
  
  public <R> Option<R> flatMap(Function<? super E, ? extends Option<R>> mapper) {
    Objects.requireNonNull(mapper);
    return (value == null)? empty(): mapper.apply(value);
  }
  public Option<E> filter(Predicate<? super E> predicate) {
    Objects.requireNonNull(predicate);
    return flatMap(value -> predicate.test(value)? this: empty());
  }
  public <R> Option<R> map(Function<? super E, ? extends R> mapper) {
    Objects.requireNonNull(mapper);
    return flatMap(value -> Option.of(mapper.apply(value)));
  }
}
