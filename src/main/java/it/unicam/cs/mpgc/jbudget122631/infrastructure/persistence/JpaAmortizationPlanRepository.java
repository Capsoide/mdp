package it.unicam.cs.mpgc.jbudget122631.infrastructure.persistence;

import it.unicam.cs.mpgc.jbudget122631.domain.model.AmortizationPlan;
import it.unicam.cs.mpgc.jbudget122631.domain.repository.AmortizationPlanRepository;
import it.unicam.cs.mpgc.jbudget122631.infrastructure.config.HibernateConfig;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;
import java.util.Optional;

public class JpaAmortizationPlanRepository implements AmortizationPlanRepository {

    private final SessionFactory sessionFactory;

    public JpaAmortizationPlanRepository() {
        this.sessionFactory = HibernateConfig.getSessionFactory();
    }

    @Override
    public AmortizationPlan save(AmortizationPlan plan) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.saveOrUpdate(plan);
            transaction.commit();
            return plan;
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Errore salvataggio piano ammortamento", e);
        }
    }

    @Override
    public Optional<AmortizationPlan> findById(Long id) {
        try (Session session = sessionFactory.openSession()) {
            AmortizationPlan plan = session.get(AmortizationPlan.class, id);
            return Optional.ofNullable(plan);
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca piano ammortamento per ID", e);
        }
    }

    @Override
    public List<AmortizationPlan> findAll() {
        try (Session session = sessionFactory.openSession()) {
            Query<AmortizationPlan> query = session.createQuery(
                    "FROM AmortizationPlan ORDER BY startDate DESC", AmortizationPlan.class);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Errore recupero tutti i piani ammortamento", e);
        }
    }

    @Override
    public void delete(AmortizationPlan plan) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.delete(plan);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Errore eliminazione piano ammortamento", e);
        }
    }

    @Override
    public void deleteById(Long id) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            AmortizationPlan plan = session.get(AmortizationPlan.class, id);
            if (plan != null) {
                session.delete(plan);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Errore eliminazione piano ammortamento per ID", e);
        }
    }

    @Override
    public Optional<AmortizationPlan> findByName(String name) {
        try (Session session = sessionFactory.openSession()) {
            Query<AmortizationPlan> query = session.createQuery(
                    "FROM AmortizationPlan WHERE LOWER(name) = LOWER(:name)", AmortizationPlan.class);
            query.setParameter("name", name);
            List<AmortizationPlan> results = query.getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca piano per nome", e);
        }
    }

    @Override
    public List<AmortizationPlan> findActivePlans() {
        try (Session session = sessionFactory.openSession()) {
            Query<AmortizationPlan> query = session.createQuery(
                    "FROM AmortizationPlan WHERE active = true ORDER BY startDate DESC", AmortizationPlan.class);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca piani attivi", e);
        }
    }

    @Override
    public List<AmortizationPlan> findCompletedPlans() {
        try (Session session = sessionFactory.openSession()) {
            Query<AmortizationPlan> query = session.createQuery(
                    "FROM AmortizationPlan WHERE active = false ORDER BY startDate DESC", AmortizationPlan.class);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca piani completati", e);
        }
    }

    @Override
    public long count() {
        try (Session session = sessionFactory.openSession()) {
            Query<Long> query = session.createQuery("SELECT COUNT(*) FROM AmortizationPlan", Long.class);
            return query.getSingleResult();
        } catch (Exception e) {
            throw new RuntimeException("Errore conteggio piani ammortamento", e);
        }
    }
}