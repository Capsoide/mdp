package it.unicam.cs.mpgc.jbudget122631.domain.repository;

import it.unicam.cs.mpgc.jbudget122631.domain.model.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {

    // CRUD operations
    Category save(Category category);
    Optional<Category> findById(Long id);
    List<Category> findAll();
    void delete(Category category);
    void deleteById(Long id);

    // Query methods
    List<Category> findByName(String name);
    List<Category> findByParent(Category parent);
    List<Category> findRootCategories(); // parent is null
    List<Category> findActiveCategories();

    // Hierarchy queries
    List<Category> findDescendants(Category category);
    List<Category> findAncestors(Category category);
    boolean existsByNameAndParent(String name, Category parent);

    long count();
}