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

import com.threewks.thundr.injection.BaseModule;
import com.threewks.thundr.injection.UpdatableInjectionContext;
import com.threewks.thundr.mail.Mailer;
import com.threewks.thundr.module.DependencyRegistry;

/**
 * Module class for thundr-contrib-sendgrid. Add it to the {@link DependencyRegistry} in your ApplicationModule
 * like this:
 * 
 * <pre>
 * <code>
 * @Override
 * 	public void requires(DependencyRegistry dependencyRegistry) {
 * 		dependencyRegistry.addDependency(SendgridModule.class);
 * 	}
 * 	
 * </code>
 * </pre>
 * 
 * This module provides the following features:
 * <ul>
 * <li>Integration with SendGrid using the SendGrid published java client</li>
 * </ul>
 * 
 */
public class SendGridModule extends BaseModule {

	@Override
	public void configure(UpdatableInjectionContext injectionContext) {
		injectionContext.inject(SendGridMailer.class).as(Mailer.class);
		injectionContext.inject(SendGridMailer.class).as(SendGridMailer.class);
	}
}
