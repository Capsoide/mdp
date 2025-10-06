# **Metodologie di Programmazione**
## Programmazione Concorrente
### **Introduzione**
La programmazione concorrente in Java si riferisce alla scrittura di programmi che possono eseguire **più attività in parallelo**. Questo approccio è particolarmente utile per sfruttare sistemi multi-core e migliorare le prestazioni del software, consentendo a più thread di eseguire operazioni simultaneamente.

Ogni programma che viene mandato in esecuzione sulla JVM dà origine ad un processo.

Per la gestione del **multithreading** la JVM non si affida al sistema operativo ad utilizza una politica di gestione dei thread di tipo **fixed-priority scheduling**. Questa è basata essenzialmente sulla priorità, che si riferisce al livello d'importanza o di urgenza associato a un processo. 
Un processo con una priorità più alta sarà eseguito prima di uno con una priorità bassa, ed è preventiva poichè garantisce che, se in qualunque momento si rende eseguibile un thread con priorità maggiore di quella del thread attualmente in esecuzione, il thread a maggiore priorità prevale sull'altro, fatto salvo casi particolari in cui debbono essere gestite potenziali situazioni di stallo.

Il primo passo nello sviluppo concorrente consiste nel suddividere le attività in task, ciascuno dei quali può essere rappresentato tramite interfacce come Runnable o Callable, oppure gestito tramite il framework Executor di Java. In questo modo, le attività possono essere eseguite in parallelo in modo ordinato e controllato, migliorando l’efficienza e la scalabilità del programma.



