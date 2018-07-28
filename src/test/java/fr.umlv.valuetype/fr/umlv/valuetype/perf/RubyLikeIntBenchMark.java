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

import fr.umlv.valuetype.RubyLikeInt;
import fr.umlv.valuetype.IntBox;

@SuppressWarnings("static-method")
@Warmup(iterations = 3, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(value = 3, jvmArgsAppend = {"-XX:+EnableValhalla"/*, "-XX:+PrintCompilation", "-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintInlining"*/})
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class RubyLikeIntBenchMark {
  @Benchmark
  public int int_loop() {
    int result = 0;
    int length = 100_000;
    for(int i = 0; i < length; i++) {
      result = result * 13 + i;
      if (result > 10_000_000) {
        result = result / 1_000_000;
      }
    }
    return result;  // 18
  }
  
  @Benchmark
  public int intbox_loop() {
    var result = IntBox.valueOf(0);
    var length = IntBox.valueOf(100_000);
    for(var i = IntBox.valueOf(0); i.compareTo(length) < 0; i = i.increment()) {
      result = result.multiply(IntBox.valueOf(13)).add(i);
      if (result.compareTo(IntBox.valueOf(10_000_000)) > 0) {
        result = result.divide(IntBox.valueOf(1_000_000));
      }
    }
    return result.intValue();  // 18
  }
  
  @Benchmark
  public int ruby_like_int_loop() {
    var result = RubyLikeInt.small(0);
    var length = RubyLikeInt.small(100_000);
    for(var i = RubyLikeInt.small(0); i.compareTo(length) < 0; i = i.succ()) {
      result = result.multiply(RubyLikeInt.small(13)).add(i);
      if (result.compareTo(RubyLikeInt.small(10_000_000)) > 0) {
        result = result.divide(RubyLikeInt.small(1_000_000));
      }
    }
    return result.extractSmallValue();  // 18
  }
  
  @Benchmark
  public int ruby_like_int_range_reduce() {
    return RubyLikeInt.small(0).rangeReduce(RubyLikeInt.small(100_000), RubyLikeInt.small(0), (acc, index) -> {
      var res = acc.multiply(RubyLikeInt.small(13)).add(index);
      if (res.compareTo(RubyLikeInt.small(10_000_000)) > 0) {
        return res.divide(RubyLikeInt.small(1_000_000));
      }
      return res;
    }).extractSmallValue();  // 18
  }
  
  /*@Benchmark
  public Integer integer_loop() {
    Integer result = 0;
    Integer length = 100_000;
    for(Integer i = 0; i < length; i++) {
      result = result * 13 + i;
      if (result > 10_000_000) {
        result = result / 1_000_000;
      }
    }
    return result;  // 18
  }*/
  
  public static void main(String[] args) throws RunnerException {
    Options opt = new OptionsBuilder()
        .include(RubyLikeIntBenchMark.class.getName())
        .build();
    new Runner(opt).run();
  }
}
