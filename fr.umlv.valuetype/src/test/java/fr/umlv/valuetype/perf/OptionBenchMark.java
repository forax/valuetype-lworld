package fr.umlv.valuetype.perf;

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

import fr.umlv.valuetype.Option;

@SuppressWarnings("static-method")
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(value = 3, jvmArgsAppend = { "-XX:+EnableValhalla" })
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class OptionBenchMark {
	private static final Map<String, Integer> MAP = Map.of("foo", 1, "bar", 2, "baz", 3);
	private static final List<String> LIST = List.of("a", "foo", "b", "bar", "c", "baz");

	@Benchmark
	public int sum_null() {
		int sum = 0;
		for (var key : LIST) {
			var value = MAP.get(key);
			sum += (value != null) ? value : 0;
		}
		return sum;
	}

	@Benchmark
	public int sum_option() {
		int sum = 0;
		for (var key : LIST) {
			var value = Option.ofNullable(MAP.get(key));
			sum += value.orElse(0);
		}
		return sum;
	}

	@Benchmark
	public int sum_optional() {
		int sum = 0;
		for (var key : LIST) {
			var value = Optional.ofNullable(MAP.get(key));
			sum += value.orElse(0);
		}
		return sum;
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder().include(OptionBenchMark.class.getName()).build();
		new Runner(opt).run();
	}
}
