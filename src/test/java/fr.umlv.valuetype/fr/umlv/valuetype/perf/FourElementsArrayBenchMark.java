package fr.umlv.valuetype.perf;

import java.util.List;
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

import fr.umlv.valuetype.FourElementsArray;

@SuppressWarnings("static-method")
@Warmup(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(value = 3, jvmArgsAppend = { "-XX:+EnableValhalla" })
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class FourElementsArrayBenchMark {
	@Benchmark
	public int list_of() {
		var list = List.of(1, 2, 3, 4);
		var sum = 0;
		for(var i = 0; i < list.size(); i++) {
			sum += i;
		}
		return sum; 
	}

	@Benchmark
	public int four_elements_array() {
		var list = FourElementsArray.of(1, 2, 3, 4);
		var sum = 0;
		for(var i = 0; i < list.size(); i++) {
			sum += i;
		}
		return sum; 
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder().include(FourElementsArrayBenchMark.class.getName()).build();
		new Runner(opt).run();
	}
}
