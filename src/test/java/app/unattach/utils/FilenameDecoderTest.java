package app.unattach.utils;

import org.junit.jupiter.api.Test;

import javax.mail.internet.MimeUtility;
import java.io.UnsupportedEncodingException;

import static org.junit.jupiter.api.Assertions.*;

class FilenameDecoderTest {
  @Test
  void decode_SHOULD_the_same_WHEN_no_special_characters() {
    String encodedFilename = "test";
    String decodedFilename = FilenameDecoder.decode(encodedFilename);
    assertEquals(encodedFilename, decodedFilename);
  }

  @Test
  void decode_SHOULD_trim_whitespace_WHEN_filename_starts_or_ends_with_whitespace() {
    String encodedFilename = "  test  ";
    String decodedFilename = FilenameDecoder.decode(encodedFilename);
    assertEquals(encodedFilename.trim(), decodedFilename);
  }

  @Test
  void decode_SHOULD_the_same_WHEN_the_filename_is_similar_to_encoding() {
    String encodedFilename = "=?iso-8859-8?";
    String decodedFilename = FilenameDecoder.decode(encodedFilename);
    assertEquals(encodedFilename, decodedFilename);
  }

  @Test
  void decode_SHOULD_handle_hebrew_WHEN_no_diacritical_marks() throws UnsupportedEncodingException {
    String filename = "עברית";
    String encodedFilename = MimeUtility.encodeText(filename, "iso-8859-8", null);
    String decodedFilename = FilenameDecoder.decode(encodedFilename);
    assertEquals(filename, decodedFilename);
  }

  @Test
  void decode_SHOULD_somewhat_handle_hebrew_WHEN_diacritical_marks() throws UnsupportedEncodingException {
    String filename = "עִברִית";
    String encodedFilename = MimeUtility.encodeText(filename, "iso-8859-8", null);
    String decodedFilename = FilenameDecoder.decode(encodedFilename);
    assertEquals("ע?בר?ית", decodedFilename);
  }

  @Test
  void decode_SHOULD_handle_hebrew_WHEN_alternative_encoding() throws UnsupportedEncodingException {
    String fakePrefix = "=?iso-8859-8-1?";
    String filename = fakePrefix + "עברית";
    String encodedFilename = MimeUtility.encodeText(filename, "iso-8859-8", null);
    System.out.println(encodedFilename);
    encodedFilename = encodedFilename.replaceAll("=\\?iso-8859-8\\?", "=?iso-8859-8-1?");
    System.out.println(encodedFilename);
    String decodedFilename = FilenameDecoder.decode(encodedFilename);
    assertEquals(filename, decodedFilename);
  }
}