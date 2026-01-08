package cl.armijo.genesis.model.dto;

import cl.armijo.genesis.model.enums.FunctionType;

import java.time.OffsetDateTime;
import java.util.UUID;

public class CalculationResponse {

  private UUID id;
  private FunctionType functionType;
  private double a;
  private double b;
  private long samplesN;
  private Long seed;

  private String status;
  private boolean disabled;

  private double result;
  private double stdError;
  private double ciLow;
  private double ciHigh;

  private long durationMs;
  private Long cpuTimeMs;

  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public FunctionType getFunctionType() { return functionType; }
  public void setFunctionType(FunctionType functionType) { this.functionType = functionType; }

  public double getA() { return a; }
  public void setA(double a) { this.a = a; }

  public double getB() { return b; }
  public void setB(double b) { this.b = b; }

  public long getSamplesN() { return samplesN; }
  public void setSamplesN(long samplesN) { this.samplesN = samplesN; }

  public Long getSeed() { return seed; }
  public void setSeed(Long seed) { this.seed = seed; }

  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }

  public boolean isDisabled() { return disabled; }
  public void setDisabled(boolean disabled) { this.disabled = disabled; }

  public double getResult() { return result; }
  public void setResult(double result) { this.result = result; }

  public double getStdError() { return stdError; }
  public void setStdError(double stdError) { this.stdError = stdError; }

  public double getCiLow() { return ciLow; }
  public void setCiLow(double ciLow) { this.ciLow = ciLow; }

  public double getCiHigh() { return ciHigh; }
  public void setCiHigh(double ciHigh) { this.ciHigh = ciHigh; }

  public long getDurationMs() { return durationMs; }
  public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

  public Long getCpuTimeMs() { return cpuTimeMs; }
  public void setCpuTimeMs(Long cpuTimeMs) { this.cpuTimeMs = cpuTimeMs; }

  public OffsetDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

  public OffsetDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
