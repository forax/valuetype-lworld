package fr.umlv.valuetype.perf;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import fr.umlv.valuetype.IntBox;

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
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 3, jvmArgsAppend = {"-XX:+EnableValhalla" })
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class RangeBenchMark  {
  private static final int[] ARRAY = new Random(0).ints(100_000).toArray(); 
  
  private static Iterable<Integer> rangeIntegers(int start, int end) {
    return () -> {
      return new Iterator<>() {
        private int index = start;
        @Override
        public boolean hasNext() {
          return index < end;
        }
        
        @Override
        public Integer next() {
          if (index == end) {
            throw new NoSuchElementException();
          }
          return index++;
        }
      };
    };
  }
  
  private static Iterable<Long> rangeLongs(int start, int end) {
    return () -> {
      return new Iterator<>() {
        private int index = start;
        @Override
        public boolean hasNext() {
          return index < end;
        }
        
        @Override
        public Long next() {
          if (index == end) {
            throw new NoSuchElementException();
          }
          int index = this.index++;
          return (long)index;
        }
      };
    };
  }
  
  private static Iterable<IntBox.ref> rangeIntBoxes(int start, int end) {
    return () -> {
      return new Iterator<>() {
        private int index = start;
        @Override
        public boolean hasNext() {
          return index < end;
        }
        
        @Override
        public IntBox.ref next() {
          if (index == end) {
            throw new NoSuchElementException();
          }
          return IntBox.valueOf(index++);
        }
      };
    };
  }
  
  @Benchmark
  public int sum_index() {
    var sum = 0;
    for(var i = 0; i < ARRAY.length; i++) {
      sum += ARRAY[i];
    }
    return sum;
  }
  
  
  
  @Benchmark
  public int sum_range_intboxes() {
    var sum = 0;
    for(var i: rangeIntBoxes(0, ARRAY.length)) {
      sum += ARRAY[i.intValue()];
    }
    return sum;
  }
  
  @Benchmark
  public int sum_range_integers() {
    var sum = 0;
    for(var i: rangeIntegers(0, ARRAY.length)) {
      sum += ARRAY[i];
    }
    return sum;
  }
  
  @Benchmark
  public int sum_range_longs() {
    var sum = 0;
    for(long i: rangeLongs(0, ARRAY.length)) {
      sum += ARRAY[(int)i];
    }
    return sum;
  }
  
  public static void main(String[] args) throws RunnerException {
    Options opt = new OptionsBuilder()
        .include(RangeBenchMark.class.getName())
        .build();
    new Runner(opt).run();
  }
}

