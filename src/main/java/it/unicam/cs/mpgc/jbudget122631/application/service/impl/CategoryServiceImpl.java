package it.unicam.cs.mpgc.jbudget122631.application.service.impl;

import it.unicam.cs.mpgc.jbudget122631.application.service.CategoryService;
import it.unicam.cs.mpgc.jbudget122631.domain.model.Category;
import it.unicam.cs.mpgc.jbudget122631.domain.repository.CategoryRepository;

import java.util.List;
import java.util.Optional;

/**
 * Implementazione del servizio per la gestione delle categorie.
 *
 * Gestisce la logica di business per:
 * - Creazione, modifica ed eliminazione di categorie
 * - Gestione della gerarchia padre-figlio
 * - Validazione dell'unicità dei nomi per parent
 * - Attivazione/disattivazione categorie
 * - Navigazione nella struttura gerarchica
 *
 * Le categorie possono essere organizzate in struttura ad albero con:
 * - Categorie radice (senza parent)
 * - Sottocategorie con relazioni parent-child
 * - Validazione unicità nomi all'interno dello stesso livello gerarchico
 *
 * @author Nicola Capancioni
 * @version 1.0
 */
public class CategoryServiceImpl implements CategoryService {

    private static final int MAX_CATEGORY_NAME_LENGTH = 100;
    private static final String CATEGORY_NOT_FOUND_MESSAGE = "Categoria non trovata";
    private static final String PARENT_CATEGORY_NOT_FOUND_MESSAGE = "Categoria parent non trovata";

    private final CategoryRepository categoryRepository;

    /**
     * Costruttore per l'iniezione delle dipendenze.
     *
     * @param categoryRepository repository per la persistenza delle categorie
     */
    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Crea una nuova categoria con validazione del nome e gestione della gerarchia.
     *
     * @param name nome della categoria (obbligatorio, max 100 caratteri)
     * @param description descrizione opzionale della categoria
     * @param parentId ID della categoria parent (null per categorie radice)
     * @return la categoria creata e salvata
     * @throws IllegalArgumentException se il nome non è valido o già esistente nel parent
     * @throws RuntimeException se la categoria parent non esiste
     */
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

    /**
     * Aggiorna una categoria esistente con validazione del nome.
     *
     * @param id ID della categoria da aggiornare
     * @param name nuovo nome (deve essere univoco nel parent)
     * @param description nuova descrizione
     * @return la categoria aggiornata
     * @throws RuntimeException se la categoria non esiste
     * @throws IllegalArgumentException se il nome non è valido o già esistente
     */
    @Override
    public Category updateCategory(Long id, String name, String description) {
        Category category = findCategoryById(id);

        validateCategoryName(name);
        validateUniqueNameForUpdate(category, name);

        updateCategoryFields(category, name, description);

        return categoryRepository.save(category);
    }

    /**
     * Elimina una categoria dopo aver verificato che sia eliminabile.
     *
     * Una categoria può essere eliminata solo se:
     * - Non ha sottocategorie figlio
     * - Non ha movimenti associati
     *
     * @param id ID della categoria da eliminare
     * @throws IllegalArgumentException se la categoria non può essere eliminata
     */
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

    /**
     * Recupera tutte le sottocategorie dirette di una categoria parent.
     *
     * @param parentId ID della categoria parent
     * @return lista delle sottocategorie figlio
     * @throws RuntimeException se la categoria parent non esiste
     */
    @Override
    public List<Category> getChildCategories(Long parentId) {
        Category parent = findCategoryById(parentId, PARENT_CATEGORY_NOT_FOUND_MESSAGE);
        return categoryRepository.findByParent(parent);
    }

    /**
     * Recupera il percorso completo dalla radice alla categoria specificata.
     *
     * @param categoryId ID della categoria
     * @return lista ordinata delle categorie dal root alla categoria target
     * @throws RuntimeException se la categoria non esiste
     */
    @Override
    public List<Category> getCategoryPath(Long categoryId) {
        Category category = findCategoryById(categoryId);
        return category.getPath();
    }

    /**
     * Recupera tutti i discendenti di una categoria (figli, nipoti, etc.).
     *
     * @param categoryId ID della categoria radice
     * @return lista di tutti i discendenti
     * @throws RuntimeException se la categoria non esiste
     */
    @Override
    public List<Category> getAllDescendants(Long categoryId) {
        Category category = findCategoryById(categoryId);
        return List.copyOf(category.getAllDescendants());
    }

    @Override
    public List<Category> getActiveCategories() {
        return categoryRepository.findActiveCategories();
    }

    /**
     * Disattiva una categoria mantenendola nel database ma escludendola dalle operazioni.
     *
     * @param id ID della categoria da disattivare
     * @throws RuntimeException se la categoria non esiste
     */
    @Override
    public void deactivateCategory(Long id) {
        Category category = findCategoryById(id);
        updateCategoryActiveStatus(category, false);
    }

    /**
     * Riattiva una categoria precedentemente disattivata.
     *
     * @param id ID della categoria da riattivare
     * @throws RuntimeException se la categoria non esiste
     */
    @Override
    public void activateCategory(Long id) {
        Category category = findCategoryById(id);
        updateCategoryActiveStatus(category, true);
    }

    /**
     * Verifica se una categoria può essere eliminata in sicurezza.
     *
     * Criteri per l'eliminazione:
     * - Non deve avere sottocategorie figlio
     * - Non deve essere associata a movimenti (TODO: da implementare con MovementRepository)
     *
     * @param id ID della categoria da verificare
     * @return true se può essere eliminata, false altrimenti
     * @throws RuntimeException se la categoria non esiste
     */
    @Override
    public boolean canDeleteCategory(Long id) {
        Category category = findCategoryById(id);

        // Verifica presenza di sottocategorie
        if (hasChildCategories(category)) {
            return true; // Non può essere eliminata
        }

        // TODO: Implementare verifica movimenti associati
        // Richiede integrazione con MovementRepository
        // return hasAssociatedMovements(category);

        return false; // Può essere eliminata
    }

    /**
     * Verifica se esiste già una categoria con il nome specificato nel parent dato.
     *
     * @param name nome da verificare
     * @param parentId ID del parent (null per categorie radice)
     * @return true se il nome esiste già, false altrimenti
     */
    @Override
    public boolean categoryNameExistsInParent(String name, Long parentId) {
        Category parent = parentId != null ? categoryRepository.findById(parentId).orElse(null) : null;
        return categoryRepository.existsByNameAndParent(name, parent);
    }

    // === METODI PRIVATI DI SUPPORTO ===

    /**
     * Trova una categoria per ID o lancia eccezione se non esiste.
     *
     * @param id ID della categoria
     * @return la categoria trovata
     * @throws RuntimeException se la categoria non esiste
     */
    private Category findCategoryById(Long id) {
        return findCategoryById(id, CATEGORY_NOT_FOUND_MESSAGE);
    }

    /**
     * Trova una categoria per ID con messaggio di errore personalizzato.
     */
    private Category findCategoryById(Long id, String errorMessage) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(errorMessage + " con ID: " + id));
    }

    /**
     * Risolve la categoria parent dall'ID fornito.
     *
     * @param parentId ID del parent (può essere null)
     * @return categoria parent o null se parentId è null
     * @throws RuntimeException se il parent specificato non esiste
     */
    private Category resolveParentCategory(Long parentId) {
        if (parentId == null) {
            return null;
        }
        return findCategoryById(parentId, PARENT_CATEGORY_NOT_FOUND_MESSAGE);
    }

    /**
     * Stabilisce la relazione parent-child tra due categorie.
     */
    private void establishParentChildRelationship(Category parent, Category child) {
        parent.addChild(child);
        categoryRepository.save(parent); // Salva per aggiornare la relazione
    }

    /**
     * Aggiorna i campi di una categoria.
     */
    private void updateCategoryFields(Category category, String name, String description) {
        category.setName(name);
        category.setDescription(description);
    }

    /**
     * Aggiorna lo stato attivo/inattivo di una categoria.
     */
    private void updateCategoryActiveStatus(Category category, boolean active) {
        category.setActive(active);
        categoryRepository.save(category);
    }

    /**
     * Verifica se una categoria ha sottocategorie figlio.
     */
    private boolean hasChildCategories(Category category) {
        return !category.getChildren().isEmpty();
    }

    // === METODI DI VALIDAZIONE ===

    /**
     * Valida il nome di una categoria secondo le regole di business.
     *
     * Regole:
     * - Non può essere null o vuoto (dopo trim)
     * - Non può superare la lunghezza massima
     *
     * @param name nome da validare
     * @throws IllegalArgumentException se il nome non è valido
     */
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

    /**
     * Verifica se una stringa è null o vuota dopo trim.
     */
    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Verifica se una stringa supera la lunghezza massima consentita.
     */
    private boolean isTooLong(String str) {
        return str.length() > MAX_CATEGORY_NAME_LENGTH;
    }

    /**
     * Valida che il nome sia univoco nel parent specificato.
     *
     * @param name nome da verificare
     * @param parent categoria parent (null per root)
     * @throws IllegalArgumentException se il nome esiste già
     */
    private void validateUniqueNameInParent(String name, Category parent) {
        if (categoryRepository.existsByNameAndParent(name, parent)) {
            String parentInfo = parent != null ? "nella categoria '" + parent.getName() + "'" : "tra le categorie radice";
            throw new IllegalArgumentException("Categoria '" + name + "' già esistente " + parentInfo);
        }
    }

    /**
     * Valida unicità del nome durante un aggiornamento.
     * Esclude la categoria corrente dal controllo per permettere salvataggi senza modifiche al nome.
     *
     * @param category categoria in aggiornamento
     * @param newName nuovo nome proposto
     * @throws IllegalArgumentException se il nome è già utilizzato da un'altra categoria
     */
    private void validateUniqueNameForUpdate(Category category, String newName) {
        // Se il nome non cambia, non serve verificare l'unicità
        if (nameUnchanged(category, newName)) {
            return;
        }

        validateUniqueNameInParent(newName, category.getParent());
    }

    /**
     * Verifica se il nome proposto è uguale a quello attuale (case-insensitive).
     */
    private boolean nameUnchanged(Category category, String newName) {
        return category.getName().equalsIgnoreCase(newName);
    }
}