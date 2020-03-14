package vn.evolus.simpleapi.rs;

import javax.servlet.ServletContextEvent;

public class RMServer extends org.springframework.web.context.ContextLoaderListener {
	@Override
	public void contextInitialized(ServletContextEvent event) {
		super.contextInitialized(event);
	}	
}
