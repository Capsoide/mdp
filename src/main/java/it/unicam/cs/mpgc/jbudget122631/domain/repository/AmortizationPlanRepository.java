package it.unicam.cs.mpgc.jbudget122631.domain.repository;

import it.unicam.cs.mpgc.jbudget122631.domain.model.AmortizationPlan;

import java.util.List;
import java.util.Optional;

public interface AmortizationPlanRepository {

    AmortizationPlan save(AmortizationPlan plan);
    Optional<AmortizationPlan> findById(Long id);
    List<AmortizationPlan> findAll();
    void delete(AmortizationPlan plan);
    void deleteById(Long id);

    Optional<AmortizationPlan> findByName(String name);
    List<AmortizationPlan> findActivePlans();
    List<AmortizationPlan> findCompletedPlans();

    long count();
}