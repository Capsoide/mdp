package it.unicam.cs.mpgc.jbudget122631.application.service.impl;

import it.unicam.cs.mpgc.jbudget122631.application.dto.MovementDTO;
import it.unicam.cs.mpgc.jbudget122631.application.service.MovementService;
import it.unicam.cs.mpgc.jbudget122631.application.service.BudgetService;
import it.unicam.cs.mpgc.jbudget122631.domain.model.Movement;
import it.unicam.cs.mpgc.jbudget122631.domain.model.MovementType;
import it.unicam.cs.mpgc.jbudget122631.domain.model.Category;
import it.unicam.cs.mpgc.jbudget122631.domain.repository.MovementRepository;
import it.unicam.cs.mpgc.jbudget122631.domain.repository.CategoryRepository;
import it.unicam.cs.mpgc.jbudget122631.domain.repository.PeriodRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MovementServiceImpl implements MovementService {

    // Categorie predefinite del sistema
    private static final String[] DEFAULT_CATEGORIES = {
            "Alimentari", "Trasporti", "Utenze", "Svago", "Stipendio", "Salute"
    };

    private static final String MOVEMENT_NOT_FOUND_MESSAGE = "Movimento non trovato";
    private static final String CATEGORY_NOT_FOUND_MESSAGE = "Categoria non trovata";

    private final MovementRepository movementRepository;
    private final CategoryRepository categoryRepository;
    private final PeriodRepository periodRepository;
    private BudgetService budgetService; // Opzionale per aggiornamento automatico budget
    private boolean categoriesInitialized = false;

    public MovementServiceImpl(MovementRepository movementRepository,
                               CategoryRepository categoryRepository,
                               PeriodRepository periodRepository,
                               BudgetService budgetService) {
        this.movementRepository = movementRepository;
        this.categoryRepository = categoryRepository;
        this.periodRepository = periodRepository;
        this.budgetService = budgetService;
    }

    public MovementServiceImpl(MovementRepository movementRepository,
                               CategoryRepository categoryRepository,
                               PeriodRepository periodRepository) {
        this(movementRepository, categoryRepository, periodRepository, null);
    }

    public void setBudgetService(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @Override
    public MovementDTO createMovement(MovementDTO movementDTO) {
        logMovementOperation("CREATE", movementDTO);

        initializeCategoriesIfNeeded();

        Movement movement = convertToEntity(movementDTO);
        logMovementCreated(movement);

        associateCategoriesToMovement(movement, movementDTO.getCategoryIds());

        Movement savedMovement = persistAndReloadMovement(movement);
        MovementDTO resultDTO = convertToDTO(savedMovement);

        logMovementResult(resultDTO);
        synchronizeBudgets("CREATE", resultDTO);

        return resultDTO;
    }

    @Override
    public Movement createMovement(Movement movement) {
        System.out.println("SERVICE - Creazione movimento diretto: " + movement.getDescription());

        Movement savedMovement = movementRepository.save(movement);

        synchronizeBudgetsForDirectMovement(savedMovement, "CREATE");

        return savedMovement;
    }

    @Override
    public Optional<MovementDTO> getMovementById(Long id) {
        return movementRepository.findById(id).map(this::convertToDTO);
    }

    @Override
    public List<MovementDTO> getAllMovements() {
        return movementRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public MovementDTO updateMovement(Long id, MovementDTO movementDTO) {
        Movement movement = findMovementById(id);

        MovementUpdateInfo updateInfo = captureUpdateInfo(movement);
        logMovementUpdate(id, updateInfo, movementDTO);

        updateMovementFields(movement, movementDTO);
        updateMovementCategories(movement, movementDTO.getCategoryIds());

        Movement savedMovement = movementRepository.save(movement);
        MovementDTO resultDTO = convertToDTO(savedMovement);

        logCompletedUpdate(updateInfo, resultDTO);
        synchronizeBudgets("UPDATE", resultDTO);

        return resultDTO;
    }

    @Override
    public void deleteMovement(Long id) {
        System.out.println("SERVICE - Eliminazione movimento ID: " + id);

        MovementDTO deletedMovement = captureMovementBeforeDeletion(id);
        movementRepository.deleteById(id);

        if (deletedMovement != null) {
            System.out.println("SERVICE - Movimento eliminato: " + deletedMovement.getDescription());
            synchronizeBudgets("DELETE", deletedMovement);
        } else {
            System.out.println("SERVICE - Movimento non trovato, eliminazione forzata");
        }
    }

    @Override
    public List<MovementDTO> getMovementsByDateRange(LocalDate startDate, LocalDate endDate) {
        return movementRepository.findByDateBetween(startDate, endDate)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<MovementDTO> getMovementsByType(MovementType type) {
        return movementRepository.findByType(type)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<MovementDTO> getMovementsByCategory(Long categoryId) {
        Category category = findCategoryById(categoryId);

        return movementRepository.findByCategoriesContaining(category)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<MovementDTO> getMovementsByPeriod(Long periodId) {
        return periodRepository.findById(periodId)
                .map(period -> movementRepository.findByPeriod(period)
                        .stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList()))
                .orElse(List.of());
    }

    @Override
    public BigDecimal getTotalByTypeAndDateRange(MovementType type, LocalDate startDate, LocalDate endDate) {
        return movementRepository.getTotalByTypeAndDateRange(type, startDate, endDate);
    }

    @Override
    public BigDecimal getTotalByCategoryAndDateRange(Long categoryId, LocalDate startDate, LocalDate endDate) {
        Category category = findCategoryById(categoryId);
        return movementRepository.getTotalByCategoryAndDateRange(category, startDate, endDate);
    }

    @Override
    public List<MovementDTO> searchMovements(String searchTerm) {
        return movementRepository.findByDescriptionContaining(searchTerm)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<MovementDTO> getMovementsPaginated(int page, int size) {
        return movementRepository.findAllPaginated(page, size)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public long getTotalMovementsCount() {
        return movementRepository.count();
    }

    private Movement findMovementById(Long id) {
        return movementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(MOVEMENT_NOT_FOUND_MESSAGE + " con ID: " + id));
    }

    private Category findCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException(CATEGORY_NOT_FOUND_MESSAGE + " con ID: " + categoryId));
    }

    private Movement persistAndReloadMovement(Movement movement) {
        Movement saved = movementRepository.save(movement);
        return movementRepository.findById(saved.getId()).orElse(saved);
    }

    private MovementDTO captureMovementBeforeDeletion(Long id) {
        return movementRepository.findById(id)
                .map(this::convertToDTO)
                .orElse(null);
    }

    private void initializeCategoriesIfNeeded() {
        if (categoriesInitialized) {
            return;
        }

        try {
            List<Category> existingCategories = categoryRepository.findAll();
            logExistingCategories(existingCategories);

            if (existingCategories.isEmpty()) {
                createDefaultCategories();
            }

            categoriesInitialized = true;
        } catch (Exception e) {
            System.err.println("Errore inizializzazione categorie: " + e.getMessage());
        }
    }

    private void createDefaultCategories() {
        System.out.println("CATEGORY - Inizializzazione categorie predefinite...");

        for (String categoryName : DEFAULT_CATEGORIES) {
            createDefaultCategory(categoryName);
        }
    }

    private void createDefaultCategory(String categoryName) {
        try {
            Category category = new Category(categoryName, "Categoria predefinita: " + categoryName);
            Category saved = categoryRepository.save(category);
            System.out.println("CATEGORY - Creata: " + saved.getName() + " (ID: " + saved.getId() + ")");
        } catch (Exception e) {
            System.err.println("CATEGORY - Errore creazione " + categoryName + ": " + e.getMessage());
        }
    }

    private void associateCategoriesToMovement(Movement movement, List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return;
        }

        System.out.println("SERVICE - Associazione categorie: " + categoryIds);

        for (Long categoryId : categoryIds) {
            associateSingleCategory(movement, categoryId);
        }

        System.out.println("SERVICE - Categorie associate: " + movement.getCategories().size());
    }

    private void associateSingleCategory(Movement movement, Long categoryId) {
        Optional<Category> categoryOpt = categoryRepository.findById(categoryId);

        if (categoryOpt.isPresent()) {
            movement.addCategory(categoryOpt.get());
            System.out.println("SERVICE - Categoria aggiunta: " + categoryOpt.get().getName());
        } else {
            System.out.println("SERVICE - Categoria non trovata: " + categoryId);
            logAvailableCategories();
        }
    }

    private void updateMovementCategories(Movement movement, List<Long> categoryIds) {
        movement.getCategories().clear();

        if (categoryIds != null) {
            System.out.println("SERVICE - Aggiornamento categorie: " + categoryIds);
            categoryIds.forEach(categoryId ->
                    categoryRepository.findById(categoryId)
                            .ifPresent(movement::addCategory)
            );
        }
    }

    private void updateMovementFields(Movement movement, MovementDTO dto) {
        movement.updateDetails(
                dto.getDescription(),
                dto.getAmount(),
                dto.getType(),
                dto.getDate(),
                dto.getNotes()
        );
    }

    private MovementUpdateInfo captureUpdateInfo(Movement movement) {
        return new MovementUpdateInfo(
                movement.getDescription(),
                movement.getAmount()
        );
    }

    private void synchronizeBudgets(String operation, MovementDTO movementDTO) {
        if (!budgetServiceAvailable()) {
            logBudgetServiceUnavailable();
            return;
        }

        try {
            logBudgetSynchronizationStart(operation, movementDTO);
            budgetService.updateAllBudgetsWithRealMovements();
            logBudgetSynchronizationSuccess();

        } catch (Exception e) {
            logBudgetSynchronizationError(e);
        }
    }

    private void synchronizeBudgetsForDirectMovement(Movement movement, String operation) {
        if (budgetServiceAvailable()) {
            try {
                MovementDTO dto = convertToDTO(movement);
                synchronizeBudgets(operation, dto);
            } catch (Exception e) {
                System.err.println("Errore sincronizzazione budget: " + e.getMessage());
            }
        }
    }

    private boolean budgetServiceAvailable() {
        return budgetService != null;
    }

    private Movement convertToEntity(MovementDTO dto) {
        Movement movement = new Movement(
                dto.getDescription(),
                dto.getAmount(),
                dto.getType(),
                dto.getDate()
        );

        if (hasNotes(dto)) {
            movement.setNotes(dto.getNotes());
        }

        movement.setScheduled(dto.isScheduled());
        return movement;
    }

    private MovementDTO convertToDTO(Movement movement) {
        MovementDTO dto = new MovementDTO();

        dto.setId(movement.getId());
        dto.setDescription(movement.getDescription());
        dto.setAmount(movement.getAmount());
        dto.setType(movement.getType());
        dto.setDate(movement.getDate());
        dto.setNotes(movement.getNotes());
        dto.setScheduled(movement.isScheduled());

        // Converti categorie in IDs
        List<Long> categoryIds = movement.getCategories()
                .stream()
                .map(Category::getId)
                .collect(Collectors.toList());
        dto.setCategoryIds(categoryIds);

        return dto;
    }

    private boolean hasNotes(MovementDTO dto) {
        return dto.getNotes() != null && !dto.getNotes().trim().isEmpty();
    }

    private String getCategoriesInfo(MovementDTO movement) {
        if (movement.getCategoryIds() == null || movement.getCategoryIds().isEmpty()) {
            return "Nessuna categoria";
        }

        return movement.getCategoryIds().stream()
                .map(this::getCategoryNameById)
                .collect(Collectors.joining(", "));
    }

    private String getCategoryNameById(Long categoryId) {
        if (categoryId == null) {
            return "Sconosciuta";
        }

        try {
            return categoryRepository.findById(categoryId)
                    .map(Category::getName)
                    .orElse("Categoria ID:" + categoryId);
        } catch (Exception e) {
            return "Categoria ID:" + categoryId;
        }
    }

    private void logMovementOperation(String operation, MovementDTO dto) {
        System.out.println("SERVICE - Operazione " + operation + ": " + dto.getDescription());
    }

    private void logMovementCreated(Movement movement) {
        System.out.println("SERVICE - Movement creato: " + movement.getDescription());
    }

    private void logMovementResult(MovementDTO dto) {
        System.out.println("SERVICE - DTO risultante con " +
                (dto.getCategoryIds() != null ? dto.getCategoryIds().size() : 0) + " categorie");
    }

    private void logExistingCategories(List<Category> categories) {
        System.out.println("CATEGORY - Categorie esistenti (" + categories.size() + "):");
        categories.forEach(cat ->
                System.out.println("  - ID: " + cat.getId() + ", Nome: " + cat.getName())
        );
    }

    private void logAvailableCategories() {
        try {
            List<Category> available = categoryRepository.findAll();
            System.out.println("SERVICE - Categorie disponibili:");
            available.forEach(cat ->
                    System.out.println("  - ID: " + cat.getId() + ", Nome: " + cat.getName())
            );
        } catch (Exception e) {
            System.err.println("Errore recupero categorie disponibili: " + e.getMessage());
        }
    }

    private void logMovementUpdate(Long id, MovementUpdateInfo oldInfo, MovementDTO newDto) {
        System.out.println("SERVICE - Aggiornamento movimento ID: " + id);
        System.out.println("  Da: " + oldInfo.description + " €" + oldInfo.amount);
        System.out.println("  A: " + newDto.getDescription() + " €" + newDto.getAmount());
    }

    private void logCompletedUpdate(MovementUpdateInfo oldInfo, MovementDTO resultDto) {
        System.out.println("SERVICE - Aggiornamento completato:");
        System.out.println("  " + oldInfo.description + " → " + resultDto.getDescription());
        System.out.println("  €" + oldInfo.amount + " → €" + resultDto.getAmount());
    }

    private void logBudgetServiceUnavailable() {
        System.out.println("BUDGET - Servizio non disponibile, sincronizzazione automatica saltata");
    }

    private void logBudgetSynchronizationStart(String operation, MovementDTO movement) {
        System.out.println("BUDGET - Sincronizzazione automatica dopo " + operation);
        System.out.println("  Movimento: " + movement.getDescription() + " (€" + movement.getAmount() + ")");
    }

    private void logBudgetSynchronizationSuccess() {
        System.out.println("BUDGET - Sincronizzazione completata con successo");
    }

    private void logBudgetSynchronizationError(Exception e) {
        System.err.println("BUDGET - Errore sincronizzazione: " + e.getMessage());
        e.printStackTrace();
    }

    private static class MovementUpdateInfo {
        final String description;
        final BigDecimal amount;

        MovementUpdateInfo(String description, BigDecimal amount) {
            this.description = description;
            this.amount = amount;
        }
    }
}