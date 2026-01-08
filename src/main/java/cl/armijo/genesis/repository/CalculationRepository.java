package cl.armijo.genesis.repository;

import cl.armijo.genesis.model.entity.Calculation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CalculationRepository extends JpaRepository<Calculation, UUID> {
  Page<Calculation> findByDisabledFalse(Pageable pageable);
  Optional<Calculation> findByIdAndDisabledFalse(UUID id);
}
