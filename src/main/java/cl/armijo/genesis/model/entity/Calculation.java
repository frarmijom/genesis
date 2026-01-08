package cl.armijo.genesis.model.entity;

import cl.armijo.genesis.model.enums.FunctionType;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "calculations")
public class Calculation {

  @Id
  @GeneratedValue
  private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private FunctionType functionType;

  @Column(nullable = false)
  private double a;

  @Column(nullable = false)
  private double b;

  @Column(nullable = false)
  private long samplesN;

  private Long seed;

  @Column(nullable = false)
  private double result;

  @Column(nullable = false)
  private double stdError;

  @Column(nullable = false)
  private double ciLow;

  @Column(nullable = false)
  private double ciHigh;

  @Column(nullable = false)
  private long durationMs;

  private Long cpuTimeMs;

  @Column(nullable = false, length = 20)
  private String status;

  @Column(nullable = false)
  private boolean disabled;

  @Column(nullable = false)
  private OffsetDateTime createdAt;

  @Column(nullable = false)
  private OffsetDateTime updatedAt;

  @PrePersist
  void prePersist() {
    var now = OffsetDateTime.now();
    createdAt = now;
    updatedAt = now;
    if (status == null) status = "SUCCESS";
    disabled = false;
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = OffsetDateTime.now();
  }

  public UUID getId() { return id; }

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

  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }

  public boolean isDisabled() { return disabled; }
  public void setDisabled(boolean disabled) { this.disabled = disabled; }

  public OffsetDateTime getCreatedAt() { return createdAt; }
  public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
