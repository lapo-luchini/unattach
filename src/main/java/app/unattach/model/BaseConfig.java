package app.unattach.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class BaseConfig implements Config {
  private static final String BACKUP_EMAIL_PROPERTY = "backup_email";
  private static final String DATE_FORMAT_PROPERTY = "date_format";
  private static final String DOWNLOAD_ATTACHMENTS_PROPERTY = "download_attachments";
  private static final String DOWNLOADED_LABEL_ID_PROPERTY = "downloaded_label_id";
  private static final String EMAIL_SIZE_PROPERTY = "email_size";
  private static final String FILENAME_SCHEMA_PROPERTY = "filename_schema";
  private static final String LABEL_IDS_PROPERTY = "label_ids";
  private static final String PROCESS_EMBEDDED_PROPERTY = "process_embedded";
  private static final String REMOVE_ATTACHMENTS_PROPERTY = "remove_attachments";
  private static final String REMOVED_LABEL_ID_PROPERTY = "removed_label_id";
  private static final String REMOVE_ORIGINAL_PROPERTY = "remove_original";
  private static final String REDUCE_IMAGE_RESOLUTION_PROPERTY = "reduce_image_resolution";
  private static final String SEARCH_QUERY_PROPERTY = "search_query";
  private static final String SELECTED_SEARCH_TAB_PROPERTY = "selected_search_tab";
  private static final String SIGN_IN_AUTOMATICALLY_PROPERTY = "sign_in_automatically";
  private static final String SUBSCRIBE_TO_UPDATES_PROPERTY = "subscribe_to_updates";
  private static final String TARGET_DIRECTORY_PROPERTY = "target_directory";

  private static final Set<String> PROPERTY_NAMES = Set.of(
      BACKUP_EMAIL_PROPERTY,
      DATE_FORMAT_PROPERTY,
      DOWNLOAD_ATTACHMENTS_PROPERTY,
      DOWNLOADED_LABEL_ID_PROPERTY,
      EMAIL_SIZE_PROPERTY,
      FILENAME_SCHEMA_PROPERTY,
      LABEL_IDS_PROPERTY,
      PROCESS_EMBEDDED_PROPERTY,
      REMOVE_ATTACHMENTS_PROPERTY,
      REMOVED_LABEL_ID_PROPERTY,
      REMOVE_ORIGINAL_PROPERTY,
      REDUCE_IMAGE_RESOLUTION_PROPERTY,
      SEARCH_QUERY_PROPERTY,
      SELECTED_SEARCH_TAB_PROPERTY,
      SIGN_IN_AUTOMATICALLY_PROPERTY,
      SUBSCRIBE_TO_UPDATES_PROPERTY,
      TARGET_DIRECTORY_PROPERTY
  );

  protected final Properties config;

  public BaseConfig() {
    config = new Properties();
    loadConfig();
  }

  public void loadConfig() {}

  public void saveConfig() {}

  @Override
  public Set<String> getPropertyNames() {
    return PROPERTY_NAMES;
  }

  @Override
  public boolean getBackupEmails() {
    return Boolean.parseBoolean(config.getProperty(BACKUP_EMAIL_PROPERTY, "true"));
  }

  @Override
  public boolean getDownloadAttachments() {
    return Boolean.parseBoolean(config.getProperty(DOWNLOAD_ATTACHMENTS_PROPERTY, "true"));
  }

  @Override
  public int getEmailSize() {
    return Integer.parseInt(config.getProperty(EMAIL_SIZE_PROPERTY, "5"));
  }

  @Override
  public String getDateFormat() {
    return config.getProperty(DATE_FORMAT_PROPERTY, DateFormat.ISO_8601_DATE.getPattern());
  }

  @Override
  public boolean getRemoveOriginal() {
    return Boolean.parseBoolean(config.getProperty(REMOVE_ORIGINAL_PROPERTY, "true"));
  }

  @Override
  public String getFilenameSchema() {
    return config.getProperty(FILENAME_SCHEMA_PROPERTY, FilenameFactory.DEFAULT_SCHEMA);
  }

  @Override
  public List<String> getLabelIds() {
    return Arrays.asList(config.getProperty(LABEL_IDS_PROPERTY, "").split(","));
  }

  @Override
  public String getDownloadedLabelId() {
    return config.getProperty(DOWNLOADED_LABEL_ID_PROPERTY);
  }

  @Override
  public boolean getProcessEmbedded() {
    return Boolean.parseBoolean(config.getProperty(PROCESS_EMBEDDED_PROPERTY, "true"));
  }

  @Override
  public boolean getRemoveAttachments() {
    return Boolean.parseBoolean(config.getProperty(PROCESS_EMBEDDED_PROPERTY, "false"));
  }

  @Override
  public String getRemovedLabelId() {
    return config.getProperty(REMOVED_LABEL_ID_PROPERTY);
  }

  @Override
  public boolean getReduceImageResolution() {
    return Boolean.parseBoolean(config.getProperty(REDUCE_IMAGE_RESOLUTION_PROPERTY, "false"));
  }

  @Override
  public String getSearchQuery() {
    return config.getProperty(SEARCH_QUERY_PROPERTY, "has:attachment size:1m");
  }

  @Override
  public String getSelectedSearchTab() {
    return config.getProperty(SELECTED_SEARCH_TAB_PROPERTY);
  }

  @Override
  public boolean getSignInAutomatically() {
    return Boolean.parseBoolean(config.getProperty(SIGN_IN_AUTOMATICALLY_PROPERTY, "false"));
  }

  @Override
  public boolean getSubscribeToUpdates() {
    return Boolean.parseBoolean(config.getProperty(SUBSCRIBE_TO_UPDATES_PROPERTY, "true"));
  }

  @Override
  public String getTargetDirectory() {
    return config.getProperty(TARGET_DIRECTORY_PROPERTY, getDefaultTargetDirectory());
  }

  @Override
  public void saveBackupEmail(boolean backupEmail) {
    config.setProperty(BACKUP_EMAIL_PROPERTY, Boolean.toString(backupEmail));
    saveConfig();
  }

  @Override
  public void saveDateFormat(String pattern) {
    config.setProperty(DATE_FORMAT_PROPERTY, pattern);
    saveConfig();
  }

  @Override
  public void saveDownloadAttachments(boolean downloadAttachments) {
    config.setProperty(DOWNLOAD_ATTACHMENTS_PROPERTY, Boolean.toString(downloadAttachments));
    saveConfig();
  }

  @Override
  public void saveFilenameSchema(String schema) {
    config.setProperty(FILENAME_SCHEMA_PROPERTY, schema);
    saveConfig();
  }

  @Override
  public void saveLabelIds(List<String> labelIds) {
    config.setProperty(LABEL_IDS_PROPERTY, String.join(",", labelIds));
    saveConfig();
  }

  @Override
  public void saveProcessEmbedded(boolean processEmbedded) {
    config.setProperty(PROCESS_EMBEDDED_PROPERTY, Boolean.toString(processEmbedded));
    saveConfig();
  }

  @Override
  public void saveDownloadedLabelId(String downloadedLabelId) {
    config.setProperty(DOWNLOADED_LABEL_ID_PROPERTY, downloadedLabelId);
    saveConfig();
  }

  @Override
  public void saveRemovedLabelId(String removedLabelId) {
    config.setProperty(REMOVED_LABEL_ID_PROPERTY, removedLabelId);
    saveConfig();
  }

  @Override
  public void saveReduceImageResolution(boolean reduceImageResolution) {
    config.setProperty(REDUCE_IMAGE_RESOLUTION_PROPERTY, Boolean.toString(reduceImageResolution));
    saveConfig();
  }

  @Override
  public void saveRemoveAttachments(boolean removeAttachments) {
    config.setProperty(REMOVE_ATTACHMENTS_PROPERTY, Boolean.toString(removeAttachments));
    saveConfig();
  }

  @Override
  public void saveSearchQuery(String query) {
    config.setProperty(SEARCH_QUERY_PROPERTY, query);
    saveConfig();
  }

  @Override
  public void saveSelectedSearchTab(String selectedSearchTab) {
    config.setProperty(SELECTED_SEARCH_TAB_PROPERTY, selectedSearchTab);
    saveConfig();
  }

  @Override
  public void saveSignInAutomatically(boolean signInAutomatically) {
    config.setProperty(SIGN_IN_AUTOMATICALLY_PROPERTY, Boolean.toString(signInAutomatically));
    saveConfig();
  }

  @Override
  public void saveTargetDirectory(String path) {
    config.setProperty(TARGET_DIRECTORY_PROPERTY, path);
    saveConfig();
  }

  @Override
  public void saveSubscribeToUpdates(boolean subscribeToUpdates) {
    config.setProperty(SUBSCRIBE_TO_UPDATES_PROPERTY, Boolean.toString(subscribeToUpdates));
    saveConfig();
  }

  @Override
  public void saveEmailSize(int emailSize) {
    config.setProperty(EMAIL_SIZE_PROPERTY, Integer.toString(emailSize));
    saveConfig();
  }

  @Override
  public void saveRemoveOriginal(boolean removeOriginal) {
    config.setProperty(REMOVE_ORIGINAL_PROPERTY, Boolean.toString(removeOriginal));
    saveConfig();
  }

  private static String getDefaultTargetDirectory() {
    String userHome = System.getProperty("user.home");
    Path defaultPath = Paths.get(userHome, "Downloads", Constants.PRODUCT_NAME);
    return defaultPath.toString();
  }
}
