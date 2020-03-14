package vn.evolus.simpleapi.sample.bean;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class ServiceController {
	@Autowired
	private SessionFactory sessionFactory;
	private Session session;
}
