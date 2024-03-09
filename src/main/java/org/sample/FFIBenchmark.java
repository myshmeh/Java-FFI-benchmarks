package org.sample;


import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.util.concurrent.TimeUnit;

@Fork(warmups = 2, value = 2, jvmArgs = {"-Xms512m", "-Xmx1024m", "--enable-native-access=ALL-UNNAMED", "--enable-preview"})
@BenchmarkMode(value = Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 20, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class FFIBenchmark {
    // Java 21 API
    Linker linker = Linker.nativeLinker();
    SymbolLookup libc = linker.defaultLookup();
    MemorySegment getpidAddr = libc.find("getpid").get();
    FunctionDescriptor getpidSig = FunctionDescriptor.of(ValueLayout.JAVA_INT);
    MethodHandle getpid = linker.downcallHandle(getpidAddr, getpidSig);

    // Java 19 API
    // get System linker
//    private static final CLinker linker = CLinker.systemCLinker();
//    // predefine symbols and method handle info
//    private static final NativeSymbol nativeSymbol = linker.lookup("getpid").get();
//    private static final MethodHandle getPidMH = linker.downcallHandle(
//            nativeSymbol,
//            FunctionDescriptor.of(ValueLayout.OfInt.JAVA_INT));

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(FFIBenchmark.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(opt).run();
    }

// uncomment this when running on macOS
//    @Benchmark
//    public int JNI() {
//        return org.bytedeco.javacpp.macosx.getpid();
//    }

    @Benchmark
    public int JNI() {
        return org.bytedeco.javacpp.linux.getpid();
    }

    @Benchmark
    public int panamaDowncall() throws Throwable {
        return (int) getpid.invokeExact();
    }

//    @Benchmark
//    public int panamaJExtract() {
//        return org.unix.unistd_h.getpid();
//    }
}
