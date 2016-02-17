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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.threewks.thundr.http.ContentType;
import com.threewks.thundr.request.RequestContainer;
import com.threewks.thundr.request.ThreadLocalRequestContainer;
import com.threewks.thundr.util.Streams;
import com.threewks.thundr.view.ViewResolverRegistry;
import com.threewks.thundr.view.file.Disposition;
import com.threewks.thundr.view.file.FileView;
import com.threewks.thundr.view.file.FileViewResolver;
import com.threewks.thundr.view.string.StringView;
import com.threewks.thundr.view.string.StringViewResolver;

@Ignore
public class SendGridMailerIT {
	private SendGridMailer mailer;
	private ViewResolverRegistry viewResolverRegistry;
	private RequestContainer requestContainer = new ThreadLocalRequestContainer();
	private String apiKey = null;
	private String sender = null;

	@Before
	public void before() {
		assertThat("Please provide a sendgrid apiKey", apiKey, is(notNullValue()));
		assertThat("Please provide a sender", sender, is(notNullValue()));
		viewResolverRegistry = new ViewResolverRegistry();
		viewResolverRegistry.addResolver(StringView.class, new StringViewResolver());
		viewResolverRegistry.addResolver(FileView.class, new FileViewResolver());

		mailer = new SendGridMailer(viewResolverRegistry, requestContainer, apiKey);
	}

	@Test
	public void shouldSendAnEmail() {
		// @formatter:off
		mailer.mail()
			.from(sender)
			.to("naokunew@gmail.com")
			.cc("naokunew+1@gmail.com")
			.bcc("naokunew+2@gmail.com")
			.replyTo(sender)
			.subject("Test thundr-contrib-sendgrid")
			.body(new StringView("Text email"))
			.send();
		// @formatter:on
	}

	@Test
	public void shouldSendAnEmailWithAttachments() {
		// @formatter:off
		mailer.mail()
			.from(sender)
			.to("naokunew@gmail.com")
			.subject("Test thundr-contrib-sendgrid attachments")
			.body(new StringView("<html><body><h1>Yeah!</h1><img src=\"cid:inline-1.png\"/></body></html>").withContentType(ContentType.TextHtml))
			.attach("inline-1.png", new FileView("inline-1.png", Streams.getResourceAsStream("com/atomicleopard/thundr/sendgrid/logoFullSmall.png"), ContentType.ImagePng.value()), Disposition.Inline)
			.attach("attached.txt", new StringView("Attached text"), Disposition.Attachment)
			.send();
		// @formatter:on		
	}
}
