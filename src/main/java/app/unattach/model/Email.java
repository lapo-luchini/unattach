package app.unattach.model;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.fxml.FXML;
import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Email implements Observable {
  private String gmailId;
  private final SortedSet<GmailLabel> labels;
  private final String from;
  private final String to;
  private final String subject;
  private final long timestamp;
  private final Date date;
  private final int sizeInBytes;
  private final List<String> attachments;
  private EmailStatus status;
  private String processLog;

  private final List<InvalidationListener> listeners = new ArrayList<>();

  public Email(String gmailId, List<GmailLabel> labels, String from, String to, String subject, long timestamp,
               int sizeInBytes, List<String> attachments) {
    this.gmailId = gmailId;
    this.labels = Collections.unmodifiableSortedSet(labels == null ?
        Collections.emptySortedSet() : new TreeSet<>(labels));
    this.from = from;
    this.to = to;
    this.subject = subject;
    this.timestamp = timestamp;
    this.date = new Date(timestamp);
    this.sizeInBytes = sizeInBytes;
    this.attachments = attachments;
    status = EmailStatus.NOT_SELECTED;
    processLog = "";
  }

  @FXML
  public String getGmailId() {
    return gmailId;
  }

  public void setGmailId(String gmailId) {
    this.gmailId = gmailId;
  }

  @SuppressWarnings("unused")
  @FXML
  public String getLabelNamesDelimited() {
    return labels.stream().map(GmailLabel::name).collect(Collectors.joining(", "));
  }

  @FXML
  public Date getDate() {
    return date;
  }

  public SortedSet<GmailLabel> getLabels() {
    return labels;
  }

  String getDateIso8601() {
    return new SimpleDateFormat("yyyy-MM-dd").format(getDate());
  }

  String getTimeString() {
    return new SimpleDateFormat("HH-mm-ss").format(getDate());
  }

  @FXML
  public String getFrom() {
    return from;
  }

  @FXML
  public String getTo() {
    return to;
  }

  String getFromEmail() {
    if (from == null) {
      return "";
    } else if (from.endsWith(">")) {
      return from.substring(from.lastIndexOf('<') + 1, from.length() - 1);
    } else {
      return from;
    }
  }

  String getFromName() {
    if (from.endsWith(">")) {
      return StringUtils.strip(from.substring(0, from.lastIndexOf('<')), " \"");
    } else {
      return "";
    }
  }

  @FXML
  public String getSubject() {
    return subject;
  }

  public int getSizeInBytes() {
    return sizeInBytes;
  }

  @FXML
  public double getSizeInMegaBytes() {
    // Round to 1 decimal place.
    return Math.round(10d * sizeInBytes / Constants.BYTES_IN_MEGABYTE) / 10d;
  }

  @FXML
  @SuppressWarnings("unused")
  public String getAttachments() {
    return String.join(", ", attachments);
  }

  @FXML
  public boolean isSelected() {
    return status == EmailStatus.TO_PROCESS;
  }

  @FXML
  public EmailStatus getStatus() {
    return status;
  }

  @FXML
  public void setStatus(EmailStatus status) {
    this.status = status;
    for (InvalidationListener listener : listeners) {
      listener.invalidated(this);
    }
  }

  long getTimestamp() {
    return timestamp;
  }

  @FXML
  @SuppressWarnings("unused")
  public String getProcessLog() {
    return processLog;
  }

  @FXML
  public void setProcessLog(String processLog) {
    this.processLog = processLog;
  }

  @Override
  public void addListener(InvalidationListener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(InvalidationListener listener) {
    listeners.remove(listener);
  }

  @Override
  public String toString() {
    return "Email{" +
            "gmailId='" + gmailId + '\'' +
            ", labels=" + labels +
            ", from='" + from + '\'' +
            ", subject='" + subject + '\'' +
            ", timestamp=" + timestamp +
            ", date=" + date +
            ", sizeInBytes=" + sizeInBytes +
            ", status=" + status +
            ", processLog='" + processLog + '\'' +
            '}';
  }
}
