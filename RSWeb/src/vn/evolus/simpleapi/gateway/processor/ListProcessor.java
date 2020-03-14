package vn.evolus.simpleapi.gateway.processor;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationContextAware;

public class ListProcessor extends BaseContainerProcessor implements IGatewayResultProcessor, ApplicationContextAware {
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Object process(Object input) {
        if (input == null) return null;
        List<?> list = (List<?>) input;
        ArrayList result = new ArrayList();
        
        for (Object object : list) {
            result.add(this.convertElement(object));
        }
        
        return result;
    }

    @Override
    public boolean supports(Class<?> type) {
        return List.class.isAssignableFrom(type);
    }

}
