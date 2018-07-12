package fr.umlv.valuetype.perf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

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
import fr.umlv.valuetype.ValueList;
import fr.umlv.valuetype.ValueList.ArrayAccess;

@SuppressWarnings("static-method")
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(value = 3, jvmArgsAppend = {"-XX:+EnableValhalla"/*, "-XX:+PrintCompilation", "-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintInlining"*/})
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class ReifiedListBenchMark {
  private final ReifiedList<IntBox> reifiedList;
  {
    var list = new ReifiedList<IntBox>(IntBox.class);
    for(int i = 0; i < 100_000; i++) {
      list.add(IntBox.valueOf(i));
    }
    reifiedList = list;
  }
  
  private final ValueList<IntBox> valueList;
  {
    var list = ValueList.empty(new ArrayAccess<IntBox>() {
      @Override
      public void set(IntBox[] array, int index, IntBox element) {
        array[index] = element;
      }
      
      @Override
      @SuppressWarnings("unchecked")
      public IntBox[] newArray(int capacity) {
        return new IntBox[capacity];
      }
      
      @Override
      public IntBox get(IntBox[] array, int index) {
        return array[index];
      }
      
      @Override
      public IntBox[] copyOf(IntBox[] array, int capacity) {
        return Arrays.copyOf(array, capacity);
      }
    });
    for(int i = 0; i < 100_000; i++) {
      list = list.append(IntBox.valueOf(i));
    }
    valueList = list;
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
    int size = reifiedList.size();
    for(int i = 0; i < size; i++) {
      sum += reifiedList.get(i).intValue();
    }
    return sum;
  }
  /*
  @Benchmark
  public int reifiedlist_intbox_cursor() {
    int sum = 0;
    for(var cursor = reifiedList.cursor(); cursor != null; cursor = cursor.next()) {
      sum += cursor.element().intValue();
    }
    return sum;
  }
  
  @Benchmark
  public int reifiedlist_intbox_iterator() {
    int sum = 0;
    for(var iterator = reifiedList.iterator(); iterator.hasNext();) {
      sum += iterator.next().intValue();
    }
    return sum;
  }*/
  
  @Benchmark
  public int valuelist_intbox_get() {
    int sum = 0;
    int size = valueList.size();
    for(int i = 0; i < size; i++) {
      sum += valueList.get(i).intValue();
    }
    return sum;
  }
  
  @Benchmark
  public int valuelist_intbox_inlined_reduce() {
    var sum = IntBox.zero();
    int size = valueList.size();
    for(int i = 0; i < size; i++) {
      sum = sum.add(valueList.get(i));
    }
    return sum.intValue();
  }
  
  @Benchmark
  public int valuelist_intbox_reduce() {
    return valueList.reduce(IntBox.zero(), (acc, element) -> acc.add(element)).intValue();
  }
  
  /*@Benchmark
  public int valuelist_intbox_innervalue_reduce() {
    __ByValue class Adder implements BiFunction<IntBox, IntBox, IntBox> {
      private final boolean nonEmpty;
      
      Adder() {
        nonEmpty = false;
        throw new AssertionError();
      }
      
      public IntBox apply(IntBox acc, IntBox element) {
        return acc.add(element);
      }
    }
    return valueList.reduce(IntBox.zero(), __MakeDefault Adder()).intValue();
  }*/
  
  @Benchmark
  public int valuelist_intbox_innervalue_inlined_reduce() {
    __ByValue class Adder implements BiFunction<IntBox, IntBox, IntBox> {
      private final boolean nonEmpty;
      
      Adder() {
        nonEmpty = false;
        throw new AssertionError();
      }
      
      public IntBox apply(IntBox acc, IntBox element) {
        return acc.add(element);
      }
    }
    BiFunction<IntBox, IntBox, IntBox> mapper = __MakeDefault Adder();
    var sum = IntBox.zero();
    int size = valueList.size();
    for(int i = 0; i < size; i++) {
      sum = mapper.apply(sum, valueList.get(i));
    }
    return sum.intValue();
  }
  
  
  /*@Benchmark
  public int arraylist_integer_iterator() {
    int sum = 0;
    for(var iterator = integerList.iterator(); iterator.hasNext();) {
      sum += iterator.next();
    }
    return sum;
  }*/
  
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
