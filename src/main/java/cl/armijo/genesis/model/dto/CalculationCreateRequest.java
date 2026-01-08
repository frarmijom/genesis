package cl.armijo.genesis.model.dto;

import cl.armijo.genesis.model.enums.FunctionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CalculationCreateRequest {

  @NotNull
  private FunctionType functionType;

  @NotNull
  private Double a;

  @NotNull
  private Double b;

  @NotNull
  @Min(10_000)
  @Max(200_000_000)
  private Long samplesN;

  private Long seed;

  public FunctionType getFunctionType() { return functionType; }
  public void setFunctionType(FunctionType functionType) { this.functionType = functionType; }

  public Double getA() { return a; }
  public void setA(Double a) { this.a = a; }

  public Double getB() { return b; }
  public void setB(Double b) { this.b = b; }

  public Long getSamplesN() { return samplesN; }
  public void setSamplesN(Long samplesN) { this.samplesN = samplesN; }

  public Long getSeed() { return seed; }
  public void setSeed(Long seed) { this.seed = seed; }
}
