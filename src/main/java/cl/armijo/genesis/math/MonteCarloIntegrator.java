package cl.armijo.genesis.math;

import cl.armijo.genesis.model.enums.FunctionType;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.SplittableRandom;

public class MonteCarloIntegrator {

  public static class Output {
    private final double result;
    private final double stdError;
    private final double ciLow;
    private final double ciHigh;
    private final long durationMs;
    private final Long cpuTimeMs;

    public Output(double result, double stdError, double ciLow, double ciHigh, long durationMs, Long cpuTimeMs) {
      this.result = result;
      this.stdError = stdError;
      this.ciLow = ciLow;
      this.ciHigh = ciHigh;
      this.durationMs = durationMs;
      this.cpuTimeMs = cpuTimeMs;
    }

    public double getResult() { return result; }
    public double getStdError() { return stdError; }
    public double getCiLow() { return ciLow; }
    public double getCiHigh() { return ciHigh; }
    public long getDurationMs() { return durationMs; }
    public Long getCpuTimeMs() { return cpuTimeMs; }
  }

  public static Output integrate(FunctionType type, double a, double b, long n, Long seed) {
    if (b <= a) throw new IllegalArgumentException("b must be > a");
    if (n <= 0) throw new IllegalArgumentException("samplesN must be > 0");

    long t0 = System.nanoTime();

    ThreadMXBean mx = ManagementFactory.getThreadMXBean();
    boolean cpuSupported = mx.isCurrentThreadCpuTimeSupported();
    boolean cpuEnabled = cpuSupported && mx.isThreadCpuTimeEnabled();
    long cpu0 = cpuEnabled ? mx.getCurrentThreadCpuTime() : 0L;

    SplittableRandom rnd = (seed == null) ? new SplittableRandom() : new SplittableRandom(seed);

    double sum = 0.0;
    double sumSq = 0.0;

    double width = (b - a);

    for (long i = 0; i < n; i++) {
      double x = a + width * rnd.nextDouble();
      double fx = f(type, x);
      sum += fx;
      sumSq += fx * fx;
    }

    double mean = sum / n;

    // var(f) = E[f^2] - (E[f])^2
    double var = (sumSq / n) - (mean * mean);
    if (var < 0) var = 0;

    double sd = Math.sqrt(var);
    double seMean = sd / Math.sqrt(n);

    double integral = width * mean;
    double seIntegral = width * seMean;

    double z = 1.96; // ~95%
    double ciLow = integral - z * seIntegral;
    double ciHigh = integral + z * seIntegral;

    long t1 = System.nanoTime();
    long durationMs = (t1 - t0) / 1_000_000;

    Long cpuTimeMs = null;
    if (cpuEnabled) {
      long cpu1 = mx.getCurrentThreadCpuTime();
      cpuTimeMs = (cpu1 - cpu0) / 1_000_000;
    }

    return new Output(integral, seIntegral, ciLow, ciHigh, durationMs, cpuTimeMs);
  }

  private static double f(FunctionType type, double x) {
    return switch (type) {
      case GAUSSIAN_SIN -> Math.sin(x) * Math.exp(-(x * x));
      case LOG_HEAVY -> Math.log(1.0 + x * x) * Math.sqrt(1.0 + x * x);
      case OSCILLATORY -> Math.sin(50.0 * x) / (1.0 + x * x);
    };
  }
}
