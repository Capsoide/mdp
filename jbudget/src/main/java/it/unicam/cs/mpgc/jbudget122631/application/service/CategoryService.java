package it.unicam.cs.mpgc.jbudget122631.application.service;

import it.unicam.cs.mpgc.jbudget122631.domain.model.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryService {

    Category createCategory(String name, String description, Long parentId);
    Optional<Category> getCategoryById(Long id);
    List<Category> getAllCategories();
    Category updateCategory(Long id, String name, String description);
    void deleteCategory(Long id);

    List<Category> getRootCategories();
    List<Category> getChildCategories(Long parentId);
    List<Category> getCategoryPath(Long categoryId);
    List<Category> getAllDescendants(Long categoryId);

    List<Category> getActiveCategories();
    void deactivateCategory(Long id);
    void activateCategory(Long id);

    boolean canDeleteCategory(Long id);
    boolean categoryNameExistsInParent(String name, Long parentId);
}