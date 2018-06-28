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
  private final ReifiedList<IntBox> intBoxList;
  {
    var list = new ReifiedList<IntBox>(IntBox.class);
    for(int i = 0; i < 100_000; i++) {
      list.add(IntBox.valueOf(i));
    }
    intBoxList = list;
  }
  
  private final ArrayList<Integer> integerList;
  {
    var list = new ArrayList<Integer>();
    for(int i = 0; i < 100_000; i++) {
      list.add(Integer.valueOf(i));
    }
    integerList = list;
  }
  
  @Benchmark
  public int reifiedlist_intbox_get() {
    int sum = 0;
    int size = intBoxList.size();
    for(int i = 0; i < size; i++) {
      sum += intBoxList.get(i).intValue();
    }
    return sum;
  }
  
  @Benchmark
  public int reifiedlist_intbox_cursor() {
    int sum = 0;
    for(var cursor = intBoxList.cursor(); cursor != null; cursor = cursor.next()) {
      sum += cursor.element().intValue();
    }
    return sum;
  }
  
  @Benchmark
  public int reifiedlist_intbox_iterator() {
    int sum = 0;
    for(var iterator = intBoxList.iterator(); iterator.hasNext();) {
      sum += iterator.next().intValue();
    }
    return sum;
  }
  
  @Benchmark
  public int arraylist_integer_iterator() {
    int sum = 0;
    for(var iterator = integerList.iterator(); iterator.hasNext();) {
      sum += iterator.next();
    }
    return sum;
  }
  
  @Benchmark
  public int arraylist_integer_get() {
    int sum = 0;
    int size = integerList.size();
    for(int i = 0; i < size; i++) {
      sum += integerList.get(i);
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
