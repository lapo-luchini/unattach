package app.unattach.model;

import javax.mail.internet.MimeMessage;

public record EmailProcessorResult(MimeMessage mimeMessage, boolean messageModified) {}
