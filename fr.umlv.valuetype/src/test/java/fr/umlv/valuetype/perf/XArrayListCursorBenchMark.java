/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 */
package fr.umlv.valuetype.perf;

import fr.umlv.valuetype.xlist.InlineCursor;
import fr.umlv.valuetype.xlist.XArrayList;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@Fork(1)
@Warmup(iterations = 3, time = 3)
@Measurement(iterations = 5, time = 3)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Thread)
public class XArrayListCursorBenchMark {
    @Param({"100000"})
    public static int size;

    private static final String constantString = "abc";

    private static XArrayList<String> list;

    @Setup
    public void setup() {
        list = new XArrayList<>();
        for (var i = 0; i < size; i++) {
            list.add(constantString);
        }
    }

    @Benchmark
    public void getViaCursorWhileLoop(Blackhole blackhole) {
        var cur = list.cursor();
        while (cur.exists()) {
            blackhole.consume(cur.get());
            cur = cur.advance();
        }
    }

    @Benchmark
    public void getViaCursorForLoop(Blackhole blackhole) {
        for (var cur = list.cursor();
             cur.exists();
             cur = cur.advance()) {
            blackhole.consume(cur.get());
        }
    }

    @Benchmark
    public void getViaIterator(Blackhole blackhole) {
        var it = list.iterator();
        while (it.hasNext()) {
            blackhole.consume(it.next());
        }
    }

    @Benchmark
    public void getViaIteratorCurs(Blackhole blackhole) {
        var it = list.iteratorCurs();
        while (it.hasNext()) {
            blackhole.consume(it.next());
        }
    }

    @Benchmark
    public void getViaArray(Blackhole blackhole) {
        for (var i = 0; i < list.size(); i++) {
            blackhole.consume(list.get(i));
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(XArrayListCursorBenchMark.class.getName())
            .build();
        new Runner(opt).run();
    }
}