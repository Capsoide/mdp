package it.unicam.cs.mpgc.jbudget122631.application.usecase;

import java.time.LocalDateTime;

public class SyncDataUseCase {

    private final SyncService syncService;

    public SyncDataUseCase(SyncService syncService) {
        this.syncService = syncService;
    }

    public SyncResult execute() {
        try {
            String localData = syncService.exportLocalData();

            SyncResult result = syncService.synchronizeWithRemote(localData);

            if (result.hasRemoteChanges()) {
                syncService.importRemoteChanges(result.getRemoteData());
            }

            syncService.updateLastSyncTimestamp(LocalDateTime.now());

            return result;

        } catch (Exception e) {
            return SyncResult.failure("Errore durante sincronizzazione: " + e.getMessage());
        }
    }

    public interface SyncService {
        String exportLocalData();
        SyncResult synchronizeWithRemote(String localData);
        void importRemoteChanges(String remoteData);
        void updateLastSyncTimestamp(LocalDateTime timestamp);
    }

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