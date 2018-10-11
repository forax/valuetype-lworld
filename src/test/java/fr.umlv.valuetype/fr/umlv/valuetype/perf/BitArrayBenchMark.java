package fr.umlv.valuetype.perf;

import java.util.BitSet;
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

import fr.umlv.valuetype.BitArray;

@SuppressWarnings("static-method")
@Warmup(iterations = 3, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(value = 3, jvmArgsAppend = {"-XX:+EnableValhalla" })
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class BitArrayBenchMark {
  static final BitArray BIT_ARRRAY = new BitArray(1_000_000);
  static final boolean[] BOOL_ARRAY = new boolean[1_000_000];
  static final BitSet BIT_SET = new BitSet(1_000_000);
  
  @Benchmark
  public void bit_array() {
    for(int i = 0; i < 1_000_000; i++) {
      BIT_ARRRAY.set(i);
    }
  }
  
  @Benchmark
  public void bool_array() {
    for(int i = 0; i < 1_000_000; i++) {
      BOOL_ARRAY[i] = true;
    }
  }
  
  @Benchmark
  public void bit_set() {
    for(int i = 0; i < 1_000_000; i++) {
      BIT_SET.set(i);
    }
  }
  
  public static void main(String[] args) throws RunnerException {
    Options opt = new OptionsBuilder()
        .include(BitArrayBenchMark.class.getName())
        .build();
    new Runner(opt).run();
  }
}
