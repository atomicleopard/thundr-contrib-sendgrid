thundr-contrib-sendgrid [![Build Status](https://travis-ci.org/atomicleopard/thundr-contrib-sendgrid.svg)](https://travis-ci.org/atomicleopard/thundr-contrib-sendgrid)
=================

A thundr module for sending emails using [SendGrid](http://sendgrid.com/) using the thundr [Mailer abstraction](http://3wks.github.io/thundr/2.0/email/index.html).

You can read more about thundr [here](http://3wks.github.io/thundr/)

Include the thundr-contrib-sendgrid dependency using maven or your favourite dependency management tool.
    
    <dependency>
  		<groupId>com.atomicleopard</groupId>
		<artifactId>thundr-contrib-sendgrid</artifactId>
		<version>2.0.0</version>
		<scope>compile</scope>
	</dependency>
    
Include your sendgrid api key in ``application.properties``

    sendgridApiKey=YOUR_KEY_HERE

Add a dependency on the SendGridModule module in your ``ApplicationModule`` file:

	@Override
	public void requires(DependencyRegistry dependencyRegistry) {
		super.requires(dependencyRegistry);
		...
		dependencyRegistry.addDependency(SendGridModule.class);
	}

The SendGridMailer will then be used as the implementation for Mailer across your application.	
    
--------------    
thundr-contrib-sendgrid - Copyright (C) 2015 Atomic Leopard    