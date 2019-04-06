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

import fr.umlv.valuetype.UnsignedInt;

@SuppressWarnings("static-method")
@Warmup(iterations = 3, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(value = 3, jvmArgsAppend = {"-XX:+EnableValhalla" })
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class UnsignedIntBenchMark {
  @Benchmark
  public int sum() {
    var sum = 0;
    for(var i = 0; i < 100_000; i++) {
      sum = sum + i % 91;
    }
    return sum;
  }
  
  @Benchmark
  public int unsigned_int_sum() {
    var sum = UnsignedInt.zero();
    for(var i = UnsignedInt.zero(); i.compareTo(UnsignedInt.ofUnsigned(100_000)) < 0; i = i.increment()) {
      sum = sum.add(i.remainder(UnsignedInt.ofUnsigned(91)));
    }
    return sum.asSigned();
  }
  
  public static void main(String[] args) throws RunnerException {
    Options opt = new OptionsBuilder()
        .include(UnsignedIntBenchMark.class.getName())
        .build();
    new Runner(opt).run();
  }
}
