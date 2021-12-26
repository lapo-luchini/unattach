package app.unattach.model;

import app.unattach.model.attachmentstorage.UserStorage;
import app.unattach.utils.InputStreamDataSource;
import app.unattach.utils.Logger;
import org.apache.commons.io.input.CountingInputStream;
import org.codehaus.plexus.util.FileUtils;
import org.imgscalr.Scalr;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.activation.DataHandler;
import javax.imageio.ImageIO;
import javax.mail.*;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.*;

import static app.unattach.model.GmailLabel.NO_LABEL;

public class EmailProcessor {
  private static final Logger logger = Logger.get();

  private final UserStorage userStorage;
  private final Email email;
  private MimeMessage mimeMessage;
  private final ProcessSettings processSettings;
  private final FilenameFactory filenameFactory;
  private int fileCounter = 0;
  private final List<Part> detectedAttachmentParts;
  private final List<Part> modifiedAttachmentParts;
  private final Map<String, String> originalToNormalizedFilename;
  private final Set<String> resizedImageNames;
  private Part mainTextPart;
  private Part mainHtmlPart;

  private EmailProcessor(UserStorage userStorage, Email email, MimeMessage mimeMessage,
                         ProcessSettings processSettings) {
    this.userStorage = userStorage;
    this.email = email;
    this.mimeMessage = mimeMessage;
    this.processSettings = processSettings;
    Set<String> unattachLabelIds = getUnattachLabelIds(processSettings);
    filenameFactory = new FilenameFactory(processSettings.filenameSchema(), unattachLabelIds);
    detectedAttachmentParts = new LinkedList<>();
    modifiedAttachmentParts = new LinkedList<>();
    originalToNormalizedFilename = new TreeMap<>();
    resizedImageNames = new TreeSet<>();
  }

  private Set<String> getUnattachLabelIds(ProcessSettings processSettings) {
    Set<String> unattachLabelIds = new HashSet<>();
    ProcessOption processOption = processSettings.processOption();
    if (!NO_LABEL.id().equals(processOption.downloadedLabelId())) {
      unattachLabelIds.add(processOption.downloadedLabelId());
    }
    if (!NO_LABEL.id().equals(processOption.removedLabelId())) {
      unattachLabelIds.add(processOption.downloadedLabelId());
    }
    return unattachLabelIds;
  }

  public static EmailProcessorResult process(UserStorage userStorage, Email email, MimeMessage mimeMessage,
                                             ProcessSettings processSettings)
      throws IOException, MessagingException {
    EmailProcessor processor = new EmailProcessor(userStorage, email, mimeMessage, processSettings);
    // As per https://bugs.openjdk.java.net/browse/JDK-8195686, Java doesn't have direct support for iso-8859-8-i
    // encoding; however, iso-8859-8 is equivalent, so we pre-emptively replace it.
    explore(processor.mimeMessage, replaceContentType("iso-8859-8-i", "iso-8859-8", true));
    explore(processor.mimeMessage, processor::detectAndMaybeSaveAttachment);
    if (processSettings.processOption().removeAttachments() || processSettings.processOption().reduceImageResolution()) {
      processor.processDetectedAttachmentParts();
      if (processSettings.addMetadata()) {
        explore(processor.mimeMessage, processor::findTextAndHtml);
        processor.addReferencesToContent();
      }
    }
    processor.mimeMessage.saveChanges();
    return new EmailProcessorResult(processor.mimeMessage, !processor.modifiedAttachmentParts.isEmpty());
  }

  @FunctionalInterface
  public interface CheckedFunction<T> {
    boolean accept(T t) throws MessagingException, IOException;
  }

  public static void explore(Part part, CheckedFunction<Part> function) throws IOException, MessagingException {
    boolean recurse = function.accept(part);
    if (!recurse) {
      return;
    }
    if (part.getContent() instanceof Multipart multipart) {
      for (int i = 0; i < multipart.getCount(); ++i) {
        BodyPart subPart = multipart.getBodyPart(i);
        explore(subPart, function);
      }
    }
  }

  public static CheckedFunction<Part> replaceContentType(String fromEncoding, String toEncoding, boolean replaceAll) {
    return part -> {
      String[] contentTypes = part.getHeader("Content-Type");
      if (contentTypes == null) {
        logger.warn("No Content-Type header found.");
        return true;
      }
      if (contentTypes.length == 1) {
        String contentType = contentTypes[0];
        if (contentType.contains(fromEncoding)) {
          String newContentType = contentType.replace(fromEncoding, toEncoding);
          part.setHeader("Content-Type", newContentType);
          return replaceAll;
        }
      }
      return true;
    };
  }

  /**
   * Detect attachment names. Save attachments to disk if downloading.
   *
   * @return Whether to recursively explore child body parts.
   */
  private boolean detectAndMaybeSaveAttachment(Part part) throws IOException, MessagingException {
    if (!processSettings.processOption().processEmbeddedAttachments() && part.isMimeType("multipart/related")) {
      return false;
    }
    String contentType = part.getContentType();
    String originalFilename = getFilename(part);
    if (originalFilename == null && "message/rfc822".equals(contentType)) {
      originalFilename = "inner_email_" + fileCounter + ".eml";
    }
    if (!isDownloadable(processSettings.processOption(), part, originalFilename)) {
      return true;
    }
    detectedAttachmentParts.add(part);

    String normalizedFilename = filenameFactory.getFilename(email, fileCounter++, originalFilename);
    originalToNormalizedFilename.put(originalFilename, normalizedFilename);
    if (processSettings.processOption().downloadAttachments()) {
      try (InputStream inputStream = part.getInputStream()) {
        userStorage.saveAttachment(inputStream, processSettings.targetDirectory(), normalizedFilename, email.getTimestamp());
        logger.info("Saved attachment %s from email with subject '%s' to file %s.", originalFilename, email.getSubject(),
            normalizedFilename);
      }
    }
    return false;
  }

  /**
   * Based on the documentation of {@link Part#getDisposition()} and https://tools.ietf.org/html/rfc2183.
   */
  private boolean isDownloadable(ProcessOption processOption, Part part, String filename) throws MessagingException {
    String disposition = part.getDisposition();
    return (disposition == null || disposition.equalsIgnoreCase(Part.ATTACHMENT) || processOption.processEmbeddedAttachments())
        && filename != null;
  }

  private String getFilename(Part part) throws MessagingException, UnsupportedEncodingException {
    String rawFilename = part.getFileName();
    if (rawFilename == null) {
      return null;
    }
    try {
      return MimeUtility.decodeText(rawFilename).trim();
    } catch (UnsupportedEncodingException e) {
      if (rawFilename.contains("iso-8859-8-i")) {
        rawFilename = rawFilename.replace("iso-8859-8-i", "iso-8859-8");
        return MimeUtility.decodeText(rawFilename).trim();
      }
      logger.error("Failed to decode the attachment filename: %s", e.getMessage());
      return null;
    }
  }

  private void processDetectedAttachmentParts() throws MessagingException, UnsupportedEncodingException {
    // If an attachment is the whole email body, replace it with an empty multipart alternative.
    if (processSettings.processOption().removeAttachments() && detectedAttachmentParts.size() == 1 &&
        detectedAttachmentParts.get(0) == mimeMessage) {
      modifiedAttachmentParts.add(mimeMessage);
      mimeMessage = shallowCopy(mimeMessage);
      return;
    }
    for (Part part : detectedAttachmentParts) {
      if (part instanceof BodyPart bodyPart) {
        Multipart parent = bodyPart.getParent();
        String filename = getFilename(part);
        if (filename == null) {
          continue;
        }
        String fileExtension = FileUtils.getExtension(filename);
        if (processSettings.processOption().reduceImageResolution() && isSupportedImageExtension(fileExtension)) {
          modifiedAttachmentParts.add(bodyPart);
          parent.removeBodyPart(bodyPart);
          insertImageWithReducedResolution(parent, part, filename, fileExtension);
        } else if (processSettings.processOption().removeAttachments()) {
          modifiedAttachmentParts.add(bodyPart);
          parent.removeBodyPart(bodyPart);
        }
      }
    }
  }

  public static boolean isSupportedImageFilename(String filename) {
    String fileExtension = FileUtils.getExtension(filename);
    return isSupportedImageExtension(fileExtension);
  }

  public static boolean isSupportedImageExtension(String fileExtension) {
    final Set<String> SUPPORTED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "tiff", "tif");
    return SUPPORTED_IMAGE_EXTENSIONS.contains(fileExtension.toLowerCase());
  }

  private void insertImageWithReducedResolution(Multipart parent, Part part, String filename, String fileExtension)
      throws MessagingException {
    try {
      logger.info("Attempting to reduce the resolution of the image named %s...", filename);
      MimeBodyPart newPart = new MimeBodyPart();
      CountingInputStream currentImageInputStream = new CountingInputStream(part.getInputStream());
      BufferedImage currentImage = ImageIO.read(currentImageInputStream);
      final int currentImageSize = currentImageInputStream.getCount();
      logger.info("The size of the current image is %d bytes.", currentImageSize);
      final int maxTargetWidth = 500, maxTargetHeight = 500;
      if (currentImage.getWidth() <= maxTargetWidth || currentImage.getHeight() <= maxTargetHeight) {
        logger.info("The current image is already smaller than the target dimensions. Skipping.");
        return;
      }
      BufferedImage resizedImage = Scalr.resize(currentImage, Scalr.Method.QUALITY, Scalr.Mode.AUTOMATIC,
          maxTargetWidth, maxTargetHeight, Scalr.OP_ANTIALIAS);
      ByteArrayOutputStream resizedImageOutputStream = new ByteArrayOutputStream();
      ImageIO.write(resizedImage, fileExtension, resizedImageOutputStream);
      final int resizedImageSize = resizedImageOutputStream.size();
      if (resizedImageSize >= currentImageSize) {
        logger.info("The resized image is larger than the current image. Skipping.");
        return;
      }
      logger.info("The size of the resized image is %d bytes.", resizedImageSize);
      ByteArrayInputStream resizedImageInputStream = new ByteArrayInputStream(resizedImageOutputStream.toByteArray());
      newPart.setDataHandler(new DataHandler(new InputStreamDataSource(resizedImageInputStream)));
      newPart.setFileName(part.getFileName());
      Enumeration<Header> headers = part.getAllHeaders();
      while (headers.hasMoreElements()) {
        Header header = headers.nextElement();
        newPart.addHeader(header.getName(), header.getValue());
      }
      newPart.setDisposition(part.getDisposition());
      parent.addBodyPart(newPart);
      resizedImageNames.add(filename);
    } catch (IOException e) {
      logger.error("Failed to reduce the resolution of an image.", e);
    }
  }

  private MimeMessage shallowCopy(MimeMessage mimeMessage) throws MessagingException {
    MimeMessage emptyMimeMessage = new MimeMessage(mimeMessage.getSession());
    Enumeration<String> allHeaderLines = mimeMessage.getAllHeaderLines();
    while (allHeaderLines.hasMoreElements()) {
      String headerLine = allHeaderLines.nextElement();
      if (!headerLine.startsWith("Content-Type") &&
          !headerLine.startsWith("Content-Disposition") &&
          !headerLine.startsWith("Content-Transfer-Encoding")) {
        emptyMimeMessage.addHeaderLine(headerLine);
      }
    }
    emptyMimeMessage.setSubject(mimeMessage.getSubject());
    emptyMimeMessage.setSender(mimeMessage.getSender());
    emptyMimeMessage.setRecipients(Message.RecipientType.TO, mimeMessage.getRecipients(Message.RecipientType.TO));
    emptyMimeMessage.setRecipients(Message.RecipientType.CC, mimeMessage.getRecipients(Message.RecipientType.CC));
    emptyMimeMessage.setRecipients(Message.RecipientType.BCC, mimeMessage.getRecipients(Message.RecipientType.BCC));
    emptyMimeMessage.setReplyTo(mimeMessage.getReplyTo());
    emptyMimeMessage.setSentDate(mimeMessage.getSentDate());
    emptyMimeMessage.setContentID(mimeMessage.getContentID());
    emptyMimeMessage.setFlags(mimeMessage.getFlags(), true);
    MimeMultipart multipart = new MimeMultipart("alternative");
    MimeBodyPart textBodyPart = new MimeBodyPart();
    textBodyPart.setContent("", "text/plain; charset=utf-8");
    multipart.addBodyPart(textBodyPart);
    MimeBodyPart htmlBodyPart = new MimeBodyPart();
    htmlBodyPart.setContent("", "text/html; charset=utf-8");
    multipart.addBodyPart(htmlBodyPart);
    emptyMimeMessage.setContent(multipart);
    emptyMimeMessage.saveChanges();
    return emptyMimeMessage;
  }

  /**
   * Find text and HTML body parts.
   *
   * @return Whether to recursively explore child body parts.
   */
  private boolean findTextAndHtml(Part part) throws MessagingException {
    if (part.isMimeType("text/plain") && mainTextPart == null) {
      mainTextPart = part;
    } else if (part.isMimeType("text/html") && mainHtmlPart == null) {
      mainHtmlPart = part;
    }
    return mainTextPart == null || mainHtmlPart == null;
  }

  private void addReferencesToContent() throws IOException, MessagingException {
    if (modifiedAttachmentParts.size() == 0) {
      return;
    }
    if (mainTextPart == null && mainHtmlPart == null) {
      logger.error("Failed to find either text or HTML body part to append info about removed attachments to.");
    }
    String dateTimeString = OffsetDateTime.now().toString();
    String hostname = getHostname();
    if (mainTextPart != null) {
      String text = mainTextPart.getContent().toString();
      String newText = generateTextSuffix(text, dateTimeString, hostname);
      mainTextPart.setContent(newText, "text/plain; charset=utf-8");
    }
    if (mainHtmlPart != null) {
      String html = mainHtmlPart.getContent().toString();
      String newHtml = generateHtmlSuffix(html, dateTimeString, hostname);
      mainHtmlPart.setContent(newHtml, "text/html; charset=utf-8");
    }
  }

  private String getHostname() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      return "(unknown)";
    }
  }

  private String generateTextSuffix(String text, String dateTimeString, String hostname) throws MessagingException {
    StringBuilder newText = new StringBuilder(text);
    newText.append("\n\n\n");
    newText.append("=========================================\n");
    newText.append("Removed/modified attachments:\n");
    for (Part modifiedAttachmentPart : modifiedAttachmentParts) {
      String originalFilename = modifiedAttachmentPart.getFileName();
      String normalizedFilename = originalToNormalizedFilename.get(originalFilename);
      newText.append(" - ");
      newText.append(resizedImageNames.contains(originalFilename) ? "resized" : "removed").append(" ");
      newText.append(originalFilename);
      if (processSettings.processOption().downloadAttachments()) {
        newText.append(" (filename: ").append(normalizedFilename).append(")");
      }
      newText.append("\n");
    }
    newText.append("\nInformation about the change:\n");
    newText.append(" - Made with:                 ").append(Constants.PRODUCT_NAME + " " +
        Constants.VERSION).append("\n");
    newText.append(" - Date and time:             ").append(dateTimeString).append("\n");
    if (processSettings.processOption().downloadAttachments()) {
      newText.append(" - Download target hostname:  ").append(hostname).append("\n");
      newText.append(" - Download target directory: ").append(processSettings.targetDirectory().getAbsolutePath()).append("\n");
    }
    return newText.toString();
  }

  private String generateHtmlSuffix(String html, String dateTimeString, String hostname) throws MessagingException {
    StringBuilder suffix = new StringBuilder("<hr /><p>Removed/modified attachments:<ul>\n");
    String targetDirectoryAbsolutePath = processSettings.targetDirectory().getAbsolutePath();
    for (Part modifiedAttachmentPart : modifiedAttachmentParts) {
      String originalFilename = modifiedAttachmentPart.getFileName();
      String normalizedFilename = originalToNormalizedFilename.get(originalFilename);
      suffix.append("<li>");
      suffix.append(resizedImageNames.contains(originalFilename) ? "resized" : "removed").append(" ");
      suffix.append(originalFilename);
      if (processSettings.processOption().downloadAttachments()) {
        suffix.append(" (");
        Path normalisedPath = Paths.get(targetDirectoryAbsolutePath, normalizedFilename);
        suffix.append("filename: ").append(normalisedPath).append(", ");
        suffix.append("<a href='file:///").append(normalisedPath).append("'>local link</a>");
        suffix.append(")");
      }
      suffix.append("</li>\n");
    }
    suffix.append("</ul></p>\n");
    suffix.append("<p>Information about the change:<ul>\n");
    suffix.append("<li>Made with: ").append("<a href='" + Constants.HOMEPAGE + "'>" + Constants.PRODUCT_NAME + "</a> " +
        Constants.VERSION).append("</li>\n");
    suffix.append("<li>Date and time: ").append(dateTimeString).append("</li>\n");
    if (processSettings.processOption().downloadAttachments()) {
      suffix.append("<li>Download target host name: ").append(hostname).append("</li>\n");
      suffix.append("<li>Download target directory: ").append(targetDirectoryAbsolutePath).append("</li>\n");
      suffix.append("<li><i>File links only work in native email apps (e.g. Mail, Outlook) on the target host.</i></li>\n");
    }
    suffix.append("</ul></p>\n");
    Document document = Jsoup.parse(html);
    document.body().append(suffix.toString());
    return document.toString();
  }
}
