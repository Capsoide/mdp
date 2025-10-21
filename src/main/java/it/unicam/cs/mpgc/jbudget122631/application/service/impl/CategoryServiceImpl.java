package it.unicam.cs.mpgc.jbudget122631.application.service.impl;

import it.unicam.cs.mpgc.jbudget122631.application.service.CategoryService;
import it.unicam.cs.mpgc.jbudget122631.domain.model.Category;
import it.unicam.cs.mpgc.jbudget122631.domain.repository.CategoryRepository;

import java.util.List;
import java.util.Optional;

public class CategoryServiceImpl implements CategoryService {

    private static final int MAX_CATEGORY_NAME_LENGTH = 100;
    private static final String CATEGORY_NOT_FOUND_MESSAGE = "Categoria non trovata";
    private static final String PARENT_CATEGORY_NOT_FOUND_MESSAGE = "Categoria parent non trovata";

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public Category createCategory(String name, String description, Long parentId) {
        validateCategoryName(name);

        Category parent = resolveParentCategory(parentId);
        validateUniqueNameInParent(name, parent);

        Category category = new Category(name, description);

        if (parent != null) {
            establishParentChildRelationship(parent, category);
        }

        return categoryRepository.save(category);
    }

    @Override
    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public Category updateCategory(Long id, String name, String description) {
        Category category = findCategoryById(id);

        validateCategoryName(name);
        validateUniqueNameForUpdate(category, name);

        updateCategoryFields(category, name, description);

        return categoryRepository.save(category);
    }

    @Override
    public void deleteCategory(Long id) {
        if (canDeleteCategory(id)) {
            throw new IllegalArgumentException("Impossibile eliminare categoria: ha figli o movimenti associati");
        }
        categoryRepository.deleteById(id);
    }

    @Override
    public List<Category> getRootCategories() {
        return categoryRepository.findRootCategories();
    }

    @Override
    public List<Category> getChildCategories(Long parentId) {
        Category parent = findCategoryById(parentId, PARENT_CATEGORY_NOT_FOUND_MESSAGE);
        return categoryRepository.findByParent(parent);
    }

    @Override
    public List<Category> getCategoryPath(Long categoryId) {
        Category category = findCategoryById(categoryId);
        return category.getPath();
    }

    @Override
    public List<Category> getAllDescendants(Long categoryId) {
        Category category = findCategoryById(categoryId);
        return List.copyOf(category.getAllDescendants());
    }

    @Override
    public List<Category> getActiveCategories() {
        return categoryRepository.findActiveCategories();
    }

    @Override
    public void deactivateCategory(Long id) {
        Category category = findCategoryById(id);
        updateCategoryActiveStatus(category, false);
    }

    @Override
    public void activateCategory(Long id) {
        Category category = findCategoryById(id);
        updateCategoryActiveStatus(category, true);
    }

    @Override
    public boolean canDeleteCategory(Long id) {
        Category category = findCategoryById(id);

        // Verifica presenza di sottocategorie
        if (hasChildCategories(category)) {
            return true; // Non può essere eliminata
        }

        return false; // Può essere eliminata
    }

    @Override
    public boolean categoryNameExistsInParent(String name, Long parentId) {
        Category parent = parentId != null ? categoryRepository.findById(parentId).orElse(null) : null;
        return categoryRepository.existsByNameAndParent(name, parent);
    }

    private Category findCategoryById(Long id) {
        return findCategoryById(id, CATEGORY_NOT_FOUND_MESSAGE);
    }

    private Category findCategoryById(Long id, String errorMessage) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(errorMessage + " con ID: " + id));
    }

    private Category resolveParentCategory(Long parentId) {
        if (parentId == null) {
            return null;
        }
        return findCategoryById(parentId, PARENT_CATEGORY_NOT_FOUND_MESSAGE);
    }

    private void establishParentChildRelationship(Category parent, Category child) {
        parent.addChild(child);
        categoryRepository.save(parent); // Salva per aggiornare la relazione
    }

    private void updateCategoryFields(Category category, String name, String description) {
        category.setName(name);
        category.setDescription(description);
    }

    private void updateCategoryActiveStatus(Category category, boolean active) {
        category.setActive(active);
        categoryRepository.save(category);
    }

    private boolean hasChildCategories(Category category) {
        return !category.getChildren().isEmpty();
    }

    private void validateCategoryName(String name) {
        if (isNullOrEmpty(name)) {
            throw new IllegalArgumentException("Nome categoria richiesto");
        }

        if (isTooLong(name)) {
            throw new IllegalArgumentException(
                    String.format("Nome categoria troppo lungo (max %d caratteri)", MAX_CATEGORY_NAME_LENGTH)
            );
        }
    }

    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    private boolean isTooLong(String str) {
        return str.length() > MAX_CATEGORY_NAME_LENGTH;
    }

    private void validateUniqueNameInParent(String name, Category parent) {
        if (categoryRepository.existsByNameAndParent(name, parent)) {
            String parentInfo = parent != null ? "nella categoria '" + parent.getName() + "'" : "tra le categorie radice";
            throw new IllegalArgumentException("Categoria '" + name + "' gia' esistente " + parentInfo);
        }
    }

    private void validateUniqueNameForUpdate(Category category, String newName) {
        // Se il nome non cambia, non serve verificare l'unicità
        if (nameUnchanged(category, newName)) {
            return;
        }

        validateUniqueNameInParent(newName, category.getParent());
    }

    private boolean nameUnchanged(Category category, String newName) {
        return category.getName().equalsIgnoreCase(newName);
    }
}