package io.microraft.benchmark;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import io.microraft.impl.util.Long2ObjectHashMap;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 3)
@Fork(1)
@State(Scope.Thread)
public class Long2ObjectHashMapBenchmark {

    private Long2ObjectHashMap<String> map;

    @Setup
    public void setUp() {
        map = new Long2ObjectHashMap<>();
        for (long i = 0; i < 10_000; i++) {
            map.put(i, "entry-" + i);
        }
    }

    @Benchmark
    public String readExistingValue() {
        return map.get(5_000);
    }

    @Benchmark
    public String writeAndReadValue() {
        map.put(12_000, "new-entry");
        return map.get(12_000);
    }
}
