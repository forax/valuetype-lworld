package fr.umlv.valuetype.perf;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

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

import fr.umlv.valuetype.IntBox;

@SuppressWarnings("static-method")
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 3, jvmArgsAppend = "-XX:+EnableValhalla")
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class IntBoxArrayBenchMark {
  static class IntArray {
    private static final int[] ARRAY = new int[100_000]; 
    static {
      IntStream.range(0, ARRAY.length).forEach(i -> ARRAY[i] = i);
    }
  }
  static class IntBoxArray {
    private static final IntBox[] ARRAY = new IntBox[100_000];
    static {
      IntStream.range(0, ARRAY.length).forEach(i -> ARRAY[i] = IntBox.valueOf(i));
      //Collections.shuffle(Arrays.asList(ARRAY));
    }
  }
  static class IntegerArray {
    private static final Integer[] ARRAY = new Integer[100_000]; 
    static {
      IntStream.range(0, ARRAY.length).forEach(i -> ARRAY[i] = Integer.valueOf(i));
      //Collections.shuffle(Arrays.asList(ARRAY));
    }
  }
  
  @Benchmark
  public int sum_IntBox() {
    var sum = 0;
    for(var value : IntBoxArray.ARRAY) {
      sum += value.intValue();
    }
    return sum;
  }
  
  @Benchmark
  public int sum_Integer() {
    var sum = 0;
    for(var value : IntegerArray.ARRAY) {
      sum += value.intValue();
    }
    return sum;
  }

  @Benchmark
  public int sum_int() {
    var sum = 0;
    for(var value : IntArray.ARRAY) {
      sum += value;
    }
    return sum;
  }
  
  public static void main(String[] args) throws RunnerException {
    Options opt = new OptionsBuilder()
        .include(IntBoxArrayBenchMark.class.getName())
        .build();
    new Runner(opt).run();
  }
}
