package vn.evolus.simpleapi.gateway;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

@XStreamAlias(value="Request")
public class ServiceRequest {
	@XStreamAsAttribute
	@XStreamAlias(value="Service")
	private String serviceName;
	
	@XStreamAsAttribute
	@XStreamAlias(value="Entry")
	private String entryName;
	
	@XStreamAlias(value="Arguments")
	private ServiceArg[] arguments;
	
	public String getServiceName() {
		return serviceName;
	}
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	public String getEntryName() {
		return entryName;
	}
	public void setEntryName(String entryName) {
		this.entryName = entryName;
	}
	public ServiceArg[] getArguments() {
		return arguments;
	}
	public void setArguments(ServiceArg[] arguments) {
		this.arguments = arguments;
	}
}
