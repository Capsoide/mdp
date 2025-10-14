# **Metodologie di Programmazione**
## Programmazione Concorrente
### **Definizioni**
**Thread**: sono le unità di esecuzione di un processo. I thread condividono lo stesso spazio di indirizzi e risorse di un processo ma eseguono in modo indipendente all'interno di esso.
Consentono l'esecuzione simultanea di più attività all'interno di un singolo processo. 
I thread all'interno dello stesso processo condividono lo stesso contesto, compreso lo spazio di indirizzi e i descrittori di file.

**Binari**: sono programmi inattivi che risiedono su un supporto di memorizzazione, compilati in un formato accessibile da un dato sistema operativo e da una determinata architettura, pronti per essere eseguiti.

**Processi**: sono l'astrazione del sistema operativo che rappresenta binari in azione (es. il binario caricato, la memoria virtualizzata, le risorse del kernel come i file aperti, un utente associato e così via). 
Un processo può contenere uno (**single thread**) o più thread (**multithread**). 
Ogni processo ha il proprio spazio di indirizzi, memoria e risorse assegnate, rendendolo isolato da altri processi.

### **Introduzione**
La programmazione concorrente in Java si riferisce alla scrittura di programmi che possono eseguire **più attività in parallelo**. Questo approccio è particolarmente utile per sfruttare sistemi multi-core e migliorare le prestazioni del software, consentendo a più thread di eseguire operazioni simultaneamente.

### **Computazione Asincrona**

Il primo passo nello sviluppo concorrente consiste nel suddividere le attività in *task*.
#### ⚠️**Runnable**
L'interfaccia funzionale `Runnable` viene utilizzata per descrivere un task da eseguire (anche in concomitanza con altri task):
```java
public interface Runnable {
  void run();
}
```
Il metodo `run()` contiene il codice da eseguire e può essere lanciato in due modi:
- creando e gestendo manualmente un thread;
- tramite un **executor**, che si occupa dell'esecuzione dei task in modo astratto senza gestire direttamente i thread

#### ⚠️**Executor**
**Executor** è un'interfaccia base nel package `java.util.concurrent` che gestisce l'esecuzione dei task senza richiedere la creazione o la gestione manuale dei thread.

Definisce il metodo:
```java
void execute(Runnable command);
```
Permette di eseguire un'operazione rappresentata dall'oggetto `Runnable` **senza creare o avviare manualmente i thread**, delegando la gestione (dei thread) all'implementazione dell'interfaccia `Runnable`.

#### ⚠️**ExecutorService**
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
```

#### ⚠️newCachedThreadPool
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
        executor.execute(getTask("T1", 5));
        executor.execute(getTask("T2", 5));
        executor.execute(getTask("T3", 5));
        executor.shutdown(); // Chiude l'executor dopo aver completato i task
    }
}

```
In questo esempio, le esecuzioni dei tre task (T1, T2, T3) si intervallano dinamicamente grazie all’uso di un cached thread pool, che crea e riutilizza thread in base al carico di lavoro.

Output:
```java
T1> 0
T2> 0
T3> 0
T1> 1
T3> 1
T2> 1
T1> 2
T2> 2
T3> 2
T3> 3
T1> 3
T2> 3
T1> 4
T2> 4
T3> 4

```
L'ordine di esecuzione non è noto/garantito, i task partono praticamente tutti insieme su thread diversi.

#### ⚠️**newFixedThreadPool**

`newFixedThreadPool` è un **metodo factory** della classe `Executors` che crea un `ExecutorService` con un **numero fisso di thread**.

Con `newFixedThreadPool(int nThreads)` vengono creati al massimo `nThreads` thread 
- se ci sono più task di quelli che i thread possono gestire contemporaneamente, i task **vengono messi in cods** e **eseguiti appena un thread si libera**;
- i thread dei pool **rimangono attivi e vengono riutilizzati** per eseguire nuovi task, evitando il costo di creazione continua di nuovi thread.

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
        ExecutorService executor = Executors.newFixedThreadPool(2); <--
        executor.execute(getTask("T1", 5));
        executor.execute(getTask("T2", 5));
        executor.execute(getTask("T3", 5));
        executor.shutdown(); // Chiude l'executor dopo aver completato i task
    }
}
```
In questo caso vengono eseguiti **solo due thread alla volta** (T1 e T2), e solo quando uno dei due termina viene avviato T3.
I task vengono eseguiti sequenzialmente `T1 → T2 → T3`; si intervallano solo due esecuzioni T1 e T2 e quando uno dei due termina parte T3.

Output:
```java
T2> 0
T2> 1
T2> 2
T2> 3
T2> 4
T1> 0
T1> 1
T1> 2
T1> 3
T3> 0
T3> 1
T3> 2
T3> 3
T3> 4
T1> 4
```
Comportamento:
- Il pool ha 2 thread disponibili, quindi T1 e T2 partono insieme.
- Appena T2 finisce, il thread che lo eseguiva viene **riutilizzato** per fare partire T3.
- T1 **non è ancora terminato**, qundi continua in parallelo con T3.
- Alla fine terminano entrambi (T3 poi T1).

#### ⚠️**newSingleThreadExecutor()**

`newSingleThreadExecutor` è un **metodo factory** della classe `Executors` che crea un `ExecutorService` con **un solo thread**.

I task vengono eseguiti **uno alla volta**, in ordine di invio (FIFO), e non c'è esecuzione parallela: **il successivo inizia solo dopo che il precedente è terminato**.
```java
ExecutorService executor = Executors.newSingleThreadExecutor();
```
Viene eseguito 1 thread alla volta: T1 -> T2 -> T3

Output:
```java
T1> 0
T1> 1
T1> 2
T1> 3
T1> 4
T2> 0
T2> 1
T2> 2
T2> 3
T2> 4
T3> 0
T3> 1
T3> 2
T3> 3
T3> 4
```


### ⚠️**Callable**
L’interfaccia funzionale `Callable<V>` viene utilizzata per rappresentare una computazione che restituisce un risultato e può sollevare un’eccezione.
A differenza di `Runnable`, che rappresenta un’attività senza valore di ritorno e senza gestione diretta delle eccezioni, `Callable` permette di eseguire un’operazione che produce un risultato e può generare eccezioni controllate.

L'interfaccia `Callable` implementa un solo metodo `call()` che rappresenta il punto d'ingresso della computazione, restituisce un risultato di tipo generico `V`.
```java
public interface Callable<V> {
	V call() throws Exception;
}
```
Tramite l' `ExecutorService` possiamo inviare un ogetto Callable. Il risultato dell'operazione è di tipo **Future**, ovvero un oggetto che rappresenterà il risultato dell'esecuzione in un qualsiasi tempo futuro. 
E' lo strumento che viene utilizzato per fare interagire chi chiama il servizio con il thread che si occupa di eseguire il servizio.
Esempio:
```java
//creazione di un ExecutorService (in questo caso, un pool di thread fisso)
ExecutorService executorService = Executor.newFixedthreadPool(1)

//creazione dell'istanza callable
Callable<String> myCallable = () -> { //simulo un'attività che restituisce una stringa dopo un x di secondi
	Thread.sleep(2000);
	return "Task completato!";
};

//sottomissione di Callable al pool di thread e ottenere un future per il risultato
Future<String> futureResult = executor.Service.submit(myCallable); //il metodo submit() invia un Callable per essere eseguito

//attendo il risultato e lo stampo
String result = futureresult.get();
System.out.println(result)
```
Quindi ogni volta che si invia un **Callable** con **submit()**, il metodo restituisce un **Future<V>**, dove V è il tipo di ritorno del `call()`.
**Future** permette di:
- recuperare il risultato (`get()`)
- controllare se è finito (`isDone()`)
- annullarlo (`cancel()`)
- gestire eventuali eccezioni (`ExecutionException`)

### ⚠️**Future**
L’interfaccia funzionale `Future` rappresenta il **risultato** di un'operazione asincrona. 
L'oggetto Future è "un contenitore" per un valore che non è ancora disponibile.
Fornisce metodi per controllare lo stato dell'operazione e ottenere il risultato quando è disponibile.

| Metodo | Eccezioni lanciate | Descrizione |
|---------|--------------------|--------------|
| `V get()` | `InterruptedException`, `ExecutionException` | Restituisce il valore quando disponibile, altrimenti viene lanciata un’eccezione. Blocca il thread chiamante fino a quando il risultato non è pronto. |
| `V get(long timeout, TimeUnit unit)` | `InterruptedException`, `ExecutionException`, `TimeoutException` | Restituisce il risultato dell'operazione quando è disponibile, aspettando al massimo il tempo specificato. Il thread chiamante viene bloccato fino a quando il risultato non è pronto o scade il timeout. |
| `boolean cancel(boolean mayInterruptIfRunning)` | — | Cerca di annullare l'operazione associata all'oggetto Future. Se l'operazione non è ancora iniziata, può essere annullata. Se è già in corso, il comportamento dipende dal valore di `mayInterruptIfRunning`. Se `mayInterruptIfRunning` è true, il thread in esecuzione può essere interrotto. Restituisce true se l'operazione è stata annullata con successo. |
| `boolean isCancelled()` | — | Verifica se l’operazione sia stata cancellata o meno. |
| `boolean isDone()` | — | Verifica se l’operazione sia stata terminata o meno, indipendentemente che sia stata completata normalmente o annullata. |

### **Esecuzione multipla**
Se è necessario aspettare i risultati di più task, il metodo `invokeAll`, che prende una collezione di Callable, può essere utilizzato:
```java
List<Callable<V>> tasks = ...
List<Future<V>> results = executor.invokeAll(tasks);
L'esecuzione del thread corrente è bloccata fino a quando tutti i task non sono terminati (con successo o meno).
```
Un'altra opzione che è possibile usare quando bisogna lavorare su più task è `invokeAny`. In questo caso viene restituito il risultato del primo task (terminato con successo); mentre gli altri compiti vengono cancellati.

### ⚠️**CompletableFuture**

FINIRE COMPLETABLEFUTURE

_________________________________________________________
_________________________________________________________
_________________________________________________________

| Runnable | Callable | Future | CompletableFuture |
|:---------|:----------|:--------|:------------------|
| Rappresenta un **task senza valore di ritorno** | Rappresenta un **task che restituisce un risultato** | Rappresenta il **risultato futuro** di un task asincrono | È una **versione avanzata di Future** che permette operazioni asincrone e composte |
| Definisce solo il metodo `run()` | Definisce il metodo `call()` che restituisce un valore | Restituito da `ExecutorService.submit()` | Permette di concatenare operazioni con metodi come `thenApply()`, `thenRun()`, ecc. |
| Non restituisce valori e **non gestisce eccezioni** | Restituisce un valore e **può sollevare eccezioni** | Permette di **controllare, attendere e ottenere** il risultato | Gestisce i risultati e le eccezioni **in modo fluido e non bloccante** |
| Eseguito tramite `executor.execute()` | Eseguito tramite `executor.submit()` | Usa metodi come `get()` e `isDone()` | Usa metodi come `supplyAsync()`, `thenAccept()`, `exceptionally()` |


### ⚠️**Visibilità**
La **visibilità** nei thread indica la capacità di un thread di vedere le modifiche apportate da un altro thread alle variabili condivise.
In Java, a causa delle ottimizzazioni della CPU e della cache, un thread potrebbe lavorare su una copia locale di una variabile, senza vedere gli aggiornamenti effettuati da altri thread. Quindi i thread lavorano su memorie separate.

Questo può portare a situazioni in cui i diversi thread operino su copie locali separate dai dati, e ciò può causare problemi di coerenza della memoria.

Per garantire visibilità si usano le variabili `volatile`, `final`, `static`.

Utilizzando `volatile` nella variabile statica `done` facciamo in modo che l'operazione di scrittura venga immediatamente riflessa in tutto il sistema.
```java
private static volatile boolean done = false;

public static void main(String[] argv) {
	Runnable hellos = () -> {
		for( int i=0 ; i<1000 ; i++ ) {
			System.out.println("Hello "+i);
		}
		done = true;
	};
	Runnable goodbyes = () -> {
		int i=0;
		while (!done) { i++; }
		System.out.println("Goodbye "+i);
	};
	ExecutorService executor = Executors.newCachedThreadPool();
	executor.execute(hellos);
	executor.execute(goodbyes);
}
```
### ⚠️**Sincronizzazione**
Quando due o più thread tentano di accedere e modificare contemporaneamente la stessa risorsa condivisa, il risultato può essere imprevedibile e non deterministico. Questo può portare ad una **race condition** (condizione di competizione) e a **problemi di sincronizzazione**.

Per evitare le race condition in Java, è importante utilizzare la sincronizzazione dei thread.
**`Synchronized`** è il meccanismo fondamentale in Java per garantire la **mutua esclusione** a sezioni critiche di codice: **assicura che solo un thread alla volta possa eseguire un blocco di codice sincronizzato**

### ⚠️**Monitor**
Nella programmazione concorrente, un **monitor** è un meccanismo di sincronizzazione che gestisce l'accesso concorrente a una risorsa condivisa da parte di più thread.

Fornisce metodi per:
- **bloccare l'accesso** alla risorsa, permettendo a un solo thread alla volta di accedervi;
- **gestire l'attesa** dei thread quando la risorsa è già in uso;

In Java, ogni oggetto può fungere da monitor. Il monitor viene implementato attraverso la parola chiave `synchronized`: quando un thread esegue un metodo o blocco sincronizzato, acquisisce il lock del monitor e blocca l'accesso ad altri thread fino al rilascio del lock.

Tutti gli oggetti che implementano `java.lang.Object` forniscono i metodi `wait()`, `notify()` e `notifyAll()`, che permettono la sincronizzazione e comunicazione tra thread.

### ⚠️**wait()**
Il metodo `wait()` permette a un thread di:
- sospendere la propria esecuzione;
- rilasciare il lock del monitor;
Oppure permette di rimanere in stato di attesa fino a quando:
- un altro thread chiama `notify()` o `notifyAll()` sullo stesso oggetto;
- scade il timeout specificato (se usato `wait(timeout)`.

Quando risvegliato, il thread deve riacquisire il lock prima di continuare.

### ⚠️**notify()**
Il metodo `motify()` notifica **un singolo thread** in attesa sul monitor. Il thread notificato viene scelto in modo non deterministico dal sistema. Gli altri thread in attesa rimangono sospesi finchè non vengono notificati o scade il loro timeout.

Il thread che chiama `notify()` **non rilascia immediatamente il lock**: continua l'esecuzione e lo rilascia solo quando esce dal blocco sincronizzato.

### ⚠️**notifyAll()**
Il metodo notifyAll() notifica **tutti i thread** in attesa sul monitor. Tutti i thread notificati tentano di acquisire il lock e riprendere l'esecuzione, ma solo uno alla volta può effettivamente procedere.

E' buona pratica **utilizzare sempre** `notifyAll()` **invece di** `notify()`, a meno che non si sappia esattamente quale thread debba essere notificato e si abbia una buona ragione per farlo.
`notifyAll()` garantisce che tutti i thread in attesa vengono notificati, evitando situazioni in cui:
- il thread sbagliato viene risvegliato;
- thread validi rimangono in attesa indefinita (starvation);
- si verificano deadlock.

### ⚠️**Thread Safety** 
La **Thread safety** si riferisce alla capcità di una classe o di una struttura dati di essere **utilizzata da più thread contemporaneamente senza generare errori o risultati imprevedibili**.

La necessità della thread safety emerge a causa di problematiche come la **race condition**, ovvero quando più thread accedono e tentano di modificare contemporaneamente una risorsa condivisa, e il risultato finale dipende dall'ordine non deterministico di esecuzione dei thread, portando a risultati imprevedibili o errati.

Java offre diverse **primitive di sincronizzazione** per gestire thread safety:
- `volatile`: garantisce che le modifiche a una variabile siano **immediatamente visibili a tutti i thread**. 	Assicura che le letture e scritture avvengano direttamente dalla memoria principale, non dalle cache locali dei core.
  
- `synchronized`: garantisce la **mutua esclusione** a sezioni critiche di codice: **assicura che solo un thread alla volta possa eseguire un blocco di codice sincronizzato**.
  
- Comunicazione tra thread con `wait()`, `notify()` e `notifyAll()`.











