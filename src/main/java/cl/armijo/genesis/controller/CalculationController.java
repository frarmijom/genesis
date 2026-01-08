package cl.armijo.genesis.controller;

import cl.armijo.genesis.model.dto.CalculationCreateRequest;
import cl.armijo.genesis.model.dto.CalculationResponse;
import cl.armijo.genesis.service.CalculationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/calculations")
public class CalculationController {

  private final CalculationService service;

  public CalculationController(CalculationService service) {
    this.service = service;
  }

  @PostMapping
  public CalculationResponse create(@Valid @RequestBody CalculationCreateRequest req) {
    return service.createAndRun(req);
  }

  @GetMapping
  public Page<CalculationResponse> list(
      @RequestParam(defaultValue = "false") boolean includeDisabled,
      Pageable pageable
  ) {
    return service.list(includeDisabled, pageable);
  }

  @GetMapping("/{id}")
  public CalculationResponse get(
      @PathVariable UUID id,
      @RequestParam(defaultValue = "false") boolean includeDisabled
  ) {
    return service.get(id, includeDisabled);
  }

  @PutMapping("/{id}")
  public CalculationResponse rerun(
      @PathVariable UUID id,
      @Valid @RequestBody CalculationCreateRequest req
  ) {
    return service.rerun(id, req);
  }

  @DeleteMapping("/{id}")
  public void disable(@PathVariable UUID id) {
    service.disable(id);
  }

  @PostMapping("/{id}/enable")
  public void enable(@PathVariable UUID id) {
    service.enable(id);
  }
}
