package fr.umlv.valuetype;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface OptionItf<E> {
	public static <E> OptionItf<E> empty() {
    return val.empty();
  }
  
  public static <E> OptionItf<E> of(E value) {
    return val.of(value);
  }
  
  public static <E> OptionItf<E> ofNullable(E value) {
    return val.ofNullable(value);
  }
	  
	  public boolean isPresent();
	  public boolean isAbsent();
	  public void ifPresent(Consumer<? super E> consumer);
	  public E orElse(E defaultValue);
	  public E orElseGet(Supplier<? extends E> supplier);
	  public OptionItf<E> or(Supplier<? extends OptionItf<E>> supplier);
	  public <R> OptionItf<R> flatMap(Function<? super E, ? extends OptionItf<R>> mapper);
	  public OptionItf<E> filter(Predicate<? super E> predicate);
	  public <R> OptionItf<R> map(Function<? super E, ? extends R> mapper);
	  
	  @__inline__
	  public final /*inline*/ class val<E> implements OptionItf<E> {
	    private final E value;
	    
	    private val(E value) {
	      this.value = value;
	    }
	    
	    public static <E> val<E> empty() {
	      return val<E>.default;
	    }
	    
	    public static <E> val<E> of(E value) {
	      Objects.requireNonNull(value);
	      return new val<>(value);
	    }
	    
	    public static <E> val<E> ofNullable(E value) {
	      return (value == null)? empty(): of(value);
	    }
	    
	    @Override
			public boolean isPresent() {
	      return value != null;
	    }
	    @Override
			public boolean isAbsent() {
	      return value == null;
	    }
	    
	    @Override
			public void ifPresent(Consumer<? super E> consumer) {
	      Objects.requireNonNull(consumer);
	      if (value != null) {
	        consumer.accept(value);
	      }
	    }
	    
	    @Override
			public E orElse(E defaultValue) {
	      return (value == null)? defaultValue: value;
	    }
	    
	    @Override
			public E orElseGet(Supplier<? extends E> supplier) {
	      Objects.requireNonNull(supplier);
	      return (value == null)? supplier.get(): value;
	    }
	    
			@Override
			@SuppressWarnings("unchecked")
			public val<E> or(Supplier<? extends OptionItf<E>> supplier) {
	      Objects.requireNonNull(supplier);
	      return (value == null)? (val<E>)supplier.get(): this;
	    }
	    
	    @Override
	    @SuppressWarnings("unchecked")
			public <R> val<R> flatMap(Function<? super E, ? extends OptionItf<R>> mapper) {
	      Objects.requireNonNull(mapper);
	      return (value == null)? empty(): (val<R>)mapper.apply(value);
	    }
	    @Override
			public val<E> filter(Predicate<? super E> predicate) {
	      Objects.requireNonNull(predicate);
	      return flatMap(value -> predicate.test(value)? this: empty());
	    }
	    @Override
			public <R> val<R> map(Function<? super E, ? extends R> mapper) {
	      Objects.requireNonNull(mapper);
	      return flatMap(value -> OptionItf.of(mapper.apply(value)));
	    }
	  }
}
