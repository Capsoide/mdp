package it.unicam.cs.mpgc.jbudget122631.infrastructure.persistence;

import it.unicam.cs.mpgc.jbudget122631.domain.model.Category;
import it.unicam.cs.mpgc.jbudget122631.domain.repository.CategoryRepository;
import it.unicam.cs.mpgc.jbudget122631.infrastructure.config.HibernateConfig;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;
import java.util.Optional;

public class JpaCategoryRepository implements CategoryRepository {

    private final SessionFactory sessionFactory;

    public JpaCategoryRepository() {
        this.sessionFactory = HibernateConfig.getSessionFactory();
    }

    @Override
    public Category save(Category category) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.saveOrUpdate(category);
            transaction.commit();
            return category;
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Errore salvataggio categoria", e);
        }
    }

    @Override
    public Optional<Category> findById(Long id) {
        try (Session session = sessionFactory.openSession()) {
            Category category = session.get(Category.class, id);
            return Optional.ofNullable(category);
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca categoria per ID", e);
        }
    }

    @Override
    public List<Category> findAll() {
        try (Session session = sessionFactory.openSession()) {
            Query<Category> query = session.createQuery("FROM Category ORDER BY name", Category.class);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Errore recupero tutte le categorie", e);
        }
    }

    @Override
    public void delete(Category category) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.delete(category);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Errore eliminazione categoria", e);
        }
    }

    @Override
    public void deleteById(Long id) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            Category category = session.get(Category.class, id);
            if (category != null) {
                session.delete(category);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Errore eliminazione categoria per ID", e);
        }
    }

    @Override
    public List<Category> findByName(String name) {
        try (Session session = sessionFactory.openSession()) {
            Query<Category> query = session.createQuery(
                    "FROM Category WHERE LOWER(name) = LOWER(:name)", Category.class);
            query.setParameter("name", name);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca categorie per nome", e);
        }
    }

    @Override
    public List<Category> findByParent(Category parent) {
        try (Session session = sessionFactory.openSession()) {
            Query<Category> query = session.createQuery(
                    "FROM Category WHERE parent = :parent ORDER BY name", Category.class);
            query.setParameter("parent", parent);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca categorie per parent", e);
        }
    }

    @Override
    public List<Category> findRootCategories() {
        try (Session session = sessionFactory.openSession()) {
            Query<Category> query = session.createQuery(
                    "FROM Category WHERE parent IS NULL ORDER BY name", Category.class);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca categorie root", e);
        }
    }

    @Override
    public List<Category> findActiveCategories() {
        try (Session session = sessionFactory.openSession()) {
            Query<Category> query = session.createQuery(
                    "FROM Category WHERE active = true ORDER BY name", Category.class);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca categorie attive", e);
        }
    }

    @Override
    public List<Category> findDescendants(Category category) {
        // Implementazione ricorsiva - per semplicità, restituiamo solo i figli diretti
        // In un'implementazione completa, si potrebbero usare CTE o query ricorsive
        return findByParent(category);
    }

    @Override
    public List<Category> findAncestors(Category category) {
        // Implementazione semplificata - restituisce il path verso la root
        try (Session session = sessionFactory.openSession()) {
            // In un'implementazione completa, si userebbe una query ricorsiva
            // Per ora restituiamo solo il parent diretto
            if (category.getParent() != null) {
                return List.of(category.getParent());
            }
            return List.of();
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca antenati categoria", e);
        }
    }

    @Override
    public boolean existsByNameAndParent(String name, Category parent) {
        try (Session session = sessionFactory.openSession()) {
            Query<Long> query;
            if (parent == null) {
                query = session.createQuery(
                        "SELECT COUNT(*) FROM Category WHERE LOWER(name) = LOWER(:name) AND parent IS NULL",
                        Long.class);
                query.setParameter("name", name);
            } else {
                query = session.createQuery(
                        "SELECT COUNT(*) FROM Category WHERE LOWER(name) = LOWER(:name) AND parent = :parent",
                        Long.class);
                query.setParameter("name", name);
                query.setParameter("parent", parent);
            }
            return query.getSingleResult() > 0;
        } catch (Exception e) {
            throw new RuntimeException("Errore verifica esistenza categoria", e);
        }
    }

    @Override
    public long count() {
        try (Session session = sessionFactory.openSession()) {
            Query<Long> query = session.createQuery("SELECT COUNT(*) FROM Category", Long.class);
            return query.getSingleResult();
        } catch (Exception e) {
            throw new RuntimeException("Errore conteggio categorie", e);
        }
    }
}