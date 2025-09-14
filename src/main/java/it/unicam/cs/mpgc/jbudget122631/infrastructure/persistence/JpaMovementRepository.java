package it.unicam.cs.mpgc.jbudget122631.infrastructure.persistence;

import it.unicam.cs.mpgc.jbudget122631.domain.model.*;
import it.unicam.cs.mpgc.jbudget122631.domain.repository.MovementRepository;
import it.unicam.cs.mpgc.jbudget122631.infrastructure.config.HibernateConfig;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class JpaMovementRepository implements MovementRepository {

    private final SessionFactory sessionFactory;

    public JpaMovementRepository() {
        this.sessionFactory = HibernateConfig.getSessionFactory();
    }

    @Override
    public Movement save(Movement movement) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();

            System.out.println("REPOSITORY - Salvando movement con " + movement.getCategories().size() + " categorie");

            // Salva o aggiorna il movimento
            session.saveOrUpdate(movement);
            session.flush(); // Forza il salvataggio immediato

            transaction.commit();

            System.out.println("REPOSITORY - Movement salvato con ID: " + movement.getId());

            // IMPORTANTE: Ricarica il movimento con le categorie dal database
            return findById(movement.getId()).orElse(movement);

        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            System.err.println("REPOSITORY - Errore salvataggio: " + e.getMessage());
            throw new RuntimeException("Errore salvataggio movimento", e);
        }
    }

    @Override
    public Optional<Movement> findById(Long id) {
        try (Session session = sessionFactory.openSession()) {
            // Carica anche le categorie in modo eager
            Query<Movement> query = session.createQuery(
                    "SELECT m FROM Movement m LEFT JOIN FETCH m.categories WHERE m.id = :id",
                    Movement.class);
            query.setParameter("id", id);
            List<Movement> results = query.getResultList();

            if (!results.isEmpty()) {
                Movement movement = results.get(0);
                System.out.println("REPOSITORY - Movement trovato con " + movement.getCategories().size() + " categorie");
                return Optional.of(movement);
            }
            return Optional.empty();
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca movimento per ID", e);
        }
    }

    @Override
    public List<Movement> findAll() {
        try (Session session = sessionFactory.openSession()) {
            // Usa LEFT JOIN FETCH per caricare le categorie in modo eager
            Query<Movement> query = session.createQuery(
                    "SELECT DISTINCT m FROM Movement m LEFT JOIN FETCH m.categories ORDER BY m.date DESC",
                    Movement.class);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Errore recupero tutti i movimenti", e);
        }
    }

    @Override
    public void delete(Movement movement) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.delete(movement);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Errore eliminazione movimento", e);
        }
    }

    @Override
    public void deleteById(Long id) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            Movement movement = session.get(Movement.class, id);
            if (movement != null) {
                session.delete(movement);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Errore eliminazione movimento per ID", e);
        }
    }

    @Override
    public List<Movement> findByDateBetween(LocalDate startDate, LocalDate endDate) {
        try (Session session = sessionFactory.openSession()) {
            Query<Movement> query = session.createQuery(
                    "SELECT DISTINCT m FROM Movement m LEFT JOIN FETCH m.categories " +
                            "WHERE m.date BETWEEN :startDate AND :endDate ORDER BY m.date DESC",
                    Movement.class);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca movimenti per intervallo date", e);
        }
    }

    @Override
    public List<Movement> findByType(MovementType type) {
        try (Session session = sessionFactory.openSession()) {
            Query<Movement> query = session.createQuery(
                    "SELECT DISTINCT m FROM Movement m LEFT JOIN FETCH m.categories WHERE m.type = :type ORDER BY m.date DESC",
                    Movement.class);
            query.setParameter("type", type);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca movimenti per tipo", e);
        }
    }

    @Override
    public List<Movement> findByCategory(Category category) {
        try (Session session = sessionFactory.openSession()) {
            Query<Movement> query = session.createQuery(
                    "SELECT DISTINCT m FROM Movement m JOIN FETCH m.categories c WHERE c = :category ORDER BY m.date DESC",
                    Movement.class);
            query.setParameter("category", category);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca movimenti per categoria", e);
        }
    }

    @Override
    public List<Movement> findByCategoriesContaining(Category category) {
        return findByCategory(category); // Stessa implementazione
    }

    @Override
    public List<Movement> findByPeriod(Period period) {
        return findByDateBetween(period.getStartDate(), period.getEndDate());
    }

    @Override
    public BigDecimal getTotalByTypeAndDateRange(MovementType type, LocalDate startDate, LocalDate endDate) {
        try (Session session = sessionFactory.openSession()) {
            Query<BigDecimal> query = session.createQuery(
                    "SELECT COALESCE(SUM(amount), 0) FROM Movement WHERE type = :type AND date BETWEEN :startDate AND :endDate",
                    BigDecimal.class);
            query.setParameter("type", type);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            return query.getSingleResult();
        } catch (Exception e) {
            throw new RuntimeException("Errore calcolo totale per tipo e intervallo", e);
        }
    }

    @Override
    public BigDecimal getTotalByCategoryAndDateRange(Category category, LocalDate startDate, LocalDate endDate) {
        try (Session session = sessionFactory.openSession()) {
            Query<BigDecimal> query = session.createQuery(
                    "SELECT COALESCE(SUM(m.amount), 0) FROM Movement m JOIN m.categories c " +
                            "WHERE c = :category AND m.date BETWEEN :startDate AND :endDate",
                    BigDecimal.class);
            query.setParameter("category", category);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            return query.getSingleResult();
        } catch (Exception e) {
            throw new RuntimeException("Errore calcolo totale per categoria e intervallo", e);
        }
    }

    @Override
    public List<Movement> findByDescriptionContaining(String description) {
        try (Session session = sessionFactory.openSession()) {
            Query<Movement> query = session.createQuery(
                    "SELECT DISTINCT m FROM Movement m LEFT JOIN FETCH m.categories WHERE LOWER(m.description) LIKE LOWER(:description) ORDER BY m.date DESC",
                    Movement.class);
            query.setParameter("description", "%" + description + "%");
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca movimenti per descrizione", e);
        }
    }

    @Override
    public List<Movement> findScheduledMovements() {
        try (Session session = sessionFactory.openSession()) {
            Query<Movement> query = session.createQuery(
                    "SELECT DISTINCT m FROM Movement m LEFT JOIN FETCH m.categories WHERE m.scheduled = true ORDER BY m.date DESC",
                    Movement.class);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca movimenti programmati", e);
        }
    }

    @Override
    public List<Movement> findByAmortizationPlanId(Long planId) {
        try (Session session = sessionFactory.openSession()) {
            Query<Movement> query = session.createQuery(
                    "SELECT DISTINCT m FROM Movement m LEFT JOIN FETCH m.categories WHERE m.amortizationPlan.id = :planId ORDER BY m.date ASC",
                    Movement.class);
            query.setParameter("planId", planId);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca movimenti per piano ammortamento", e);
        }
    }

    @Override
    public List<Movement> findAllPaginated(int page, int size) {
        try (Session session = sessionFactory.openSession()) {
            Query<Movement> query = session.createQuery(
                    "SELECT DISTINCT m FROM Movement m LEFT JOIN FETCH m.categories ORDER BY m.date DESC",
                    Movement.class);
            query.setFirstResult(page * size);
            query.setMaxResults(size);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Errore paginazione movimenti", e);
        }
    }

    @Override
    public long count() {
        try (Session session = sessionFactory.openSession()) {
            Query<Long> query = session.createQuery("SELECT COUNT(*) FROM Movement", Long.class);
            return query.getSingleResult();
        } catch (Exception e) {
            throw new RuntimeException("Errore conteggio movimenti", e);
        }
    }
}