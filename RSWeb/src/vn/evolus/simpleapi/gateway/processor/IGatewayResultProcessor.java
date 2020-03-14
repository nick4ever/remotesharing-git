package vn.evolus.simpleapi.gateway.processor;

public interface IGatewayResultProcessor {
    public Object process(Object input);
    public boolean supports(Class<?> type);
}
