# **Metodologie di Programmazione**
## Programmazione Concorrente
### **Definizioni**
**Thread**: sono le più piccole unità di
elaborazione all'interno di un processo. I
thread condividono lo stesso spazio di indirizzi
e risorse di un processo ma eseguono in
modo indipendente all'interno di esso.
Consentono l'esecuzione simultanea di più
attività all'interno di un singolo processo. I
thread all'interno dello stesso processo
condividono lo stesso contesto, compreso lo
spazio di indirizzi e i descrittori di file.

**Binaries**: 
sono programmi dormienti che
risiedono su un supporto di memorizzazione,
compilati in un formato accessibile da un dato
sistema operativo, pronti per essere eseguiti.

**Processo**: è un'istanza di un programma in
esecuzione in un sistema informatico. Sono
l'astrazione del sistema operativo che
rappresentano quei binari in azione (es. il
binario caricato, la memoria virtualizzata, le
risorse del kernel come i file aperti, un utente
associato e così via). Un processo può
contenere uno (**single thread**) o più thread
(**multithread**). 
Ogni processo ha il proprio spazio di indirizzi,
memoria e risorse assegnate, rendendolo
isolato da altri processi.
### **Introduzione**
La programmazione concorrente in Java si riferisce alla scrittura di programmi che possono eseguire **più attività in parallelo**. Questo approccio è particolarmente utile per sfruttare sistemi multi-core e migliorare le prestazioni del software, consentendo a più thread di eseguire operazioni simultaneamente.

Ogni programma che viene mandato in esecuzione sulla JVM dà origine ad un processo.

Per la gestione del **multithreading** la JVM non si affida al sistema operativo ad utilizza una politica di gestione dei thread di tipo **fixed-priority scheduling**. Questa è basata essenzialmente sulla priorità, che si riferisce al livello d'importanza o di urgenza associato a un processo. 
Un processo con una priorità più alta sarà eseguito prima di uno con una priorità bassa, ed è preventiva poichè garantisce che, se in qualunque momento si rende eseguibile un thread con priorità maggiore di quella del thread attualmente in esecuzione, il thread a maggiore priorità prevale sull'altro, fatto salvo casi particolari in cui debbono essere gestite potenziali situazioni di stallo.

Il primo passo nello sviluppo concorrente consiste nel suddividere le attività in task, ciascuno dei quali può essere rappresentato tramite interfacce come `Runnable` o `Callable`, oppure gestito tramite il framework `Executor` di Java. In questo modo, le attività possono essere eseguite in parallelo in modo ordinato e controllato, migliorando l’efficienza e la scalabilità del programma.




