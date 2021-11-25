package app.unattach.utils;

import app.unattach.model.*;
import app.unattach.model.attachmentstorage.FileUserStorage;
import app.unattach.model.attachmentstorage.UserStorage;
import app.unattach.view.Action;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class OfflineProcessor {
  private static final Logger logger = Logger.get();

  public static void main(String[] args) throws MessagingException, IOException {
    if (args.length != 1) {
      logger.error("Please provide the full path to the EML file to process.");
      return;
    }
    String emlFilePath = args[0];
    logger.info("Attempting to process %s...", emlFilePath);
    File emlFile = new File(emlFilePath);
    Session session = Session.getInstance(new Properties());
    try (InputStream inputStream = new FileInputStream(emlFile)) {
      MimeMessage mimeMessage = new MimeMessage(session, inputStream);
      UserStorage userStorage = new FileUserStorage();
      String gmailId = "placeholder-id";
      List<GmailLabel> labels = List.of();
      String from = "john.doe@example.com";
      String to = "jane.doe@example.com";
      String subject = mimeMessage.getSubject();
      long timestamp = mimeMessage.getSentDate().getTime();
      int sizeInBytes = mimeMessage.getSize();
      List<String> knownAttachments = List.of();
      Email email = new Email(gmailId, labels, from, to, subject, timestamp, sizeInBytes, knownAttachments);
      ProcessOption processOption = new ProcessOption(Action.DOWNLOAD_AND_REMOVE, true,
          true, true, false, Constants.DEFAULT_DOWNLOADED_LABEL_NAME,
          Constants.DEFAULT_REMOVED_LABEL_NAME);
      File targetDirectory = emlFile.getParentFile();
      String filenameSchema = FilenameFactory.DEFAULT_SCHEMA;
      SortedMap<String, String> idToLabel = Collections.emptySortedMap();
      ProcessSettings processSettings = new ProcessSettings(processOption, targetDirectory, filenameSchema,
          true, idToLabel);
      logger.info("Using process settings: %s", processSettings);
      Set<String> attachmentNames = new TreeSet<>();
      logger.info("Mime structure before: " + MimeMessagePrettyPrinter.prettyPrint(mimeMessage));
      EmailProcessor.process(userStorage, email, mimeMessage, processSettings, attachmentNames);
      logger.info("Mime structure after: " + MimeMessagePrettyPrinter.prettyPrint(mimeMessage));
      logger.info("Attachment names: " + attachmentNames);
    }
  }
}
