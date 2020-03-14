package vn.evolus.simpleapi.gateway.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public abstract class BaseContainerProcessor implements IGatewayResultProcessor, ApplicationContextAware {
    ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
    }
    
    protected Object convertElement(Object element) {
        if (element == null) {
        	//System.out.println("Obj is null skip convertElement, my class is: " + this.getClass().getName());
        	return null;
        }
        Map<String, IGatewayResultProcessor> map = this.context.getBeansOfType(IGatewayResultProcessor.class);
        List<IGatewayResultProcessor> supporteds = new ArrayList<IGatewayResultProcessor>();
        for (IGatewayResultProcessor processor : map.values()) {
            boolean supports = processor.supports(element.getClass());
            //System.out.println("Checking  "+ processor.getClass().getName() + ", support: " + supports);
			if (supports) {
				//System.out.println("Added " + processor);
				supporteds.add(processor);
            }
        }
        if (supporteds.size() > 0) {
	        IGatewayResultProcessor defaultProcessor = supporteds.get(0);
	        //System.out.println("Process with " + defaultProcessor.getClass().getSimpleName());
	        
	   	 	Object processedObject = defaultProcessor.process(element);
	        return processedObject;
        }
        throw new RuntimeException("No processor found for element type: " + element.getClass().getName());
    }

}
