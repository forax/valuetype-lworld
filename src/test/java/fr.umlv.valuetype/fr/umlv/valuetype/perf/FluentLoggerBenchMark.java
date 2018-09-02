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

import fr.umlv.valuetype.FluentLogger;
import fr.umlv.valuetype.FluentLogger.Level;

@SuppressWarnings("static-method")
@Warmup(iterations = 3, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(value = 3, jvmArgsAppend = {"-XX:+EnableValhalla" })
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class FluentLoggerBenchMark {
  static class LoggerHolder {
    static final FluentLogger LOGGER = FluentLogger.create(FluentLoggerBenchMark.class, Level.WARNING);
  }
  
  @Benchmark
  public void log() {
    LoggerHolder.LOGGER.atDebug().message("should not be logged").log();
  }
  
  @Benchmark
  public void noop() {
    // empty
  }
  
  public static void main(String[] args) throws RunnerException {
    Options opt = new OptionsBuilder()
        .include(FluentLoggerBenchMark.class.getName())
        .build();
    new Runner(opt).run();
  }
}
