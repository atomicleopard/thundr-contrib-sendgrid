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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import com.sendgrid.SendGrid;
import com.sendgrid.SendGrid.Email;
import com.sendgrid.SendGridException;
import com.threewks.thundr.http.ContentType;
import com.threewks.thundr.logger.Logger;
import com.threewks.thundr.mail.Attachment;
import com.threewks.thundr.mail.BaseMailer;
import com.threewks.thundr.mail.MailException;
import com.threewks.thundr.request.InMemoryResponse;
import com.threewks.thundr.request.RequestContainer;
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
		SendGrid.Email email = createEmail();
		email.setSubject(subject);
		email.setFrom(from.getKey());
		email.setFromName(determineName(from));

		addBody(body, email);
		addReplyTo(replyTo, email);
		addTo(to, email);
		addCc(cc, email);
		addBcc(bcc, email);
		addAttachments(attachments, email);
		send(email);
	}

	protected Email createEmail() {
		return new SendGrid.Email();
	}

	protected void send(SendGrid.Email email) {
		try {
			SendGrid.Response response = sendgrid.send(email);
			Logger.info("Sendgrid response: %s %s", response.getCode(), response.getMessage());
			if (!response.getStatus()) {
				throw new MailException("Failed to send email through Sendgrid (%s): %s", response.getCode(), response.getMessage());
			}
		} catch (SendGridException e) {
			throw new MailException("Failed to send email through Sendgrid: %s", e.getMessage());
		}
	}

	protected void addBody(Object body, SendGrid.Email email) {
		if (body == null) {
			throw new MailException("No email body supplied");
		}
		InMemoryResponse renderedResult = render(body);
		String content = renderedResult.getBodyAsString();
		String contentType = ContentType.cleanContentType(renderedResult.getContentTypeString());
		contentType = StringUtils.isBlank(contentType) ? ContentType.TextHtml.value() : contentType;

		if (ContentType.TextPlain.matches(contentType)) {
			email.setText(content);
		} else {
			email.setHtml(content);
		}
	}

	protected void addReplyTo(Entry<String, String> replyTo, SendGrid.Email email) {
		if (replyTo != null) {
			email.setReplyTo(replyTo.getKey());
		}
	}

	protected void addAttachments(List<Attachment> attachments, SendGrid.Email email) {
		for (Attachment attachment : attachments) {
			addAttachment(email, attachment);
		}
	}

	protected void addBcc(Map<String, String> bcc, SendGrid.Email email) {
		for (Map.Entry<String, String> receiver : bcc.entrySet()) {
			email.addBcc(receiver.getKey());
		}
	}

	protected void addCc(Map<String, String> cc, SendGrid.Email email) {
		for (Map.Entry<String, String> receiver : cc.entrySet()) {
			email.addCc(receiver.getKey());
		}
	}

	protected void addTo(Map<String, String> to, SendGrid.Email email) {
		for (Map.Entry<String, String> receiver : to.entrySet()) {
			email.addTo(receiver.getKey(), determineName(receiver));
		}
	}

	protected void addAttachment(SendGrid.Email email, Attachment attachment) {
		try {
			InMemoryResponse renderedAttachment = render(attachment.view());
			if (attachment.isInline()) {
				// SendGrid handles wrapping the name in content id tags - (< and >) for us
				email.addContentId(attachment.name(), attachment.name());
			}
			email.addAttachment(attachment.name(), new ByteArrayInputStream(renderedAttachment.getBodyAsBytes()));
		} catch (IOException e) {
			throw new MailException(e, "Failed to add attachment '%s' to SendGrid email: %s", attachment.name(), e.getMessage());
		}
	}

	protected String determineName(Map.Entry<String, String> receiver) {
		return StringUtils.isBlank(receiver.getValue()) ? receiver.getKey() : receiver.getValue();
	}

}
