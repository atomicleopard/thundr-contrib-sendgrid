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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.sendgrid.SendGrid;
import com.sendgrid.SendGrid.Email;
import com.threewks.thundr.http.ContentType;
import com.threewks.thundr.mail.MailException;
import com.threewks.thundr.view.ViewResolverRegistry;
import com.threewks.thundr.view.file.Disposition;
import com.threewks.thundr.view.file.FileView;
import com.threewks.thundr.view.file.FileViewResolver;
import com.threewks.thundr.view.string.StringView;
import com.threewks.thundr.view.string.StringViewResolver;

public class SendGridMailerTest {
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	private ViewResolverRegistry viewResolverRegistry = new ViewResolverRegistry();
	private SendGrid.Email sent;
	private SendGridMailer mailer = new SendGridMailer(viewResolverRegistry, "apiKey") {
		@Override
		protected void send(com.sendgrid.SendGrid.Email email) {
			sent = email;
		};
	};

	@Before
	public void before() {
		viewResolverRegistry.addResolver(StringView.class, new StringViewResolver());
		viewResolverRegistry.addResolver(FileView.class, new FileViewResolver());
	}

	@Test
	public void shouldProvideDirectAccessToSendGridClass() {
		assertThat(mailer.getSendgrid(), is(notNullValue()));
	}

	@Test
	public void shouldSendBasicTextEmail() {
		// @formatter:off
		mailer.mail()
			.subject("Subject")
			.from("me@mail.com")
			.to("someone@mail.com")
			.body(new StringView("Body"))
			.send();
		// @formatter:on
		assertThat(sent.getSubject(), is("Subject"));
		assertThat(sent.getFrom(), is("me@mail.com"));
		assertThat(sent.getFromName(), is("me@mail.com"));
		assertThat(sent.getTos(), hasItemInArray("someone@mail.com"));
		assertThat(sent.getToNames(), hasItemInArray("someone@mail.com"));
		assertThat(sent.getText(), is("Body"));

	}

	@Test
	public void shouldSendBasicTextEmailWithNames() {
		// @formatter:off
		mailer.mail()
			.subject("Subject")
			.from("me@mail.com", "Me")
			.to("someone@mail.com", "Recipient")
			.body(new StringView("Body"))
			.send();
		// @formatter:on
		assertThat(sent.getSubject(), is("Subject"));
		assertThat(sent.getFrom(), is("me@mail.com"));
		assertThat(sent.getFromName(), is("Me"));
		assertThat(sent.getTos(), hasItemInArray("someone@mail.com"));
		assertThat(sent.getToNames(), hasItemInArray("Recipient"));
		assertThat(sent.getText(), is("Body"));
		assertThat(sent.getHtml(), is(nullValue()));
	}

	@Test
	public void shouldSendBasicHtmlEmail() {
		// @formatter:off
		mailer.mail()
			.subject("Subject")
			.from("me@mail.com", "Me")
			.to("someone@mail.com", "Recipient")
			.body(new StringView("<html><body><h1>Content</h1></body></html>").withContentType(ContentType.TextHtml))
			.send();
		// @formatter:on
		assertThat(sent.getSubject(), is("Subject"));
		assertThat(sent.getFrom(), is("me@mail.com"));
		assertThat(sent.getFromName(), is("Me"));
		assertThat(sent.getTos(), hasItemInArray("someone@mail.com"));
		assertThat(sent.getToNames(), hasItemInArray("Recipient"));
		assertThat(sent.getText(), is(nullValue()));
		assertThat(sent.getHtml(), is("<html><body><h1>Content</h1></body></html>"));
	}

	@Test
	public void shouldRespectSendAndRecieverNames() {
		// @formatter:off
		mailer.mail()
			.subject("Subject")
			.from("me@mail.com", "Me")
			.to("someone@mail.com", "Recipient")
			.to("someone-else@mail.com")
			.body(new StringView("<html><body><h1>Content</h1></body></html>").withContentType(ContentType.TextHtml))
			.send();
		// @formatter:on
		assertThat(sent.getSubject(), is("Subject"));
		assertThat(sent.getFrom(), is("me@mail.com"));
		assertThat(sent.getFromName(), is("Me"));
		assertThat(sent.getTos()[0], is("someone@mail.com"));
		assertThat(sent.getTos()[1], is("someone-else@mail.com"));
		assertThat(sent.getToNames()[0], is("Recipient"));
		assertThat(sent.getToNames()[1], is("someone-else@mail.com"));
		assertThat(sent.getText(), is(nullValue()));
		assertThat(sent.getHtml(), is("<html><body><h1>Content</h1></body></html>"));
	}

	@Test
	public void shouldToFromReplyToCcAndBcc() {
		// @formatter:off
		mailer.mail()
			.subject("Subject")
			.from("me@mail.com", "Me")
			.replyTo("no-reply@mail.com", "People reply anyway, they can't help themselves")
			.to("someone@mail.com", "Recipient")
			.to("someone-else@mail.com")
			.cc("cc1@domain.com")
			.cc("cc2@domain.com", "CC2")
			.bcc("bcc1@domain.com")
			.bcc("bcc2@domain.com", "BCC2")
			.body(new StringView("<html><body><h1>Content</h1></body></html>").withContentType(ContentType.TextHtml))
			.send();
		// @formatter:on
		assertThat(sent.getSubject(), is("Subject"));
		assertThat(sent.getFrom(), is("me@mail.com"));
		assertThat(sent.getFromName(), is("Me"));
		assertThat(sent.getReplyTo(), is("no-reply@mail.com"));
		assertThat(sent.getTos()[0], is("someone@mail.com"));
		assertThat(sent.getTos()[1], is("someone-else@mail.com"));
		assertThat(sent.getToNames()[0], is("Recipient"));
		assertThat(sent.getToNames()[1], is("someone-else@mail.com"));
		assertThat(sent.getCcs()[0], is("cc1@domain.com"));
		assertThat(sent.getCcs()[1], is("cc2@domain.com"));
		assertThat(sent.getBccs()[0], is("bcc1@domain.com"));
		assertThat(sent.getBccs()[1], is("bcc2@domain.com"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void shouldIncludeAttachements() {
		// @formatter:off
		mailer.mail()
			.subject("Subject")
			.from("me@mail.com", "Me")
			.to("someone.com")
			.body(new StringView("<html><body><h1>Content</h1></body></html>").withContentType(ContentType.TextHtml))
			.attach("Text", new FileView("file.txt", new byte[]{0,1,2}, "text/plain"), Disposition.Attachment)
			.attach("Image.png", new FileView("image.png", new byte[]{0,1,2}, "image/png"), Disposition.Inline)
			.send();
		// @formatter:on

		Map<String, InputStream> attachments = sent.getAttachments();
		assertThat(attachments, hasKey("Text"));
		assertThat(attachments, hasKey("Image.png"));

		Map<String, String> contentIds = sent.getContentIds();
		assertThat(contentIds, hasEntry("Image.png", "Image.png"));
	}

	@Test
	public void shouldFailIfNoBodySpecified() {
		thrown.expect(MailException.class);
		thrown.expectMessage("No email body supplied");
		// @formatter:off
		mailer.mail()
			.subject("Subject")
			.from("me@mail.com", "Me")
			.to("someone.com")
			.send();
		// @formatter:on
	}

	@Test
	public void shouldThrowMailExceptionIfAttachmentFails() throws IOException {
		thrown.expect(MailException.class);
		thrown.expectMessage("Failed to add attachment 'Text' to SendGrid email: Expected");
		
		Email email = spy(new SendGrid.Email());
		mailer = spy(mailer);
		when(mailer.createEmail()).thenReturn(email);
		when(email.addAttachment(anyString(), Mockito.any(InputStream.class))).thenThrow(new IOException("Expected"));

		// @formatter:off
		mailer.mail()
			.subject("Subject")
			.from("me@mail.com", "Me")
			.to("someone.com")
			.body(new StringView("<html><body><h1>Content</h1></body></html>").withContentType(ContentType.TextHtml))
			.attach("Text", new FileView("file.txt", new byte[]{0,1,2}, "text/plain"), Disposition.Attachment)
			.send();
		// @formatter:on
		
	}
}
