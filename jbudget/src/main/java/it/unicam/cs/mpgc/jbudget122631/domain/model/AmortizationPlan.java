package it.unicam.cs.mpgc.jbudget122631.domain.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Entity
@Table(name = "amortization_plans")
public class AmortizationPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal interestRate;

    @Column(nullable = false)
    private int numberOfInstallments;

    @Column(nullable = false)
    private LocalDate startDate;

    @OneToMany(mappedBy = "amortizationPlan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("date ASC")
    private List<Movement> installments = new ArrayList<>();

    @Column(nullable = false)
    private boolean active = true;

    // Costruttori
    protected AmortizationPlan() {} // JPA

    public AmortizationPlan(String name, BigDecimal totalAmount,
                            BigDecimal interestRate, int numberOfInstallments,
                            LocalDate startDate) {
        this.name = Objects.requireNonNull(name);
        this.totalAmount = validatePositiveAmount(totalAmount);
        this.interestRate = validateInterestRate(interestRate);
        this.numberOfInstallments = validateInstallments(numberOfInstallments);
        this.startDate = Objects.requireNonNull(startDate);
    }

    private BigDecimal validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Importo deve essere > 0");
        }
        return amount;
    }

    private BigDecimal validateInterestRate(BigDecimal rate) {
        if (rate == null || rate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Tasso interesse deve essere >= 0");
        }
        return rate;
    }

    private int validateInstallments(int installments) {
        if (installments <= 0) {
            throw new IllegalArgumentException("Numero rate deve essere > 0");
        }
        return installments;
    }

    public void generateInstallments() {
        installments.clear();

        BigDecimal monthlyRate = interestRate.divide(BigDecimal.valueOf(12), 6,
                BigDecimal.ROUND_HALF_UP);
        BigDecimal monthlyPayment = calculateMonthlyPayment(monthlyRate);

        BigDecimal remainingPrincipal = totalAmount;
        LocalDate currentDate = startDate;

        for (int i = 1; i <= numberOfInstallments; i++) {
            BigDecimal interestPayment = remainingPrincipal.multiply(monthlyRate)
                    .setScale(2, BigDecimal.ROUND_HALF_UP);
            BigDecimal principalPayment = monthlyPayment.subtract(interestPayment);

            // Ultima rata: aggiusta per differenze di arrotondamento
            if (i == numberOfInstallments) {
                principalPayment = remainingPrincipal;
                monthlyPayment = principalPayment.add(interestPayment);
            }

            Movement installment = new Movement(
                    String.format("%s - Rata %d/%d", name, i, numberOfInstallments),
                    monthlyPayment,
                    MovementType.EXPENSE,
                    currentDate
            );

            installment.setAmortizationPlan(this);
            installment.setScheduled(true);
            installment.setNotes(String.format("Capitale: %s, Interessi: %s",
                    principalPayment, interestPayment));

            installments.add(installment);

            remainingPrincipal = remainingPrincipal.subtract(principalPayment);
            currentDate = currentDate.plusMonths(1);
        }
    }

    private BigDecimal calculateMonthlyPayment(BigDecimal monthlyRate) {
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return totalAmount.divide(BigDecimal.valueOf(numberOfInstallments), 2,
                    BigDecimal.ROUND_HALF_UP);
        }

        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal onePlusRPowN = onePlusR.pow(numberOfInstallments);

        return totalAmount.multiply(monthlyRate)
                .multiply(onePlusRPowN)
                .divide(onePlusRPowN.subtract(BigDecimal.ONE), 2,
                        BigDecimal.ROUND_HALF_UP);
    }

    public BigDecimal getTotalInterest() {
        return installments.stream()
                .map(Movement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .subtract(totalAmount);
    }

    public int getCompletedInstallments() {
        LocalDate today = LocalDate.now();
        return (int) installments.stream()
                .filter(m -> !m.getDate().isAfter(today))
                .count();
    }

    public BigDecimal getRemainingAmount() {
        LocalDate today = LocalDate.now();
        return installments.stream()
                .filter(m -> m.getDate().isAfter(today))
                .map(Movement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getInterestRate() { return interestRate; }
    public int getNumberOfInstallments() { return numberOfInstallments; }
    public LocalDate getStartDate() { return startDate; }
    public List<Movement> getInstallments() { return new ArrayList<>(installments); }
    public boolean isActive() { return active; }

    public void setActive(boolean active) { this.active = active; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AmortizationPlan)) return false;
        AmortizationPlan that = (AmortizationPlan) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(startDate, that.startDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, startDate);
    }
}