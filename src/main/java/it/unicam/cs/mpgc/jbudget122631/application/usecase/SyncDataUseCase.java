package it.unicam.cs.mpgc.jbudget122631.application.usecase;

import java.time.LocalDateTime;

public class SyncDataUseCase {

    private final SyncService syncService;

    public SyncDataUseCase(SyncService syncService) {
        this.syncService = syncService;
    }

    public SyncResult execute() {
        try {
            // Esporta dati locali
            String localData = syncService.exportLocalData();

            // Sincronizza con il servizio remoto
            SyncResult result = syncService.synchronizeWithRemote(localData);

            // Importa cambiamenti se presenti
            if (result.hasRemoteChanges()) {
                syncService.importRemoteChanges(result.getRemoteData());
            }

            // Aggiorna timestamp ultima sincronizzazione
            syncService.updateLastSyncTimestamp(LocalDateTime.now());

            return result;

        } catch (Exception e) {
            return SyncResult.failure("Errore durante sincronizzazione: " + e.getMessage());
        }
    }

    // Interfaccia per il servizio di sincronizzazione (estendibile)
    public interface SyncService {
        String exportLocalData();
        SyncResult synchronizeWithRemote(String localData);
        void importRemoteChanges(String remoteData);
        void updateLastSyncTimestamp(LocalDateTime timestamp);
    }

    // Risultato della sincronizzazione
    public static class SyncResult {
        private final boolean success;
        private final String message;
        private final String remoteData;
        private final LocalDateTime timestamp;

        private SyncResult(boolean success, String message, String remoteData) {
            this.success = success;
            this.message = message;
            this.remoteData = remoteData;
            this.timestamp = LocalDateTime.now();
        }

        public static SyncResult success(String remoteData) {
            return new SyncResult(true, "Sincronizzazione completata", remoteData);
        }

        public static SyncResult failure(String message) {
            return new SyncResult(false, message, null);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getRemoteData() { return remoteData; }
        public boolean hasRemoteChanges() { return remoteData != null && !remoteData.isEmpty(); }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}