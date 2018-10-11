package fr.umlv.valuetype.perf;

import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@SuppressWarnings("static-method")
@Warmup(iterations = 3, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(value = 3, jvmArgsAppend = {"-XX:+EnableValhalla" })
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class ComparatorBenchMark {
  static class Person {
    private final String firstname;
    private final String lastname;
    
    public Person(String firstname, String lastname) {
      this.firstname = firstname;
      this.lastname = lastname;
    }
    
    public String getFirstname() {
      return firstname;
    }
    public String getLastname() {
      return lastname;
    }
  }
  
  static final Person[] PERSONS = new Person[1_000_000];
  static {
    var random = ThreadLocalRandom.current();
    Arrays.setAll(PERSONS, i -> new Person("" + random.nextInt(1_000), "" + random.nextInt(1_000)));
  }
  
  static final Comparator<Person> PERSON_COMPARATOR = Comparator.comparing(Person::getFirstname).thenComparing(Person::getLastname);
  
  static <T, U extends Comparable<? super U>> Comparator<T> comparing(Function<? super T, ? extends U> keyExtractor) {
    return new value Comparator<T>() {
      int blank = 0;
      
      @Override
      public int compare(T t1, T t2) {
        return keyExtractor.apply(t1).compareTo(keyExtractor.apply(t2));
      }
    };
  }
  static <T, U extends Comparable<? super U>> Comparator<T> thenComparing(Comparator<? super T> comparator, Function<? super T, ? extends U> keyExtractor) {
    return new value Comparator<T>() {
      int blank = 0;
      
      @Override
      public int compare(T t1, T t2) {
        var cmp = comparator.compare(t1, t2);
        if (cmp != 0)  {
          return cmp;
        }
        return keyExtractor.apply(t1).compareTo(keyExtractor.apply(t2));
      }
    };
  }
  static Function<Person, String> getFirstname() {
    return new value Function<Person, String>() {
      int blank = 0;
      
      @Override
      public String apply(Person p) {
        return p.getFirstname();
      }
    };
  }
  static Function<Person, String> getLastname() {
    return new value Function<Person, String>() {
      int blank = 0;
      
      @Override
      public String apply(Person p) {
        return p.getFirstname();
      }
    };
  }
  
  @Benchmark
  public void vt_compator() {
    Arrays.sort(PERSONS, thenComparing(comparing(getFirstname()), getLastname()));
  }
  
  @Benchmark
  public void poc_compator() {
    Arrays.sort(PERSONS, new value Comparator<Person>() {
      int blank = 0;
      
      public int compare(Person p1, Person p2) {
        var nameCmp = p1.getFirstname().compareTo(p2.getFirstname());
        if (nameCmp != 0) {
          return nameCmp;
        }
        return p1.getLastname().compareTo(p2.getLastname());
      }
    });
  }
  
  @Benchmark
  public void j_u_compator() {
    Arrays.sort(PERSONS, Comparator.comparing(Person::getFirstname).thenComparing(Person::getLastname));
  }
  
  @Benchmark
  public void j_u_compator_static() {
    Arrays.sort(PERSONS, PERSON_COMPARATOR);
  }
  
  
  public static void main(String[] args) throws RunnerException {
    Options opt = new OptionsBuilder()
        .include(ComparatorBenchMark.class.getName())
        .build();
    new Runner(opt).run();
  }
}
