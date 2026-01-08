package cl.armijo.genesis.model.enums;

public enum FunctionType {
  GAUSSIAN_SIN,      // sin(x) * exp(-x^2)
  LOG_HEAVY,         // log(1+x^2) * sqrt(1+x^2)
  OSCILLATORY        // sin(50x)/(1+x^2)
}
