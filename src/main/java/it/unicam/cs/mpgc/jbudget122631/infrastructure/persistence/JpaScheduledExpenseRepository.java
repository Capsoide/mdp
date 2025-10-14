package it.unicam.cs.mpgc.jbudget122631.infrastructure.persistence;

import it.unicam.cs.mpgc.jbudget122631.domain.model.*;
import it.unicam.cs.mpgc.jbudget122631.domain.repository.ScheduledExpenseRepository;
import it.unicam.cs.mpgc.jbudget122631.infrastructure.config.HibernateConfig;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class JpaScheduledExpenseRepository implements ScheduledExpenseRepository {

    private final SessionFactory sessionFactory;

    public JpaScheduledExpenseRepository() {
        this.sessionFactory = HibernateConfig.getSessionFactory();
    }

    @Override
    public ScheduledExpense save(ScheduledExpense scheduledExpense) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.saveOrUpdate(scheduledExpense);
            transaction.commit();
            return scheduledExpense;
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Errore salvataggio spesa programmata", e);
        }
    }

    @Override
    public Optional<ScheduledExpense> findByCreatedMovement(Movement movement) {
        try (Session session = sessionFactory.openSession()) {
            Query<ScheduledExpense> query = session.createQuery(
                    "FROM ScheduledExpense se WHERE se.createdMovement = :movement",
                    ScheduledExpense.class);
            query.setParameter("movement", movement);
            return query.uniqueResultOptional();
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca spesa programmata per movimento creato", e);
        }
    }

    @Override
    public Optional<ScheduledExpense> findById(Long id) {
        try (Session session = sessionFactory.openSession()) {
            Query<ScheduledExpense> query = session.createQuery(
                    "FROM ScheduledExpense se LEFT JOIN FETCH se.categories WHERE se.id = :id",
                    ScheduledExpense.class);
            query.setParameter("id", id);
            return query.uniqueResultOptional();
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca spesa programmata per ID", e);
        }
    }

    @Override
    public List<ScheduledExpense> findAll() {
        try (Session session = sessionFactory.openSession()) {
            Query<ScheduledExpense> query = session.createQuery(
                    "FROM ScheduledExpense se LEFT JOIN FETCH se.categories ORDER BY se.dueDate ASC",
                    ScheduledExpense.class);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Errore recupero tutte le spese programmate", e);
        }
    }

    @Override
    public void delete(ScheduledExpense scheduledExpense) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.delete(scheduledExpense);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Errore eliminazione spesa programmata", e);
        }
    }

    @Override
    public void deleteById(Long id) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            ScheduledExpense expense = session.get(ScheduledExpense.class, id);
            if (expense != null) {
                session.delete(expense);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Errore eliminazione spesa programmata per ID", e);
        }
    }

    @Override
    public List<ScheduledExpense> findByDueDate(LocalDate dueDate) {
        try (Session session = sessionFactory.openSession()) {
            Query<ScheduledExpense> query = session.createQuery(
                    "FROM ScheduledExpense se LEFT JOIN FETCH se.categories WHERE se.dueDate = :dueDate",
                    ScheduledExpense.class);
            query.setParameter("dueDate", dueDate);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca spese per data scadenza", e);
        }
    }

    @Override
    public List<ScheduledExpense> findByDueDateBetween(LocalDate startDate, LocalDate endDate) {
        try (Session session = sessionFactory.openSession()) {
            Query<ScheduledExpense> query = session.createQuery(
                    "FROM ScheduledExpense se LEFT JOIN FETCH se.categories WHERE se.dueDate BETWEEN :startDate AND :endDate ORDER BY se.dueDate ASC",
                    ScheduledExpense.class);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca spese per intervallo date", e);
        }
    }

    @Override
    public List<ScheduledExpense> findOverdueExpenses() {
        try (Session session = sessionFactory.openSession()) {
            Query<ScheduledExpense> query = session.createQuery(
                    "FROM ScheduledExpense se LEFT JOIN FETCH se.categories WHERE se.dueDate < :today AND se.completed = false ORDER BY se.dueDate ASC",
                    ScheduledExpense.class);
            query.setParameter("today", LocalDate.now());
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca spese scadute", e);
        }
    }

    @Override
    public List<ScheduledExpense> findDueToday() {
        return findByDueDate(LocalDate.now());
    }

    @Override
    public List<ScheduledExpense> findDueThisWeek() {
        LocalDate startOfWeek = LocalDate.now();
        LocalDate endOfWeek = startOfWeek.plusDays(7);
        return findByDueDateBetween(startOfWeek, endOfWeek);
    }

    @Override
    public List<ScheduledExpense> findByCompleted(boolean completed) {
        try (Session session = sessionFactory.openSession()) {
            Query<ScheduledExpense> query = session.createQuery(
                    "FROM ScheduledExpense se LEFT JOIN FETCH se.categories WHERE se.completed = :completed ORDER BY se.dueDate ASC",
                    ScheduledExpense.class);
            query.setParameter("completed", completed);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca spese per stato completamento", e);
        }
    }

    @Override
    public List<ScheduledExpense> findActiveExpenses() {
        try (Session session = sessionFactory.openSession()) {
            Query<ScheduledExpense> query = session.createQuery(
                    "FROM ScheduledExpense se LEFT JOIN FETCH se.categories WHERE se.active = true ORDER BY se.dueDate ASC",
                    ScheduledExpense.class);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca spese attive", e);
        }
    }

    @Override
    public List<ScheduledExpense> findByRecurrenceType(RecurrenceType recurrenceType) {
        try (Session session = sessionFactory.openSession()) {
            Query<ScheduledExpense> query = session.createQuery(
                    "FROM ScheduledExpense se LEFT JOIN FETCH se.categories WHERE se.recurrenceType = :recurrenceType ORDER BY se.dueDate ASC",
                    ScheduledExpense.class);
            query.setParameter("recurrenceType", recurrenceType);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca spese per tipo ricorrenza", e);
        }
    }

    @Override
    public List<ScheduledExpense> findRecurringExpenses() {
        try (Session session = sessionFactory.openSession()) {
            Query<ScheduledExpense> query = session.createQuery(
                    "FROM ScheduledExpense se LEFT JOIN FETCH se.categories WHERE se.recurrenceType != :none ORDER BY se.dueDate ASC",
                    ScheduledExpense.class);
            query.setParameter("none", RecurrenceType.NONE);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca spese ricorrenti", e);
        }
    }

    @Override
    public List<ScheduledExpense> findByCategory(Category category) {
        try (Session session = sessionFactory.openSession()) {
            Query<ScheduledExpense> query = session.createQuery(
                    "SELECT DISTINCT se FROM ScheduledExpense se LEFT JOIN FETCH se.categories c WHERE c = :category ORDER BY se.dueDate ASC",
                    ScheduledExpense.class);
            query.setParameter("category", category);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca spese per categoria", e);
        }
    }

    @Override
    public long count() {
        try (Session session = sessionFactory.openSession()) {
            Query<Long> query = session.createQuery("SELECT COUNT(*) FROM ScheduledExpense", Long.class);
            return query.getSingleResult();
        } catch (Exception e) {
            throw new RuntimeException("Errore conteggio spese programmate", e);
        }
    }
}