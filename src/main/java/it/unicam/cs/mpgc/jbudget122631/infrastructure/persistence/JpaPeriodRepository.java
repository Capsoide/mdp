package it.unicam.cs.mpgc.jbudget122631.infrastructure.persistence;

import it.unicam.cs.mpgc.jbudget122631.domain.model.Period;
import it.unicam.cs.mpgc.jbudget122631.domain.repository.PeriodRepository;
import it.unicam.cs.mpgc.jbudget122631.infrastructure.config.HibernateConfig;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class JpaPeriodRepository implements PeriodRepository {

    private final SessionFactory sessionFactory;

    public JpaPeriodRepository() {
        this.sessionFactory = HibernateConfig.getSessionFactory();
    }

    @Override
    public Period save(Period period) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.saveOrUpdate(period);
            transaction.commit();
            return period;
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Errore salvataggio periodo", e);
        }
    }

    @Override
    public Optional<Period> findById(Long id) {
        try (Session session = sessionFactory.openSession()) {
            Period period = session.get(Period.class, id);
            return Optional.ofNullable(period);
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca periodo per ID", e);
        }
    }

    @Override
    public List<Period> findAll() {
        try (Session session = sessionFactory.openSession()) {
            Query<Period> query = session.createQuery(
                    "FROM Period ORDER BY startDate DESC", Period.class);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Errore recupero tutti i periodi", e);
        }
    }

    @Override
    public void delete(Period period) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.delete(period);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Errore eliminazione periodo", e);
        }
    }

    @Override
    public void deleteById(Long id) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            Period period = session.get(Period.class, id);
            if (period != null) {
                session.delete(period);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Errore eliminazione periodo per ID", e);
        }
    }

    @Override
    public Optional<Period> findByName(String name) {
        try (Session session = sessionFactory.openSession()) {
            Query<Period> query = session.createQuery(
                    "FROM Period WHERE LOWER(name) = LOWER(:name)", Period.class);
            query.setParameter("name", name);
            List<Period> results = query.getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca periodo per nome", e);
        }
    }

    @Override
    public List<Period> findByDateRange(LocalDate startDate, LocalDate endDate) {
        try (Session session = sessionFactory.openSession()) {
            Query<Period> query = session.createQuery(
                    "FROM Period WHERE startDate <= :endDate AND endDate >= :startDate ORDER BY startDate ASC",
                    Period.class);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca periodi per intervallo", e);
        }
    }

    @Override
    public Optional<Period> findPeriodContaining(LocalDate date) {
        try (Session session = sessionFactory.openSession()) {
            Query<Period> query = session.createQuery(
                    "FROM Period WHERE startDate <= :date AND endDate >= :date", Period.class);
            query.setParameter("date", date);
            List<Period> results = query.getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca periodo contenente data", e);
        }
    }

    @Override
    public List<Period> findOverlappingPeriods(LocalDate startDate, LocalDate endDate) {
        return findByDateRange(startDate, endDate);
    }

    @Override
    public List<Period> findCurrentPeriods() {
        LocalDate today = LocalDate.now();
        try (Session session = sessionFactory.openSession()) {
            Query<Period> query = session.createQuery(
                    "FROM Period WHERE startDate <= :today AND endDate >= :today ORDER BY startDate ASC",
                    Period.class);
            query.setParameter("today", today);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca periodi correnti", e);
        }
    }

    @Override
    public List<Period> findFuturePeriods() {
        LocalDate today = LocalDate.now();
        try (Session session = sessionFactory.openSession()) {
            Query<Period> query = session.createQuery(
                    "FROM Period WHERE startDate > :today ORDER BY startDate ASC", Period.class);
            query.setParameter("today", today);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca periodi futuri", e);
        }
    }

    @Override
    public List<Period> findPastPeriods() {
        LocalDate today = LocalDate.now();
        try (Session session = sessionFactory.openSession()) {
            Query<Period> query = session.createQuery(
                    "FROM Period WHERE endDate < :today ORDER BY startDate DESC", Period.class);
            query.setParameter("today", today);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca periodi passati", e);
        }
    }

    @Override
    public long count() {
        try (Session session = sessionFactory.openSession()) {
            Query<Long> query = session.createQuery("SELECT COUNT(*) FROM Period", Long.class);
            return query.getSingleResult();
        } catch (Exception e) {
            throw new RuntimeException("Errore conteggio periodi", e);
        }
    }
}