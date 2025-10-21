package it.unicam.cs.mpgc.jbudget122631.domain.model;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Category> children = new HashSet<>();

    @Column(nullable = false)
    private boolean active = true;

    protected Category() {}

    public Category(String name) {
        this.name = Objects.requireNonNull(name, "Nome categoria richiesto");
    }

    public Category(String name, String description) {
        this(name);
        this.description = description;
    }

    public void addChild(Category child) {
        Objects.requireNonNull(child, "Categoria figlio non puo' essere null");
        if (child.equals(this)) {
            throw new IllegalArgumentException("Una categoria non puo' essere figlia di se stessa");
        }
        if (isDescendantOf(child)) {
            throw new IllegalArgumentException("Ciclo nella gerarchia non ammesso");
        }

        children.add(child);
        child.parent = this;
    }

    public void removeChild(Category child) {
        if (children.remove(child)) {
            child.parent = null;
        }
    }

    public boolean isRoot() {
        return parent == null;
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    public boolean isDescendantOf(Category ancestor) {
        Category current = this.parent;
        while (current != null) {
            if (current.equals(ancestor)) {
                return true;
            }
            current = current.parent;
        }
        return false;
    }

    public List<Category> getPath() {
        List<Category> path = new ArrayList<>();
        Category current = this;
        while (current != null) {
            path.add(0, current);
            current = current.parent;
        }
        return path;
    }

    public Set<Category> getAllDescendants() {
        Set<Category> descendants = new HashSet<>();
        for (Category child : children) {
            descendants.add(child);
            descendants.addAll(child.getAllDescendants());
        }
        return descendants;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Category getParent() { return parent; }
    public Set<Category> getChildren() { return new HashSet<>(children); }
    public boolean isActive() { return active; }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name);
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category)) return false;
        Category category = (Category) o;

        if (this.id != null && category.id != null) {
            return Objects.equals(this.id, category.id);
        }

        return Objects.equals(name, category.name) &&
                Objects.equals(parent, category.parent);
    }

    @Override
    public int hashCode() {
        // Se ha un ID, usa quello per l'hash
        if (id != null) {
            return Objects.hash(id);
        }

        return Objects.hash(name, parent);
    }

    @Override
    public String toString() {
        return getPath().stream()
                .map(Category::getName)
                .reduce((a, b) -> a + " > " + b)
                .orElse(name);
    }
}