# **Metodologie di Programmazione**
## Programmazione Concorrente
### **Definizioni**
**Thread**: sono le unità di esecuzione di un processo. I thread condividono lo stesso spazio di indirizzi e risorse di un processo ma eseguono in modo indipendente all'interno di esso.
Consentono l'esecuzione simultanea di più attività all'interno di un singolo processo. 
I thread all'interno dello stesso processo condividono lo stesso contesto, compreso lo spazio di indirizzi e i descrittori di file.

un *processo* contiene uno o più tread.

**Binari**: sono programmi inattivi che risiedono su un supporto di memorizzazione, compilati in un formato accessibile da un dato sistema operativo e da una determinata architettura, pronti per essere eseguiti.

**Processi**: sono l'astrazione del sistema operativo che rappresenta binari in azione (es. il binario caricato, la memoria virtualizzata, le risorse del kernel come i file aperti, un utente associato e così via). 
Un processo può contenere uno (**single thread**) o più thread (**multithread**). 
Ogni processo ha il proprio spazio di indirizzi, memoria e risorse assegnate, rendendolo isolato da altri processi.

### **Introduzione**
La programmazione concorrente in Java si riferisce alla scrittura di programmi che possono eseguire **più attività in parallelo**. Questo approccio è particolarmente utile per sfruttare sistemi multi-core e migliorare le prestazioni del software, consentendo a più thread di eseguire operazioni simultaneamente.

Il primo passo nello sviluppo concorrente consiste nel suddividere le attività in *task*.
### **Runnable**
L'interfaccia funzionale `Runnable` viene utilizzata per descrivere un task da eseguire (anche in concomitanza con altri task):
```java
public interface Runnable {
  void run();
}
```
Il metodo `run()` contiene il codice da eseguire e può essere lanciato in due modi:
- creando e gestendo manualmente un thread;
- tramite un **executor**, che si occupa dell'esecuzione dei task in modo astratto senza gestire direttamente i thread

### ***java.util.concurrent***
#### **Executor**
**Executor** è un'interfaccia base nel package `java.util.concurrent` che gestisce l'esecuzione dei task senza richiedere la creazione o la gestione manuale dei thread.

Definisce il metodo:
```java
void execute(Runnable command);
```
Permette di eseguire un'operazione rappresentata dall'oggetto `Runnable` **senza creare o avviare manualmente i thread**, delegando la gestione (dei thread) all'implementazione dell'interfaccia `Runnable`.

#### **ExecutorService**
**ExecutorService** estende l'interfaccia `Executor`,e fornisce funzionalità più avanzate per la gestione dei task, consentendo di:
- eseguire **operazioni asincrone**;
- gestire **un pool di thread** in modo efficente; (*)
- controllare il ciclo di vita dei thread (avvio, attesa e terminazione).

```text
(*) Un **pool di thread** è un insieme di thread già pronti all’esecuzione,
 gestiti da un sistema centralizzato (come un `ExecutorService`,).
Invece di creare un nuovo thread ogni volta che c’è un compito da svolgere,
i thread del pool vengono riutilizzati per eseguire più task,
migliorando così l’efficienza e le prestazioni del programma.
```

La classe di utilità `Executors` fornisce **metodi factory** per creare facilmente diverse tipologie di `ExecutorService`:
```java
static ExecutorService newCachedThreadPool()
static ExecutorService newFixedThreadPool(int nThreads)
static ExecutorService newSingleThreadExecutor()
static ExecutorService newWorkStealingPool()
```

#### newCachedThreadPool
`newCachedThreadPool` è un **metodo factory** della classe `Executor` che serve a creare un `ExecutorService`.
Con `newCachedThreadPool` vengono creati più thread se non ce ne sono disponibili.

Esempio:
```java
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    public static Runnable getTask(String name, int counter) {
        return () -> {
            for (int i = 0; i < counter; i++) {
                System.out.println(name + "> " + i);
            }
        };
    }

    public static void main(String[] args) {
        ExecutorService executor = Executors.newCachedThreadPool();
        executor.execute(getTask("T1", 10));
        executor.execute(getTask("T2", 10));
        executor.execute(getTask("T3", 10));
        executor.shutdown(); // Chiude l'executor dopo aver completato i task
    }
}

```
In questo esempio, le esecuzioni dei tre task (T1, T2, T3) si intervallano dinamicamente grazie all’uso di un cached thread pool, che crea e riutilizza thread in base al carico di lavoro.

Output:
```java
T3> 0
T1> 0
T1> 1
T1> 2
T1> 3
T1> 4
T1> 5
T1> 6
T1> 7
T1> 8
T1> 9
T2> 0
T2> 1
T3> 1
T2> 2
T2> 3
T2> 4
T2> 5
T2> 6
T2> 7
T2> 8
T3> 2
T3> 3
T3> 4
T3> 5
T3> 6
T3> 7
T3> 8
T2> 9
T3> 9
```
(l'ordine di esecuzione non è noto/garantito, i task partono praticamente tutti insieme su thread diversi).
