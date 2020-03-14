package vn.evolus.simpleapi.gateway;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

@XStreamAlias(value="Arg")
public class ServiceArg {
	
	@XStreamAsAttribute
	private String name;
	private Object value;
	
	public ServiceArg() {
	}
	public ServiceArg(String name, Object value) {
		this.name = name;
		this.value = value;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Object getValue() {
		return value;
	}
	public void setValue(Object value) {
		this.value = value;
	}
}
