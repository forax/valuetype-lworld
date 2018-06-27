package fr.umlv.valuetype.perf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import fr.umlv.valuetype.Option;
import fr.umlv.valuetype.ReifiedList;

@SuppressWarnings("static-method")
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(value = 3, jvmArgsAppend = {"-XX:+EnableValhalla"/*, "-XX:+PrintCompilation", "-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintInlining"*/})
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class ReifiedListBenchMark {
  private static final ReifiedList<IntBox> INTBOX_LIST;
  static {
    var list = new ReifiedList<IntBox>(IntBox.class);
    for(int i = 0; i < 100_000; i++) {
      list.add(IntBox.valueOf(i));
    }
    INTBOX_LIST = list;
  }
  
  private static final ArrayList<Integer> INTEGER_LIST;
  static {
    var list = new ArrayList<Integer>();
    for(int i = 0; i < 100_000; i++) {
      list.add(Integer.valueOf(i));
    }
    INTEGER_LIST = list;
  }
  
  @Benchmark
  public int reifiedlist_intbox_get() {
    int sum = 0;
    int size = INTBOX_LIST.size();
    for(int i = 0; i < size; i++) {
      sum += INTBOX_LIST.get(i).intValue();
    }
    return sum;
  }
  
  @Benchmark
  public int reifiedlist_intbox_cursor() {
    int sum = 0;
    for(var cursor = INTBOX_LIST.cursor(); cursor != null; cursor = cursor.next()) {
      sum += cursor.element().intValue();
    }
    return sum;
  }
  
  @Benchmark
  public int reifiedlist_intbox_iterator() {
    int sum = 0;
    for(var iterator = INTBOX_LIST.iterator(); iterator.hasNext();) {
      sum += iterator.next().intValue();
    }
    return sum;
  }
  
  @Benchmark
  public int arraylist_integer_iterator() {
    int sum = 0;
    for(var iterator = INTEGER_LIST.iterator(); iterator.hasNext();) {
      sum += iterator.next();
    }
    return sum;
  }
  
  @Benchmark
  public int arraylist_integer_get() {
    int sum = 0;
    int size = INTEGER_LIST.size();
    for(int i = 0; i < size; i++) {
      sum += INTEGER_LIST.get(i);
    }
    return sum;
  }
  
  public static void main(String[] args) throws RunnerException {
    Options opt = new OptionsBuilder()
        .include(ReifiedListBenchMark.class.getName())
        .build();
    new Runner(opt).run();
  }
}
