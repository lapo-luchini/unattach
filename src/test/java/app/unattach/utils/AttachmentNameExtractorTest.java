package app.unattach.utils;

import app.unattach.model.TestStore;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.model.Message;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AttachmentNameExtractorTest {
  @Test
  void test_getAttachmentNames_SHOULD_extract_all_names_WHEN_there_are_sub_parts() throws IOException {
    JsonFactory factory = GsonFactory.getDefaultInstance();
    Message message = TestStore.loadMessage(factory, "4-attachment-names");
    List<String> attachmentNames = AttachmentNameExtractor.getAttachmentNames(message);
    assertEquals(List.of("test1.jpg", "test2.jpg"), attachmentNames);
  }
}