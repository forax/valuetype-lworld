package fr.umlv.valuetype.perf;

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

import fr.umlv.valuetype.IntBox;

@SuppressWarnings("static-method")
@Warmup(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(value = 3, jvmArgsAppend = "-XX:+EnableValhalla")
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class IntBoxBenchMark {
  
  @Benchmark
  public int sum_IntBox() {
    IntBox sum = IntBox.valueOf(0);
    IntBox n = IntBox.valueOf(100_000);
    for(IntBox i = IntBox.valueOf(0); i.compareTo(n) < 0; i = i.add(IntBox.valueOf(1))) {
      sum = sum.add(i);
    }
    return sum.intValue();
  }
  
  @Benchmark
  public int sum_Integer() {
    Integer sum = Integer.valueOf(0);
    Integer n = Integer.valueOf(100_000);
    for(Integer i = Integer.valueOf(0); i.compareTo(n) < 0; i = Integer.valueOf(i.intValue() + 1)) {
      sum = sum.intValue() + i.intValue();
    }
    return sum.intValue();
  }

  @Benchmark
  public int sum_int() {
    int sum = 0;
    int n = 100_000;
    for(int i = 0; i < n; i = i + 1) {
      sum = sum + i;
    }
    return sum;
  }
  
  public static void main(String[] args) throws RunnerException {
    Options opt = new OptionsBuilder()
        .include(IntBoxBenchMark.class.getName())
        .build();
    new Runner(opt).run();
  }
}
