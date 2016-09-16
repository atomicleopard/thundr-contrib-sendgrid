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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.sendgrid.Attachments;
import com.sendgrid.Content;
import com.sendgrid.Email;
import com.sendgrid.Mail;
import com.threewks.thundr.http.ContentType;
import com.threewks.thundr.mail.MailException;
import com.threewks.thundr.request.RequestContainer;
import com.threewks.thundr.request.ThreadLocalRequestContainer;
import com.threewks.thundr.util.Encoder;
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
    private Mail sent;
    private RequestContainer requestContainer = new ThreadLocalRequestContainer();
    private SendGridMailer mailer = new SendGridMailer(viewResolverRegistry, requestContainer, "apiKey") {
        @Override
        protected void send(Mail email) {
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
        assertThat(sent.from.getEmail(), is("me@mail.com"));
        assertThat(sent.from.getName(), is("me@mail.com"));
        assertThat(getTo(0).getEmail(), is("someone@mail.com"));
        assertThat(getTo(0).getName(), is("someone@mail.com"));
        assertThat(getContent().getValue(), is("Body"));
        assertThat(getContent().getType(), is("text/plain"));

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
        assertThat(sent.from.getEmail(), is("me@mail.com"));
        assertThat(sent.from.getName(), is("Me"));
        assertThat(getTo(0).getEmail(), is("someone@mail.com"));
        assertThat(getTo(0).getName(), is("Recipient"));
        assertThat(getContent().getValue(), is("Body"));
        assertThat(getContent().getType(), is("text/plain"));
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
        assertThat(sent.from.getEmail(), is("me@mail.com"));
        assertThat(sent.from.getName(), is("Me"));
        assertThat(getTo(0).getEmail(), is("someone@mail.com"));
        assertThat(getTo(0).getName(), is("Recipient"));
        assertThat(getContent().getValue(), is("<html><body><h1>Content</h1></body></html>"));
        assertThat(getContent().getType(), is("text/html"));
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
        assertThat(sent.from.getEmail(), is("me@mail.com"));
        assertThat(sent.from.getName(), is("Me"));
        assertThat(getTo(0).getEmail(), is("someone@mail.com"));
        assertThat(getTo(1).getEmail(), is("someone-else@mail.com"));
        assertThat(getTo(0).getName(), is("Recipient"));
        assertThat(getTo(1).getName(), is("someone-else@mail.com"));
        assertThat(getContent().getValue(), is("<html><body><h1>Content</h1></body></html>"));
        assertThat(getContent().getType(), is("text/html"));
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
        assertThat(sent.from.getEmail(), is("me@mail.com"));
        assertThat(sent.from.getName(), is("Me"));
        assertThat(sent.replyTo.getEmail(), is("no-reply@mail.com"));
        assertThat(sent.replyTo.getName(), is("People reply anyway, they can't help themselves"));
        assertThat(getTo(0).getEmail(), is("someone@mail.com"));
        assertThat(getTo(1).getEmail(), is("someone-else@mail.com"));
        assertThat(getTo(0).getName(), is("Recipient"));
        assertThat(getTo(1).getName(), is("someone-else@mail.com"));
        assertThat(getCc(0).getEmail(), is("cc1@domain.com"));
        assertThat(getCc(1).getEmail(), is("cc2@domain.com"));
        assertThat(getCc(0).getName(), is("cc1@domain.com"));
        assertThat(getCc(1).getName(), is("CC2"));
        assertThat(getBcc(0).getEmail(), is("bcc1@domain.com"));
        assertThat(getBcc(1).getEmail(), is("bcc2@domain.com"));
        assertThat(getBcc(0).getName(), is("bcc1@domain.com"));
        assertThat(getBcc(1).getName(), is("BCC2"));
    }

    @Test
    public void shouldIncludeAttachements() {
        // @formatter:off
		mailer.mail()
			.subject("Subject")
			.from("me@mail.com", "Me")
			.to("someone.com")
			.body(new StringView("<html><body><h1>Content</h1></body></html>").withContentType(ContentType.TextHtml))
			.attach("Text", new FileView("file.txt", new byte[]{0,1,2}, "text/plain"), Disposition.Attachment)
			.attach("Image.png", new FileView("image.png", new byte[]{3,2,1}, "image/png"), Disposition.Inline)
			.send();
		// @formatter:on

		assertThat(sent.attachments.get(0).getFilename(), is("Text"));
		assertThat(sent.attachments.get(0).getContent(), is(base64(new byte[]{0,1,2})));
		assertThat(sent.attachments.get(0).getType(), is("text/plain"));
		assertThat(sent.attachments.get(0).getContentId(), is("Text"));
		assertThat(sent.attachments.get(0).getDisposition(), is("attachment"));
		assertThat(sent.attachments.get(1).getFilename(), is("Image.png"));
		assertThat(sent.attachments.get(1).getContent(), is(base64(new byte[]{3,2,1})));
		assertThat(sent.attachments.get(1).getType(), is("image/png"));
		assertThat(sent.attachments.get(1).getContentId(), is("Image.png"));
		assertThat(sent.attachments.get(1).getDisposition(), is("inline"));
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

        Mail mail = spy(new Mail());
        mailer = spy(mailer);
        when(mailer.createMail()).thenReturn(mail);
        doThrow(new RuntimeException("Expected")).when(mail).addAttachments(Mockito.any(Attachments.class));

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

    private String base64(byte[] value) {
        return new Encoder(value).base64()
                .string();
    }

    private Email getTo(int index) {
        return sent.getPersonalization()
                   .get(0)
                   .getTos()
                   .get(index);
    }

    private Email getCc(int index) {
        return sent.getPersonalization()
                   .get(0)
                   .getCcs()
                   .get(index);
    }

    private Email getBcc(int index) {
        return sent.getPersonalization()
                   .get(0)
                   .getBccs()
                   .get(index);
    }

    private Content getContent() {
        return sent.getContent()
                   .get(0);
    }
}
