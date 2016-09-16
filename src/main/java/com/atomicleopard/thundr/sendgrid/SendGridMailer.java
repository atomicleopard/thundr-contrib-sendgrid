/*
 * This file is a part of thundr-contrib-sendgrid, a software library from Atomic Leopard.
 *
 * Copyright (C) 2015 Atomic Leopard, <nick@atomicleopard.com.au>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.atomicleopard.thundr.sendgrid;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import com.sendgrid.Attachments;
import com.sendgrid.Content;
import com.sendgrid.Email;
import com.sendgrid.Mail;
import com.sendgrid.Method;
import com.sendgrid.Personalization;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.threewks.thundr.http.ContentType;
import com.threewks.thundr.http.StatusCode;
import com.threewks.thundr.logger.Logger;
import com.threewks.thundr.mail.Attachment;
import com.threewks.thundr.mail.BaseMailer;
import com.threewks.thundr.mail.MailException;
import com.threewks.thundr.request.InMemoryResponse;
import com.threewks.thundr.request.RequestContainer;
import com.threewks.thundr.util.Encoder;
import com.threewks.thundr.view.ViewResolverRegistry;

public class SendGridMailer extends BaseMailer {

    protected SendGrid sendgrid;

    public SendGridMailer(ViewResolverRegistry viewResolverRegistry, RequestContainer requestContainer, String sendgridApiKey) {
        super(viewResolverRegistry, requestContainer);
        this.sendgrid = new SendGrid(sendgridApiKey);
    }

    public SendGrid getSendgrid() {
        return sendgrid;
    }

    @Override
    protected void sendInternal(Entry<String, String> from, Entry<String, String> replyTo, Map<String, String> to, Map<String, String> cc, Map<String, String> bcc, String subject, Object body,
            List<Attachment> attachments) {
        Mail mail = createMail();
        mail.setSubject(subject);
        mail.setFrom(email(from));

        Personalization personalization = new Personalization();
        mail.addPersonalization(personalization);

        addReplyTo(replyTo, mail);
        addBody(body, mail);
        addTo(to, personalization);
        addCc(cc, personalization);
        addBcc(bcc, personalization);
        addAttachments(attachments, mail);
        send(mail);
    }

    protected void send(Mail email) {
        try {
            Request request = new Request();
            request.method = Method.POST;
            request.endpoint = "mail/send";
            request.body = email.build();
            Response response = sendgrid.api(request);
            Logger.info("Sendgrid response: %s %s", response.statusCode, response.body);
            if (!StatusCode.OK.isInFamily(response.statusCode)) {
                throw new MailException("Failed to send email through Sendgrid (%s): %s", response.statusCode, response.body);
            }
        } catch (IOException e) {
            throw new MailException("Failed to send email through Sendgrid: %s", e.getMessage());
        }
    }

    protected void addBody(Object body, Mail email) {
        if (body == null) {
            throw new MailException("No email body supplied");
        }
        InMemoryResponse renderedResult = render(body);
        String content = renderedResult.getBodyAsString();
        String contentType = ContentType.cleanContentType(renderedResult.getContentTypeString());
        contentType = StringUtils.isBlank(contentType) ? ContentType.TextHtml.value() : contentType;

        email.addContent(new Content(contentType, content));
    }

    protected void addReplyTo(Entry<String, String> replyTo, Mail email) {
        if (replyTo != null) {
            email.setReplyTo(new Email(replyTo.getKey(), determineName(replyTo)));
        }
    }

    protected void addAttachments(List<Attachment> attachments, Mail email) {
        for (Attachment attachment : attachments) {
            addAttachment(email, attachment);
        }
    }

    protected void addBcc(Map<String, String> bcc, Personalization personalization) {
        for (Map.Entry<String, String> receiver : bcc.entrySet()) {
            personalization.addBcc(email(receiver));
        }
    }

    protected void addCc(Map<String, String> cc, Personalization personalization) {
        for (Map.Entry<String, String> receiver : cc.entrySet()) {
            personalization.addCc(email(receiver));
        }
    }

    protected void addTo(Map<String, String> to, Personalization personalization) {
        for (Map.Entry<String, String> receiver : to.entrySet()) {
            personalization.addTo(email(receiver));
        }
    }

    protected void addAttachment(Mail email, Attachment attachment) {
        try {
            InMemoryResponse renderedAttachment = render(attachment.view());
            Attachments attachments = new Attachments();
            // SendGrid handles wrapping the name in content id tags - (< and >) for us
            attachments.setContentId(attachment.name());
            attachments.setDisposition(attachment.disposition()
                                                 .value());
            attachments.setFilename(attachment.name());
            attachments.setContent(new Encoder(renderedAttachment.getBodyAsBytes()).base64()
                                                                                   .string());
            attachments.setType(renderedAttachment.getContentTypeString());
            email.addAttachments(attachments);
        } catch (Exception e) {
            throw new MailException(e, "Failed to add attachment '%s' to SendGrid email: %s", attachment.name(), e.getMessage());
        }
    }

    protected String determineName(Map.Entry<String, String> receiver) {
        return StringUtils.isBlank(receiver.getValue()) ? receiver.getKey() : receiver.getValue();
    }

    protected Email email(Entry<String, String> receiver) {
        return new Email(receiver.getKey(), determineName(receiver));
    }

    protected Mail createMail() {
        return new Mail();
    }
}
