package vn.evolus.simpleapi.gateway;

public class ServiceResponse {
	private Object result;
	private Error error;
	public Object getResult() {
		return result;
	}
	public void setResult(Object result) {
		this.result = result;
	}
	public Error getError() {
		return error;
	}
	public void setError(Error error) {
		this.error = error;
	}
}
