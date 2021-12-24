package app.unattach.model;

public record ProcessOption(boolean backupEmail, boolean downloadAttachments, boolean removeAttachments,
                            boolean reduceImageResolution, boolean processEmbeddedAttachments,
                            boolean permanentlyRemoveOriginalEmail, String downloadedLabelId, String removedLabelId) {}
