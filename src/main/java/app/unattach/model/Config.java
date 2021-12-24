package app.unattach.model;

import java.util.List;
import java.util.Set;

public interface Config {
  Set<String> getPropertyNames();
  boolean getBackupEmails();
  boolean getDownloadAttachments();
  int getEmailSize();
  String getDateFormat();
  boolean getRemoveOriginal();
  String getFilenameSchema();
  List<String> getLabelIds();
  String getDownloadedLabelId();
  boolean getProcessEmbedded();
  boolean getRemoveAttachments();
  String getRemovedLabelId();
  boolean getReduceImageResolution();
  String getSearchQuery();
  String getSelectedSearchTab();
  boolean getSignInAutomatically();
  boolean getSubscribeToUpdates();
  String getTargetDirectory();
  void saveBackupEmail(boolean backupEmail);
  void saveDateFormat(String pattern);
  void saveDownloadAttachments(boolean downloadAttachments);
  void saveDownloadedLabelId(String downloadedLabelId);
  void saveEmailSize(int emailSize);
  void saveFilenameSchema(String schema);
  void saveLabelIds(List<String> labelIds);
  void saveProcessEmbedded(boolean processEmbedded);
  void saveReduceImageResolution(boolean reduceImageResolution);
  void saveRemoveAttachments(boolean removeAttachments);
  void saveRemoveOriginal(boolean removeOriginal);
  void saveRemovedLabelId(String removedLabelId);
  void saveSearchQuery(String query);
  void saveSelectedSearchTab(String selectedSearchTab);
  void saveSignInAutomatically(boolean signInAutomatically);
  void saveSubscribeToUpdates(boolean subscribeToUpdates);
  void saveTargetDirectory(String path);
}
