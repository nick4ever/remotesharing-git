package vn.evolus.simpleapi.gateway.processor;

import vn.evolus.simpleapi.gateway.TransferableUtil;

public class ReflectionBasedProcessor implements IGatewayResultProcessor {

    @Override
    public Object process(Object input) {
        return TransferableUtil.createTransferable(input);
    }
    
    @Override
    public boolean supports(Class<?> type) {
        return true;
    }
}
