package app.unattach.utils;

import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeUtility;
import java.io.UnsupportedEncodingException;

public class FilenameDecoder {
  private static final Logger logger = Logger.get();

  public static String getFilename(Part part) throws MessagingException {
    String rawFilename = part.getFileName();
    if (rawFilename == null) {
      return null;
    }
    return decode(rawFilename);
  }

  public static String decode(String encodedFilename) {
    try {
      return decodeAndPostprocess(encodedFilename);
    } catch (UnsupportedEncodingException e) {
      logger.warn("Unable to decode a filename: " + encodedFilename, e);
      if (encodedFilename.startsWith("=?iso-8859-8-1?")) {
        logger.warn("Detected iso-8859-8-1 encoding. Attempting a workaround...");
        String input = encodedFilename.replaceAll("=\\?iso-8859-8-1\\?", "=?iso-8859-8?");
        try {
          return decodeAndPostprocess(input);
        } catch (UnsupportedEncodingException ex) {
          logger.error("Workaround failed: " + encodedFilename, ex);
          return null;
        }
      }
      return null;
    }
  }

  private static String decodeAndPostprocess(String encoded) throws UnsupportedEncodingException {
    // Attempt to parse invalid encodings.
    System.setProperty("mail.mime.decodetext.strict", "false");
    return MimeUtility.decodeText(encoded).trim();
  }
}
