package cl.armijo.genesis.service;

import cl.armijo.genesis.math.MonteCarloIntegrator;
import cl.armijo.genesis.model.dto.CalculationCreateRequest;
import cl.armijo.genesis.model.dto.CalculationResponse;
import cl.armijo.genesis.model.entity.Calculation;
import cl.armijo.genesis.repository.CalculationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CalculationService {

  private final CalculationRepository repo;

  public CalculationService(CalculationRepository repo) {
    this.repo = repo;
  }

  public CalculationResponse createAndRun(CalculationCreateRequest req) {
    var out = MonteCarloIntegrator.integrate(
        req.getFunctionType(),
        req.getA(),
        req.getB(),
        req.getSamplesN(),
        req.getSeed()
    );

    Calculation entity = new Calculation();
    entity.setFunctionType(req.getFunctionType());
    entity.setA(req.getA());
    entity.setB(req.getB());
    entity.setSamplesN(req.getSamplesN());
    entity.setSeed(req.getSeed());

    entity.setResult(out.getResult());
    entity.setStdError(out.getStdError());
    entity.setCiLow(out.getCiLow());
    entity.setCiHigh(out.getCiHigh());
    entity.setDurationMs(out.getDurationMs());
    entity.setCpuTimeMs(out.getCpuTimeMs());
    entity.setStatus("SUCCESS");

    entity = repo.save(entity);
    return toResponse(entity);
  }

  public Page<CalculationResponse> list(boolean includeDisabled, Pageable pageable) {
    return (includeDisabled ? repo.findAll(pageable) : repo.findByDisabledFalse(pageable))
        .map(this::toResponse);
  }

  public CalculationResponse get(UUID id, boolean includeDisabled) {
    Calculation c = includeDisabled
        ? repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"))
        : repo.findByIdAndDisabledFalse(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
    return toResponse(c);
  }

  public CalculationResponse rerun(UUID id, CalculationCreateRequest req) {
    Calculation existing = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));

    var out = MonteCarloIntegrator.integrate(
        req.getFunctionType(),
        req.getA(),
        req.getB(),
        req.getSamplesN(),
        req.getSeed()
    );

    existing.setFunctionType(req.getFunctionType());
    existing.setA(req.getA());
    existing.setB(req.getB());
    existing.setSamplesN(req.getSamplesN());
    existing.setSeed(req.getSeed());

    existing.setResult(out.getResult());
    existing.setStdError(out.getStdError());
    existing.setCiLow(out.getCiLow());
    existing.setCiHigh(out.getCiHigh());
    existing.setDurationMs(out.getDurationMs());
    existing.setCpuTimeMs(out.getCpuTimeMs());
    existing.setStatus("SUCCESS");

    existing = repo.save(existing);
    return toResponse(existing);
  }

  public void disable(UUID id) {
    Calculation c = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
    c.setDisabled(true);
    repo.save(c);
  }

  public void enable(UUID id) {
    Calculation c = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
    c.setDisabled(false);
    repo.save(c);
  }

  private CalculationResponse toResponse(Calculation c) {
    CalculationResponse r = new CalculationResponse();
    r.setId(c.getId());
    r.setFunctionType(c.getFunctionType());
    r.setA(c.getA());
    r.setB(c.getB());
    r.setSamplesN(c.getSamplesN());
    r.setSeed(c.getSeed());
    r.setStatus(c.getStatus());
    r.setDisabled(c.isDisabled());
    r.setResult(c.getResult());
    r.setStdError(c.getStdError());
    r.setCiLow(c.getCiLow());
    r.setCiHigh(c.getCiHigh());
    r.setDurationMs(c.getDurationMs());
    r.setCpuTimeMs(c.getCpuTimeMs());
    r.setCreatedAt(c.getCreatedAt());
    r.setUpdatedAt(c.getUpdatedAt());
    return r;
  }
}
