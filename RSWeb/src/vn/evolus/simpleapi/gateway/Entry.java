package vn.evolus.simpleapi.gateway;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import vn.evolus.simpleapi.gateway.processor.IGatewayResultProcessor;
import vn.evolus.simpleapi.gateway.processor.ReflectionBasedProcessor;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Entry {
    Class<? extends IGatewayResultProcessor> processor() default ReflectionBasedProcessor.class;
    boolean secured() default true;
}
