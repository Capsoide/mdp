# **Metodologie di Programmazione**
## Principi Solid
### **Single Responsibility Principle (SRP)**
Ogni classe dovrebbe avere una ed una sola responsabilità, interamente incapsulata al suo interno.

Una responsabilità è una singola famiglia di funzionalità che servono ad un particolare attore.
Nella prima fase della realizzazione del progetto è fondamentale individuare le responsabilità di ogni elemento o componente dell'applicazione.

Quindi quando si sviluppa una soluzione software è necessario:

• individuare gli attori;

• identificare le responsabilità di questi attori;

• raggruppare le funzionalità nelle differenti interfacce/classi in modo che ognuna abbia una singola responsabilità.

#### Esempio di Violazione
Una classe `Order` che gestisce i dati di un ordine (id, descrizione, prezzo) e contiene anche un metodo `print()`. 
La responsabilità principale di `Order` è **rappredentare un ordine**, non **gestire la stampa**.

```java
public class Order {
  ...
  public int getId(){ ... }
  public String getDescription() { ... }
  public double getPrice() { ... }
  public void print () {
    ...
  }
  ...
}
```

#### Refactoring Corretto
Bisogna separare le responsabilità di stampa in una propria astrazione, ad esempio un'interfaccia `OrderPrinter`.
```java
public class Order {
  ...
  public int getId(){ ... }
  public String getDescription() { ... }
  public double getPrice() { ... }
  ...
}

public interface OrderPrinter {
  public void print(Order o);
}
```
L'utilizzo del **Single Responsibility Principle** rende il software più facile da **manutenere** e da **estendere** poichè le modifiche a una funzionalità sono **isolate**.

### **Open/Closed Principle (OCP)**
Un'entità software dovrebbe essere aperta alle estensioni, ma chiusa alle modifiche.

Un'entità software dovrebbe essere aperta alle estensioni, ma chiusa alle modifiche. Ciò significa che è possibile cambiare il comportamento di una componente software senza dover modificare il codice sorgente che rimane nascosto. 
Un modulo è detto:
- **aperto**: se questo permette le estensioni;
- **chiuso** se è disponibile per l'uso da parte di altre componenti senza la neccessità di conoscerne l'esatta implementazione.

#### Esempio di Violazione
```java
public class Rectangle {
  private final double width;
  private final double height;

  public Rectangle(double width, double height){
    this.width = width;
    this.height = height;
  }

  public double getWidth(){
    return width;
  }

  public double getHeight(){
    return height;
  }
}
```
```java
public class AreaCalculator {
  public double computeArea(Rectangle[] shapes) {
    double area = 0;
    for(int i=0; i<shapes.length; i++) {
      area += shapes[i].getWidth() * shapes[i].getHeight();
    }
  }
}
```
La violazione sta proprio in `AreaCalculator` perchè **dipende direttamente** dalla classe concreta `Rectangle` e dalla sua implementazione perchè usa `getWidth()` e `getHeight()` per calcolare l'area.

Questo è un problema perchè se volessi aggiungere **un nuovo tipo di forma**, ad esempio `Circle` o `Triangle`, dovrei modificare il codice della classe `AreaCalculator` per gestore ogni nuovo caso

#### Refactoring Corretto
Con il refactoring, si introduce un'astrazione `Shape` che incapsula la logica per calcolare l'area:
```java
public interface Shape {
  public double getArea();
}
```
Con l'aggiunta di `Shape`, `AreaCalculator` non deve più sapere come calcolare l'area:

```java
public class AreaCalculator {
  public double computeArea(Shape[] shapes) {
    double area = 0;
    for (int i = 0; i < shapes.length; i++) {
      area += shapes[i].getArea();
    }
    return area;
  }
}
```
Ogni forma concreta (es. `Rectangle`, `Circle`) implementa la propria logica:

#### Interfaccia `Shape`
```java
public interface Shape {
    double getArea();
}
```
#### Classe `Rectangle`
```java
public class Rectangle implements Shape {
    private final double width;
    private final double height;

    public Rectangle(double width, double height) {
        this.width = width;
        this.height = height;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    @Override
    public double getArea() {
        return width * height;
    }
}

```
#### Classe `Circle`
```java
public class Circle implements Shape {
    private final double radius;

    public Circle(double radius) {
        this.radius = radius;
    }

    public double getRadius() {
        return radius;
    }

    @Override
    public double getArea() {
        return Math.PI * Math.pow(radius, 2);
    }
}

```
L'utilizzo dell' **Open/Closed Principle** rende il software **aperto all'estensione** (è possibile aggiungere nuove forme implementando `Shape`) e **chiuso alla modifica** (non si deve più cambiare `AreaCalculator`), questo riduce il rischio di introdurre bug in codice già funzionante migliorando **manutentibilità** e **leggibilità** del codice.

### **Principio di Sostituzione di Liskov (LSP)**

In un programma è sempre possibile sostituire le istanze di una classe con le istanze di una sua sottoclasse senza alterare il comportamento del programma. Tale principio è una particolare definizione di *sottotipo*:
```text
Sia q(x) una proprietà verificata da tutti gli oggetti x di tipo T.
Allora q(y) sarà verificata da tutti gli oggetti y di tipo S dove
S è un sottotipo di T.
```
In base a questo principio, una **classe dovrebbe poter essere sostituita da qualsiasi classe derivata**, mantenendo il codice perfettamente funzionante.

#### Esempio di Violazione
Superclasse `Rectangle`
```java
public class Rectangle {
    protected double width;
    protected double height;

    public double getWidth() { return width; }
    public double getHeight() { return height; }

    public void setWidth(double width) { this.width = width; }
    public void setHeight(double height) { this.height = height; }

    public double getArea() {
        return width * height;
    }
}
```
Sottoclasse `Square`
```java
public class Square extends Rectangle {
    @Override
    public void setWidth(double width) {
        this.width = width;
        this.height = width; // forza il lato uguale
    }

    @Override
    public void setHeight(double height) {
        this.height = height;
        this.width = height; // forza il lato uguale
    }
}
```
Codice Cient
```java
public class ShapeTest {
    public static void checkArea(Rectangle r) {
        r.setWidth(5);
        r.setHeight(10);
        if (r.getArea() != 50) {
            throw new IllegalStateException("Bad area!");
        }
    }

    public static void main(String[] args) {
        checkArea(new Rectangle()); // OK
        checkArea(new Square());    // Errore: area = 100, non 50
    }
}

```
Perchè è una violzione:
Il metodo `checkArea()` si aspetta che larghezza e altezza possano essere impostate in modo indipendente (comportamento valido per `Rectangle`).
Ma la sottoclasse `Square` rompe questo contratto: imposta sempre larghezza e altezza allo stesso valore.

Il risultato è che, sostituendo `Rectangle` con `Square`, il comportamento del programma cambia, e il test fallisce.
Quindi la sostituzione non è valida, e si viola il principio di sostituzione di Liskov.

#### Refactoring Corretto

1. Separare le classi, evitando l'ereditarietà
```java
public interface Shape {
    double getArea();
}
```
2. Implementazioni coerenti
```java
public class Rectangle implements Shape {
    private final double width;
    private final double height;

    public Rectangle(double width, double height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public double getArea() {
        return width * height;
    }
}
//---
public class Square implements Shape {
    private final double side;

    public Square(double side) {
        this.side = side;
    }

    @Override
    public double getArea() {
        return side * side;
    }
}
```
3. Codice client corretto
```java
public class ShapeTest {
    public static void printArea(Shape s) {
        System.out.println("Area: " + s.getArea());
    }

    public static void main(String[] args) {
        Shape r = new Rectangle(5, 10);
        Shape s = new Square(5);

        printArea(r); // Area: 50.0
        printArea(s); // Area: 25.0
    }
}
```
Ora è possibile sostituire un oggetto con un altro che implementa la stessa interfaccia senza alterare il comportamento del programma, rispettando perfettamente il Liskov Substitution Principle.

### Interface Segregation Principle (ISP)
Sarebbero preferibili più interfacce specifiche, che una singola generica.

E' consigliabile **suddividere le interfacce per scopi specifici**. In tal modo, le classi possono implementare solo le interfacce di cui hanno necessariamente bisogno, evitando la dichiarazione di metodi vuoti e inutili. In questo modo, il codice viene ridotto notevolmente, riducendo di conseguenza, i tempi di sviluppo e la probabilità di presenza di bug.
<p align="center">
  <img src="https://github.com/user-attachments/assets/5c9876fc-376e-479d-b21d-223846e4a23e" width="600" alt="Diagramma LSP">
</p>

### Dependency Inversion Principle (DIP)
I moduli di alto livello non devono dipendere da quelli di basso livello, entrambi devono dipendere da astrazioni.
Le astrazioni non devono dipendere dai dattagli, sono i dettagli che dipendono dalle astrazioni.

I moduli di alto livello sono le **classi che realizzano le proprie funzioni facendo uso dei moduli di basso livello**, attraverso le interfacce esposte da questi ultimi.

Il principio di inversione delle dipendenze cerca di **disaccoppiare quanto più possibile i due moduli, connettendoli entrambi tramite astrazioni**. In questo modo, i moduli di alto livello usano determinate astrazioni, mentre i moduli di basso livello le implementano.

## Code Smells
Un **code smells** rappresenta una caratteristica del codice di un programma che indica la possibile presenza di un problema serio. Sono **cattive pratiche di programmazione** che rendono il codice inusabile o particolarmente pericoloso.

Determinare un code smell è soggettivo, e varia da linguaggio, sviluppatore e metodologia di sviluppo, i più frequenti sono:

### **Metodi troppo lunghi** 
La presenza di un metodo che è più lungo di **10 righe** è indice di una cattiva organizzazione del codice con la probabilità di contenere codice ripetuto e renderà il codice difficile da testare e manutenere.

#### **Soluzione**
Utilizzare dei **metodi semplici**, con **poche righe di codice**, che abbiano un nome semplice e significativo; inoltre è bene fattorizzare il codice creando metodi di utilità.

### **Classi troppo grandi**
Una classe che ha molti campi, metodi e linee è quasi sicuramente scritta nel modo sbagliato in quanto potrebbe avere **troppe responsabilità** e ciò viola il primo principio SOLID (Single Responsability Principle).

#### **Soluzione**
In questo caso è bene dividere la classe spostando alcuni comportamenti fuori dalla classe, estraendo sottoclassi o interfacce e utilizzando, quando possibile, la composizione tra oggetti.

### **Classi diverse, stessa interfaccia**
Si verifica quando due o più classi forniscono la stessa funzionalità, ma i nomi dei metodi o le firme sono diversi. E' bene rinominare i metodi per renderli identici in tutte le classi, oppure rendere le firme dei metodi e le implementazioni identiche.

### **Gerarchie Parallele**
Si verifica quando:

- Ogni volta che si crea una **sottoclasse** di una certa classe, ci si ritrova a dover creare anche una sottoclasse
  corrispondente in un’altra gerarchia.
  

- Con l’aumento del numero di classi, la manutenzione diventa complessa e le modifiche richiedono interventi su più gerarchie
  contemporaneamente.

Spesso questo problema è sintomo di una violazione del principio di sostituzione di Liskov (LSP) o di altri principi SOLID, perché le gerarchie diventano troppo rigide e dipendenti tra loro.

#### **Soluzione**
E' possibile **deduplicare le gerarchie parallele** seguendo due passaggi principali:
1. **Riferire una gerarchia all'altra**, quindi bisogna fare in modo che le istanze di una gerarchia possano utilizzare o delegare il comportamento alle istanze dell'altra gerarchia.
2. **Rimuovere la gerarchia ridondante** (duplicata) nella classe di riferimento una volta consolidata la logica, mantenendo un'unica struttura coerente.

### **Commenti eccessivi**
Il code smell dei commenti eccessivi si verifica quando un metodo o una classe contiene numerosi commenti esplicativi che cercano di chiarire logiche poco intuitive. Questo accade spesso quando l’autore percepisce che il codice non è immediatamente chiaro. In questi casi, i commenti funzionano come un “deodorante”: mascherano la complessità invece di risolverla.

Un principio fondamentale è che il miglior commento è un buon nome di metodo, classe o variabile. Se un frammento di codice non può essere compreso senza commenti, è preferibile rifattorizzarlo, migliorandone la struttura, rinominando elementi in modo più descrittivo e suddividendo la logica in unità più semplici. In questo modo i commenti diventano superflui e il codice risulta autoesplicativo, leggibile e più facile da manutenere.













