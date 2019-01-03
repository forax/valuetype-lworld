package fr.umlv.valuetype.perf;

import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.TimeUnit;

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
public class TupleLoopBenchMark {
  private static final int[] ARRAY = new Random(0).ints(100_000).toArray();
  
  static value class Tuple {
    private final int index;
    private final int element;
    
    
    private Tuple(int index, int element) {
      this.index = index;
      this.element = element;
    }
  }
  
  static value class Cursor {
    private final int[] array;
    private final int index;
    
    private Cursor(int[] array, int index) {
      this.array = array;
      this.index = index;
    }
    
    Tuple current() {
      return new Tuple(index, array[index]);
    }
    
    Cursor.box next() {
      if (index + 1 == array.length) {
        return null;
      }
      return new Cursor(array, index + 1);
    }
  }
  
  private static Cursor.box indexedElements(int[] array) {
    if (array.length == 0) {
      return null;
    }
    return new Cursor(array, 0);
  }
  
  private static Cursor fix(Cursor.box cursor) {
    return cursor;
  }
  
  @Benchmark
  public int sum() {
    int sum = 0;
    for(Cursor.box cursor = indexedElements(ARRAY); cursor != null; cursor = fix(cursor).next()) {
      Tuple tuple = fix(cursor).current();
      sum += tuple.index + tuple.element;
    }
    return sum;
  }
  
  public static void main(String[] args) throws RunnerException {
    Options opt = new OptionsBuilder()
        .include(TupleLoopBenchMark.class.getName())
        .build();
    new Runner(opt).run();
  }
}
