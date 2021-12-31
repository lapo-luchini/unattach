package app.unattach.controller;

import app.unattach.model.*;
import app.unattach.model.attachmentstorage.FileUserStorage;
import app.unattach.model.attachmentstorage.UserStorage;
import app.unattach.model.service.*;
import app.unattach.utils.Clock;
import app.unattach.utils.MockClock;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.Message;
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

import static org.junit.jupiter.api.Assertions.*;

public class GmailServiceTest {
  private Controller controller;

  @BeforeEach
  public void setup() throws GmailServiceManagerException, GmailServiceException {
    try {
      Clock clock = new MockClock();
      Config config = new BaseConfig();
      UserStorage userStorage = new FileUserStorage();
      String emailAddress = "rok.strnisa@gmail.com";
      JsonFactory factory = GsonFactory.getDefaultInstance();
      ListLabelsResponse listLabelsResponse = TestStore.loadLabels(factory);
      SortedMap<String, String> idToLabel = GmailService.labelsResponseToMap(listLabelsResponse);
      Message simpleBefore = TestStore.loadMessage(factory, "1-simple-before");
      Message mixedBefore = TestStore.loadMessage(factory, "2-mixed-before");
      Message noBodyBefore = TestStore.loadMessage(factory, "3-no-body-before");
      Message imageAndPdfBefore = TestStore.loadMessage(factory, "5-image-and-pdf");
      Message smallImageBefore = TestStore.loadMessage(factory, "6-small-image");
      List<Message> messages = Arrays.asList(simpleBefore, mixedBefore, noBodyBefore, imageAndPdfBefore,
          smallImageBefore);
      Map<String, String> beforeIdToAfterId = Map.of(
          simpleBefore.getId(), "1-simple-after",
          mixedBefore.getId(), "2-mixed-after",
          noBodyBefore.getId(), "3-no-body-after",
          imageAndPdfBefore.getId(), "5-image-and-pdf-after",
          smallImageBefore.getId(), "6-small-image-after"
      );
      GmailServiceManager gmailServiceManager =
          new FakeGmailServiceManager(emailAddress, idToLabel, messages, beforeIdToAfterId);
      Model model = new LiveModel(clock, config, userStorage, gmailServiceManager);
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
    testDownloadAndOrRemove(tempDir,  "simple attachment", true, false, true,
        false, "logo-256.png");
  }

  @Test
  void test_getProcessTask_SHOULD_download_backup_and_update_WHEN_downloading_and_removing_simple(@TempDir Path tempDir)
      throws GmailServiceException, LongTaskException, IOException, MessagingException {
    testDownloadAndOrRemove(tempDir, "simple attachment", true, true, true,
        false, "logo-256.png");
  }

  @Test
  void test_getProcessTask_SHOULD_remove_backup_and_update_WHEN_removing_simple(@TempDir Path tempDir)
      throws GmailServiceException, LongTaskException, IOException, MessagingException {
    testDownloadAndOrRemove(tempDir, "simple attachment", false, true, true,
        false, "logo-256.png");
  }

  @Test
  void test_getProcessTask_SHOULD_download_backup_and_not_update_WHEN_downloading_mixed(@TempDir Path tempDir)
      throws GmailServiceException, LongTaskException, IOException, MessagingException {
    testDownloadAndOrRemove(tempDir, "mixed", true, false, true,
        false, "logo-attached.png", "logo-embedded.png");
  }

  @Test
  void test_getProcessTask_SHOULD_download_backup_and_update_WHEN_downloading_and_removing_mixed(@TempDir Path tempDir)
      throws GmailServiceException, LongTaskException, IOException, MessagingException {
    testDownloadAndOrRemove(tempDir, "mixed", true, true, false,
        false, "logo-attached.png");
  }

  @Test
  void test_getProcessTask_SHOULD_remove_backup_and_update_WHEN_downloading_and_removing_mixed(@TempDir Path tempDir)
      throws GmailServiceException, LongTaskException, IOException, MessagingException {
    testDownloadAndOrRemove(tempDir, "mixed", false, true, false,
        false, "logo-attached.png");
  }

  @Test
  void test_getProcessTask_SHOULD_download_backup_and_not_update_WHEN_downloading_no_body(@TempDir Path tempDir)
      throws GmailServiceException, LongTaskException, IOException, MessagingException {
    testDownloadAndOrRemove(tempDir, "PDF attachment", true, false, true,
        false, "Google.pdf");
  }

  @Test
  void test_getProcessTask_SHOULD_download_backup_and_update_WHEN_downloading_and_removing_no_body(@TempDir Path tempDir)
      throws GmailServiceException, LongTaskException, IOException, MessagingException {
    testDownloadAndOrRemove(tempDir, "PDF attachment", true, true, true,
        false, "Google.pdf");
  }

  @Test
  void test_getProcessTask_SHOULD_remove_backup_and_update_WHEN_downloading_and_removing_no_body(@TempDir Path tempDir)
      throws GmailServiceException, LongTaskException, IOException, MessagingException {
    testDownloadAndOrRemove(tempDir, "PDF attachment", false, true, true,
        false, "Google.pdf");
  }

  @Test
  void test_getProcessTask_SHOULD_remove_pdf_and_resize_image_WHEN_removing_and_resizing(@TempDir Path tempDir)
      throws GmailServiceException, LongTaskException, IOException, MessagingException {
    testDownloadAndOrRemove(tempDir, "image and pdf", true, true, true,
        true, "Google.pdf", "logo.png");
  }

  @Test
  void test_getProcessTask_SHOULD_resize_image_but_ignore_pdf_WHEN_resizing_and_not_removing(@TempDir Path tempDir)
      throws GmailServiceException, LongTaskException, IOException, MessagingException {
    testDownloadAndOrRemove(tempDir, "image and pdf", true, false, true,
        true, "Google.pdf", "logo.png");
  }

  @Test
  void test_getProcessTask_SHOULD_not_resize_image_WHEN_resizing_image_is_small(@TempDir Path tempDir)
      throws GmailServiceException, LongTaskException {
    Path firstRunPath = tempDir.resolve("first_run");
    ProcessEmailResult result = processEmail(firstRunPath, "small image", true, true,
        true, true);
    // Check that email ID hasn't changed.
    assertNull(result.newId());
  }

  private void testDownloadAndOrRemove(Path tempDir, String query, boolean download, boolean remove,
                                       boolean processEmbedded, boolean resizeImages, String... attachments)
      throws GmailServiceException, LongTaskException, IOException, MessagingException {
    Path firstRunPath = tempDir.resolve("first_run");
    ProcessEmailResult result = processEmail(firstRunPath, query, download, remove, processEmbedded, resizeImages);

    if (remove || resizeImages) {
      // Check that the email ID has changed.
      assertNotNull(result.newId());
    } else {
      // Check that email ID hasn't changed.
      assertNull(result.newId());
    }

    // Check that an email backup was made.
    File[] emailBackups = getEmailBackups(firstRunPath);
    assertEquals(1, emailBackups.length);
    File emailBackup = emailBackups[0];

    // Check that the original email was changed if the attachments were removed.
    Path secondRunPath = tempDir.resolve("second_run");
    List<ProcessEmailResult> secondResults = processEmails(secondRunPath, query, download, false,
        processEmbedded, false);
    assertEquals(1, secondResults.size());
    File[] secondEmailBackups = getEmailBackups(secondRunPath);
    assertEquals(1, secondEmailBackups.length);
    File newEmailBackup = secondEmailBackups[0];
    if (remove || resizeImages) {
      assertFalse(FileUtils.contentEquals(emailBackup, newEmailBackup));
    } else {
      assertTrue(FileUtils.contentEquals(emailBackup, newEmailBackup));
    }

    for (String attachment : attachments) {
      // Check that the original attachment is in the test store.
      File originalFile = new File("test-store", attachment);
      assertTrue(originalFile.exists(), attachment);
      File fileFirstRun = firstRunPath.resolve("attachments").resolve(attachment).toFile();
      // Check that, if downloading attachments, that it was downloaded and matches the test store one.
      if (download) {
        assertTrue(fileFirstRun.exists(), attachment);
        assertTrue(FileUtils.contentEquals(originalFile, fileFirstRun), attachment);
      }
      File fileSecondRun = secondRunPath.resolve("attachments").resolve(attachment).toFile();
      boolean isImage = EmailProcessor.isSupportedImageFilename(attachment);
      if (isImage && resizeImages) {
        // If attachment is an image, and we're resizing images, the new image must exist and be smaller.
        assertTrue(fileSecondRun.exists(), attachment);
        assertTrue(fileSecondRun.length() < originalFile.length(), attachment);
      } else if (remove) {
        // If instead we're removing attachments, the email must no longer contain the attachment.
        assertFalse(fileSecondRun.exists(), attachment);
      } else {
        // Otherwise, the attachment wasn't modified and should still match the test store one.
        assertTrue(fileSecondRun.exists(), attachment);
        assertTrue(FileUtils.contentEquals(originalFile, fileFirstRun), attachment);
      }
    }

    // If we have modified the email, check that the new email contains metadata.
    if (remove || resizeImages) {
      Session session = Session.getInstance(new Properties());
      try (InputStream inputStream = new FileInputStream(newEmailBackup)) {
        MimeMessage newMimeMessage = new MimeMessage(session, inputStream);
        String content = getMainContent(newMimeMessage.getContent());
        assertTrue(content.contains("Removed/modified attachments"));
        for (String attachment : attachments) {
          boolean isImage = EmailProcessor.isSupportedImageFilename(attachment);
          assertEquals(isImage || remove, content.contains(attachment));
        }
      }
    }
  }

  private ProcessEmailResult processEmail(Path downloadPath, String query, boolean download, boolean remove,
                                          boolean processEmbedded, boolean resizeImages)
      throws GmailServiceException, LongTaskException {
    List<ProcessEmailResult> results = processEmails(downloadPath, query, download, remove, processEmbedded,
        resizeImages);
    assertEquals(1, results.size());
    return results.get(0);
  }

  @SuppressWarnings("SameParameterValue")
  private List<ProcessEmailResult> processEmails(Path downloadPath, String query, boolean download, boolean remove,
                                                 boolean processEmbedded, boolean resizeImages)
      throws GmailServiceException, LongTaskException {
    List<ProcessEmailResult> results = new ArrayList<>();
    for (Email email : searchForEmailsThroughController(query)) {
      String downloadedLabelId = controller.getOrCreateDownloadedLabelId();
      String removedLabelId = controller.getOrCreateRemovedLabelId();
      ProcessOption processOption = new ProcessOption(true, download, remove, resizeImages, processEmbedded,
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