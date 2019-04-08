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
  
  @__value__
  static /*value*/ class Tuple {
    private final int index;
    private final int element;
    
    private Tuple(int index, int element) {
      this.index = index;
      this.element = element;
    }
  }
  
  @__value__
  static /*value*/ class Cursor {
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
      
//      Cursor.box box;
//      if (index + 1 == array.length) {
//        box = null;
//      } else {
//        box = new Cursor(array, index + 1);
//      }
//      return box;
    }
  }
  
  private static Cursor.box indexedElements(int[] array) {
    if (array.length == 0) {
      return null;
    }
    return new Cursor(array, 0);
//    Cursor.box box;
//    if (array.length == 0) {
//      box = null;
//    } else {
//      box = new Cursor(array, 0);
//    }
//    return box;
  }
  
  @__value__
  static /*value*/ class FlatCursor {
    private final int[] array;
    private final int index;
    
    private FlatCursor(int[] array, int index) {
      this.array = array;
      this.index = index;
    }
    
    Tuple current() {
      return new Tuple(index, array[index]);
    }
    
    boolean hasNext() {
      return index < array.length;
    }
    
    FlatCursor next() {
      return new FlatCursor(array, index + 1);
    }
  }
  
  private static FlatCursor flatIndexedElements(int[] array) {
    return new FlatCursor(array, 0);
  }
  
  @Benchmark
  public int sum_indexedElements() {
    var sum = 0;
    for(var cursor = indexedElements(ARRAY); cursor != null; cursor = cursor.next()) {
      var tuple = cursor.current();
      sum += tuple.index + tuple.element;
    }
    return sum;
  }
  
  @Benchmark
  public int sum_flat_indexedElements() {
    var sum = 0;
    for(var cursor = flatIndexedElements(ARRAY); cursor.hasNext(); cursor = cursor.next()) {
      var tuple = cursor.current();
      sum += tuple.index + tuple.element;
    }
    return sum;
  }
  
  @Benchmark
  public int sum_loop() {
    var sum = 0;
    for(var i = 0; i < ARRAY.length; i = i + 1) {
      sum += i + ARRAY[i];
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
