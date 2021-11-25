package app.unattach.controller;

import app.unattach.model.*;
import app.unattach.model.attachmentstorage.FileUserStorage;
import app.unattach.model.attachmentstorage.UserStorage;
import app.unattach.model.service.*;
import app.unattach.view.Action;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.Message;
import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static app.unattach.view.Action.*;
import static org.junit.jupiter.api.Assertions.*;

public class GmailServiceTest {
  private Controller controller;

  @BeforeEach
  public void setup() throws GmailServiceManagerException, GmailServiceException {
    try {
      UserStorage userStorage = new FileUserStorage();
      String emailAddress = "rok.strnisa@gmail.com";
      JsonFactory factory = JacksonFactory.getDefaultInstance();
      ListLabelsResponse listLabelsResponse = TestStore.loadLabels(factory);
      SortedMap<String, String> idToLabel = GmailService.labelsResponseToMap(listLabelsResponse);
      Message simpleBefore = TestStore.loadMessage(factory, "1-simple-before");
      Message mixedBefore = TestStore.loadMessage(factory, "2-mixed-before");
      Message noBodyBefore = TestStore.loadMessage(factory, "3-no-body-before");
      Message imageResizeBefore = TestStore.loadMessage(factory, "5-image-resize");
      List<Message> messages = Arrays.asList(simpleBefore, mixedBefore, noBodyBefore, imageResizeBefore);
      Map<String, String> beforeIdToAfterId = Map.of(
          simpleBefore.getId(), "1-simple-after",
          mixedBefore.getId(), "2-mixed-after",
          noBodyBefore.getId(), "3-no-body-after",
          imageResizeBefore.getId(), "5-image-resize-after"
      );
      GmailServiceManager gmailServiceManager =
          new FakeGmailServiceManager(emailAddress, idToLabel, messages, beforeIdToAfterId);
      Config config = new BaseConfig();
      Model model = new LiveModel(config, userStorage, gmailServiceManager);
      controller = new DefaultController(model);
      assertEquals(controller.signIn(), emailAddress);
    } catch (IOException e) {
      throw new GmailServiceManagerException(e);
    }
  }

  @Test
  void test_getOrCreateLabelId_SHOULD_work_WHEN_label_already_exists() {
    String downloadedLabelId = controller.getOrCreateDownloadedLabelId();
    assertEquals("Label_11", downloadedLabelId);
    String removedLabelId = controller.getOrCreateRemovedLabelId();
    assertEquals("Label_10", removedLabelId);
  }

  @Test
  void test_getSearchTask_SHOULD_work_WHEN_query_is_substring_of_subject() throws GmailServiceException,
      LongTaskException {
    List<Email> emails = searchForEmailsThroughController("simple attachment");
    assertEquals(1, emails.size());
  }

  @Test
  void test_getProcessTask_SHOULD_download_backup_and_not_update_WHEN_downloading_simple(@TempDir Path tempDir)
      throws GmailServiceException, LongTaskException, IOException, MessagingException {
    testDownloadAndOrRemove(tempDir,  "simple attachment", DOWNLOAD, true, false,
        "logo-256.png");
  }

  @Test
  void test_getProcessTask_SHOULD_download_backup_and_update_WHEN_downloading_and_removing_simple(@TempDir Path tempDir)
      throws GmailServiceException, LongTaskException, IOException, MessagingException {
    testDownloadAndOrRemove(tempDir, "simple attachment", DOWNLOAD_AND_REMOVE, true,
        false, "logo-256.png");
  }

  @Test
  void test_getProcessTask_SHOULD_remove_backup_and_update_WHEN_removing_simple(@TempDir Path tempDir)
      throws GmailServiceException, LongTaskException, IOException, MessagingException {
    testDownloadAndOrRemove(tempDir, "simple attachment", REMOVE, true,
        false, "logo-256.png");
  }

  @Test
  void test_getProcessTask_SHOULD_download_backup_and_not_update_WHEN_downloading_mixed(@TempDir Path tempDir)
      throws GmailServiceException, LongTaskException, IOException, MessagingException {
    testDownloadAndOrRemove(tempDir, "mixed", DOWNLOAD, true, false,
        "logo-attached.png", "logo-embedded.png");
  }

  @Test
  void test_getProcessTask_SHOULD_download_backup_and_update_WHEN_downloading_and_removing_mixed(@TempDir Path tempDir)
      throws GmailServiceException, LongTaskException, IOException, MessagingException {
    testDownloadAndOrRemove(tempDir, "mixed", DOWNLOAD_AND_REMOVE, false, false,
        "logo-attached.png");
  }

  @Test
  void test_getProcessTask_SHOULD_remove_backup_and_update_WHEN_downloading_and_removing_mixed(@TempDir Path tempDir)
      throws GmailServiceException, LongTaskException, IOException, MessagingException {
    testDownloadAndOrRemove(tempDir, "mixed", REMOVE, false, false,
        "logo-attached.png");
  }

  @Test
  void test_getProcessTask_SHOULD_download_backup_and_not_update_WHEN_downloading_no_body(@TempDir Path tempDir)
      throws GmailServiceException, LongTaskException, IOException, MessagingException {
    testDownloadAndOrRemove(tempDir, "PDF attachment", DOWNLOAD, true, false,
        "Google.pdf");
  }

  @Test
  void test_getProcessTask_SHOULD_download_backup_and_update_WHEN_downloading_and_removing_no_body(@TempDir Path tempDir)
      throws GmailServiceException, LongTaskException, IOException, MessagingException {
    testDownloadAndOrRemove(tempDir, "PDF attachment", DOWNLOAD_AND_REMOVE, true,
        false, "Google.pdf");
  }

  @Test
  void test_getProcessTask_SHOULD_remove_backup_and_update_WHEN_downloading_and_removing_no_body(@TempDir Path tempDir)
      throws GmailServiceException, LongTaskException, IOException, MessagingException {
    testDownloadAndOrRemove(tempDir, "PDF attachment", REMOVE, true, false,
        "Google.pdf");
  }

  @Test
  void test_getProcessTask_SHOULD_resize_image_WHEN_removing(@TempDir Path tempDir)
      throws GmailServiceException, LongTaskException, IOException, MessagingException {
    testDownloadAndOrRemove(tempDir, "image resize", DOWNLOAD_AND_REMOVE, true, true,
        "logo.png");
  }

  private void testDownloadAndOrRemove(Path tempDir, String query, Action action, boolean processEmbedded,
                                       boolean resizeImages, String... attachments)
      throws GmailServiceException, LongTaskException, IOException, MessagingException {
    Path firstRunPath = tempDir.resolve("first_run");
    ProcessEmailResult result = processEmail(firstRunPath, query, action, processEmbedded, resizeImages);

    if (action == DOWNLOAD) {
      // Check that email ID hasn't changed.
      assertNull(result.newId());
    } else {
      // Check that the email ID has changed.
      assertNotNull(result.newId());
    }

    boolean checkDownloadedFiles = action == DOWNLOAD || action == DOWNLOAD_AND_REMOVE;
    File emailBackup = checkDownloadsAndGetEmailBackup(firstRunPath, result, checkDownloadedFiles, attachments);

    // Check that the original email was changed if attachments were removed.
    Path secondRunPath = tempDir.resolve("second_run");
    List<ProcessEmailResult> secondResults = processEmails(secondRunPath, query, DOWNLOAD, processEmbedded,
        false);
    assertEquals(1, secondResults.size());
    File[] secondEmailBackups = getEmailBackups(secondRunPath);
    assertEquals(1, secondEmailBackups.length);
    File newEmailBackup = secondEmailBackups[0];
    if (action == DOWNLOAD) {
      assertTrue(FileUtils.contentEquals(emailBackup, newEmailBackup));
    } else {
      assertFalse(FileUtils.contentEquals(emailBackup, newEmailBackup));
    }

    // If resizing images, check that the image was resized. Assumes all attachments are images.
    if (resizeImages) {
      ProcessEmailResult secondResult = secondResults.get(0);
      for (String filename : secondResult.filenames()) {
        File imageBefore = firstRunPath.resolve("attachments").resolve(filename).toFile();
        File imageAfter = secondRunPath.resolve("attachments").resolve(filename).toFile();
        assertTrue(imageBefore.exists());
        assertTrue(imageAfter.exists());
        assertTrue(imageBefore.length() > 0);
        assertTrue(imageAfter.length() > 0);
        assertTrue(imageAfter.length() < imageBefore.length());
      }
    }

    if (action != DOWNLOAD) {
      // Sanity check the new email backup.
      assertTrue(newEmailBackup.length() < emailBackup.length());
      Session session = Session.getInstance(new Properties());
      try (InputStream inputStream = new FileInputStream(newEmailBackup)) {
        MimeMessage newMimeMessage = new MimeMessage(session, inputStream);
        String content = getMainContent(newMimeMessage.getContent());
        assertTrue(content.contains("Removed/modified attachments"));
        for (String attachment : attachments) {
          assertTrue(content.contains(attachment));
        }
      }
    }
  }

  private File checkDownloadsAndGetEmailBackup(Path downloadPath, ProcessEmailResult result, boolean checkDownloadedFiles,
                                               String... attachments)
      throws IOException {
    // Check that the right attachments were found.
    HashSet<String> expectedAttachments = Sets.newHashSet(attachments);
    assertEquals(expectedAttachments, result.filenames());

    if (checkDownloadedFiles) {
      // Check that the attachments were correctly backed up.
      Set<String> attachmentsFound = checkFilesEqual(downloadPath.resolve("attachments"), Path.of("test-store"));
      assertEquals(expectedAttachments, attachmentsFound);
    }

    // Check that an email backup was made.
    File[] emailBackups = getEmailBackups(downloadPath);
    assertEquals(1, emailBackups.length);
    return emailBackups[0];
  }

  private ProcessEmailResult processEmail(Path downloadPath, String query, Action action, boolean processEmbedded,
                                          boolean resizeImages) throws GmailServiceException, LongTaskException {
    List<ProcessEmailResult> results = processEmails(downloadPath, query, action, processEmbedded, resizeImages);
    assertEquals(1, results.size());
    return results.get(0);
  }

  @SuppressWarnings("SameParameterValue")
  private List<ProcessEmailResult> processEmails(Path downloadPath, String query, Action action, boolean processEmbedded,
                                                 boolean resizeImages) throws GmailServiceException, LongTaskException {
    List<ProcessEmailResult> results = new ArrayList<>();
    for (Email email : searchForEmailsThroughController(query)) {
      String downloadedLabelId = controller.getOrCreateDownloadedLabelId();
      String removedLabelId = controller.getOrCreateRemovedLabelId();
      ProcessOption processOption = new ProcessOption(action, processEmbedded, resizeImages, true,
          true, downloadedLabelId, removedLabelId);
      String filenameSchema = "attachments/${ATTACHMENT_NAME}";
      SortedMap<String, String> idToLabel = controller.getIdToLabel();
      ProcessSettings processSettings =
          new ProcessSettings(processOption, downloadPath.toFile(), filenameSchema, true, idToLabel);
      LongTask<ProcessEmailResult> task = controller.getProcessTask(email, processSettings);
      results.add(task.takeStep());
    }
    return results;
  }

  private List<Email> searchForEmailsThroughController(@SuppressWarnings("SameParameterValue") String query)
      throws GmailServiceException, LongTaskException {
    GetEmailMetadataTask searchTask = controller.getSearchTask(query);
    while (searchTask.hasMoreSteps()) {
      searchTask.takeStep();
    }
    return controller.getSearchResults();
  }

  private Set<String> checkFilesEqual(Path directoryOfFilesToCheck, Path expectedFilesDirectory) throws IOException {
    File[] files = directoryOfFilesToCheck.toFile().listFiles();
    assertNotNull(files);
    for (File file : files) {
      File expectedFile = expectedFilesDirectory.resolve(file.getName()).toFile();
      assertTrue(expectedFile.exists());
      assertTrue(FileUtils.contentEquals(expectedFile, file));
    }
    return Arrays.stream(files).map(File::getName).collect(Collectors.toSet());
  }

  private File[] getEmailBackups(Path downloadPath) {
    File[] files = downloadPath.toFile().listFiles((dir, name) -> name.endsWith(".eml"));
    assertNotNull(files);
    return files;
  }

  private String getMainContent(Object content) throws MessagingException, IOException {
    if (content instanceof Multipart multipart) {
      for (int i = 0; i < multipart.getCount(); ++i) {
        BodyPart bodyPart = multipart.getBodyPart(i);
        if (bodyPart.isMimeType("text/plain") || bodyPart.isMimeType("text/html")) {
          return bodyPart.getContent().toString();
        }
        String result = getMainContent(bodyPart.getContent());
        if (result != null) {
          return result;
        }
      }
    }
    return null;
  }
}