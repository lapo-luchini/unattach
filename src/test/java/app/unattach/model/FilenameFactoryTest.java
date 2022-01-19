package app.unattach.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class FilenameFactoryTest {
  private Email email;

  @BeforeEach
  public void setUp() {
    email = createEmail("\"Rok Strniša\" <rok.strnisa@gmail.com>");
  }

  @Test
  public void testFromEmail() {
    testGetFilename("${FROM_EMAIL}", "a%b@.jpg", "rok.strnisa@gmail.com");
    testGetFilename("${FROM_EMAIL:3}", "a%b@.jpg", "rok");
    testGetFilename("${FROM_EMAIL:-9}", "a%b@.jpg", "gmail.com");
    testGetFilename("${FROM_EMAIL:-42}", "a%b@.jpg", "rok.strnisa@gmail.com");
  }

  @Test
  public void testFromName() {
    testGetFilename("${FROM_NAME}", "a%b@.jpg", "Rok Strniša");
    testGetFilename("${FROM_NAME:3}", "a%b@.jpg", "Rok");
  }

  @Test
  public void testFromNameOrEmail() {
    testGetFilename("${FROM_NAME_OR_EMAIL}", "a%b@.jpg", "Rok Strniša");
    testGetFilename("${FROM_NAME_OR_EMAIL:3}", "a%b@.jpg", "Rok");
    email = createEmail("rok.strnisa@gmail.com");
    testGetFilename("${FROM_NAME_OR_EMAIL}", "a%b@.jpg", "rok.strnisa@gmail.com");
    testGetFilename("${FROM_NAME_OR_EMAIL:5}", "a%b@.jpg", "rok.s");
  }

  @Test
  public void testSubject() {
    testGetFilename("${SUBJECT}", "a%b@.jpg", "subject");
    testGetFilename("${SUBJECT:3}", "a%b@.jpg", "sub");
  }

  @Test
  public void testTimestamp() {
    testGetFilename("${TIMESTAMP}", "a%b@.jpg", "1501545600000");
    testGetFilename("${TIMESTAMP:3}", "a%b@.jpg", "150");
  }

  @Test
  public void testDate() {
    String dateString = new SimpleDateFormat("yyyy-MM-dd").format(email.getDate());
    testGetFilename("${DATE}", "a%b@.jpg", dateString);
    testGetFilename("${DATE:7}", "a%b@.jpg", dateString.substring(0, 7));
  }

  @Test
  public void testTime() {
    String timeString = new SimpleDateFormat("HH-mm-ss").format(email.getDate());
    testGetFilename("${TIME}", "a%b@.jpg", timeString);
    testGetFilename("${TIME:7}", "a%b@.jpg", timeString.substring(0, 7));
  }

  @Test
  public void testEmailId() {
    testGetFilename("${ID}", "a%b@.jpg", "id3");
    testGetFilename("${ID:2}", "a%b@.jpg","id");
  }

  @Test
  public void testBodyPartIndex() {
    testGetFilename("${BODY_PART_INDEX}", "a%b@.jpg", "234");
    testGetFilename("${BODY_PART_INDEX:1}", "a%b@.jpg", "2");
  }

  @Test
  public void testAttachmentName() {
    testGetFilename("${RAW_ATTACHMENT_NAME}", "a%b~.jpg", "a%b~.jpg");
    testGetFilename("${RAW_ATTACHMENT_NAME:3}", "a%b~", "a%b");
    testGetFilename("${RAW_ATTACHMENT_NAME:6}", "a%b~.jpg", "a%.jpg");
  }

  @Test
  public void testNormalizedAttachmentName() {
    testGetFilename("${ATTACHMENT_NAME}", "a:b~.jpg", "a.b~.jpg");
    testGetFilename("${ATTACHMENT_NAME:3}", "a%b~", "a%b");
    testGetFilename("${ATTACHMENT_NAME:6}", "a%b~.jpg", "a%.jpg");
    testGetFilename("${ATTACHMENT_NAME:-2}", "a%b~.jpg", "pg");
  }

  @Test
  public void testNormalizedAttachmentBase() {
    testGetFilename("${ATTACHMENT_BASE}", "a:b~.jpg", "a.b~");
    testGetFilename("${ATTACHMENT_BASE:2}", "a%b~", "a%");
    testGetFilename("${ATTACHMENT_BASE:5}", "a%b~.jpg", "a%b~");
  }

  @Test
  public void testNormalizedAttachmentExtension() {
    testGetFilename("${ATTACHMENT_EXTENSION}", "a:b~.jpg", "jpg");
    testGetFilename("${ATTACHMENT_EXTENSION:2}", "a%b~.jpg", "jp");
    testGetFilename("${ATTACHMENT_EXTENSION:5}", "a%b~", "");
  }

  @Test
  public void testSpecialCharacters() {
    testGetFilename("${ATTACHMENT_NAME}",
        "<(*Öptions*)>: \"one\", \\'二'/-/_ & |three?.jpg",
        "_(_Öptions_)_. 'one', ,'二',-,_ & ,three..jpg");
  }

    @Test
  public void testLabels() {
    testGetFilename("${LABELS}", "a%b@.jpg", "LABEL_42_IMPORTANT_LABEL_13");
  }

  @Test
  public void testLabelNames() {
    testGetFilename("${LABEL_NAMES}", "a%b@.jpg",
        "Friends' Files, IMPORTANT, Unattach - Downloaded");
  }

  @Test
  public void testCustomLabelNames() {
    testGetFilename("${CUSTOM_LABEL_NAMES}", "a%b@.jpg", "Friends' Files");
  }

  @Test
  public void testUnknownPlaceholder() {
    assertThrows(InvalidParameterException.class, () ->
        testGetFilename("${FOO}", "a%b@.jpg", "a%b@.jpg")
    );
  }

  private static Email createEmail(String from) {
    List<GmailLabel> labels = Arrays.asList(
        new GmailLabel("IMPORTANT", "IMPORTANT"),
        new GmailLabel("LABEL_13", "Unattach - Downloaded"),
        new GmailLabel("LABEL_42", "Friends' Files"));
    return new Email("id3", labels,
        from, "to@example.com", "subject", 1501545600000L,
        32141, Collections.singletonList("data.zip"));
  }

  private void testGetFilename(String schema, String attachmentName, String expectedFilename) {
    FilenameFactory filenameFactory = new FilenameFactory(schema, Set.of("LABEL_13"));
    String actualFilename = filenameFactory.getFilename(email, 234, attachmentName);
    assertEquals(expectedFilename, actualFilename);
  }
}