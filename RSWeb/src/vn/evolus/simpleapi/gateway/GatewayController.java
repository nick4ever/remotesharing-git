package vn.evolus.simpleapi.gateway;

import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.mapper.MapperWrapper;

import vn.evolus.simpleapi.gateway.processor.IGatewayResultProcessor;

@Controller
public class GatewayController implements ApplicationContextAware {
    
    
    ApplicationContext context;
    
    private Map<String, Object> registry = new HashMap<String, Object>();
    private List<String> targetNames = null;
    
    public GatewayController(List<String> beanNames, List<String> allowedModelClasses) {
        this.targetNames = beanNames;
        
        for (String allowedModelClass : allowedModelClasses) {
            if (allowedModelClass.endsWith(".*")) {
                XStreamRestrictedObjectConverter.addAllowedDataModelPackage(allowedModelClass.substring(0, allowedModelClass.length() - 2));
            } else {
                XStreamRestrictedObjectConverter.addAllowedDataModelClass(allowedModelClass);
            }
        }
    }
    
    protected void register(String serviceName, Object registeredTarget) {
        this.registry.put(serviceName, registeredTarget);
    }
    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
        this.ensureRegistration();
    }
    public IGatewayResultProcessor getProcessor(Class<? extends IGatewayResultProcessor> clazz) {
        return this.context.getBean(clazz);
    }
    
    public static String toJson(Object obj){
        if (obj == null) return "null";
        
        @SuppressWarnings("unused")
        JsonSerializer<Object> typeInfoEnhancerAdapter = new JsonSerializer<Object>() {

            @Override
            public JsonElement serialize(Object def, Type type, JsonSerializationContext jsc) {
                JsonElement jsonElement = jsc.serialize(def);
                if (jsonElement instanceof JsonObject && jsonElement != null) {
                    JsonObject jsonObject = (JsonObject) jsonElement;
                    jsonObject.addProperty("_class", def.getClass().getName());
                }
                return jsonElement;
            }
        };
        GsonBuilder builder = new GsonBuilder()
        .setDateFormat("yyyy-MM-dd HH:mm:ss")
        .setPrettyPrinting()
//        .registerTypeAdapter(BaseModel.class, typeInfoEnhancerAdapter)
        .enableComplexMapKeySerialization();
        
        Gson gson = builder.create();
        try {
            String json = gson.toJson(obj);
            return json;
        } catch(Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    XStream xstream = createXStream();
    static {
    }
    
    public XStream createXStream() {
        if (xstream != null) return xstream;
        
        xstream = createStaticXStream();
        
        return xstream;
    }
    
    public static XStream createStaticXStream() {
        XStream xstream = new XStream() {
            @Override
            protected MapperWrapper wrapMapper(MapperWrapper next) {
                return new MapperWrapper(next) {
                    @Override
                    public boolean shouldSerializeMember(@SuppressWarnings("rawtypes") Class definedIn, String fieldName) {
                        if (definedIn == Object.class) {
                            return false;
                        }
                        return super.shouldSerializeMember(definedIn, fieldName);
                    }
                };
            }
        };
        xstream.processAnnotations(ServiceRequest.class);
        xstream.processAnnotations(ServiceArg.class);
        xstream.registerConverter(new XStreamDateConverter());
        xstream.registerConverter(new XStreamRestrictedObjectConverter(xstream.getMapper(), xstream.getReflectionProvider()), XStream.PRIORITY_LOW);
        
        return xstream;
    }
    
    private static boolean signatureRegistered = false;
    
    private void ensureRegistration() {
        this.registry.clear();
        for (String beanName : targetNames) {
            this.register(beanName, this.context.getBean(beanName));
        }
        
        if (!signatureRegistered) {
            Set<String> names = this.registry.keySet();
            for (String name : names) {
                Object registeredTarget = this.registry.get(name);
                
                Method[] methods = registeredTarget.getClass().getMethods();
                for (Method m : methods) {
                    Entry entry = m.getAnnotation(Entry.class);
                    if (entry != null) SignatureUtil.registerMethodType(name, m);
                }
            }
            signatureRegistered = true;
        }
    }
    
    
    @ResponseBody
    @RequestMapping(value = "/transport.js", method = RequestMethod.GET)
    public String buildRegistryJS(HttpServletRequest request, HttpServletResponse response) {
        ensureRegistration();
        
        response.setContentType("application/json");
        
        Set<String> names = this.registry.keySet();
        StringBuilder sb = new StringBuilder();
        for (String name : names) {
            Object registeredTarget = this.registry.get(name);
            
            Method[] methods = registeredTarget.getClass().getMethods();
            List<String> allowedMethodNames = new ArrayList<String>();
            for (Method m : methods) {
                Entry entry = m.getAnnotation(Entry.class);
                if (entry != null) allowedMethodNames.add(m.getName());
            }

            String entries = StringUtils.join(allowedMethodNames, "\", \"");
            sb.append("ServiceFactory.registerService(\"" + name + "\", { entries: [\"" + entries + "\"]});\n");
        }
        
        sb.append("\n");
        sb.append(SignatureUtil.getTypeInformationJSON());
        
        //base url
        sb.append("var BASE_FULL_URL = \"" + request.getScheme() + "://");
        sb.append(request.getServerName());
        int port = request.getServerPort();
        boolean secure = "https".equals(request.getScheme());
        if ((secure && port != 443) || (!secure && port != 80)) {
            sb.append(":" + port);
        }
        sb.append(request.getContextPath() + "/\";\n");
        
        sb.append("var API_TRANSPORT_URI = BASE_FULL_URL + \"api/transport\"\n;");
        
        return sb.toString();
    }
    
    @ResponseBody
    @RequestMapping(value = "/unsecured/transport.js", method = RequestMethod.GET)
    public String buildUnsecuredRegistryJS(HttpServletRequest request, HttpServletResponse response) {
        ensureRegistration();
        
        response.setContentType("application/json");
        
        Set<String> names = this.registry.keySet();
        StringBuilder sb = new StringBuilder();
        for (String name : names) {
            Object registeredTarget = this.registry.get(name);
            
            Method[] methods = registeredTarget.getClass().getMethods();
            List<String> allowedMethodNames = new ArrayList<String>();
            for (Method m : methods) {
                Entry entry = m.getAnnotation(Entry.class);
                if (entry != null && !entry.secured()) allowedMethodNames.add(m.getName());
            }
            
            if (allowedMethodNames.isEmpty()) continue;

            String entries = StringUtils.join(allowedMethodNames, "\", \"");
            sb.append("ServiceFactory.registerService(\"" + name + "\", { entries: [\"" + entries + "\"]});\n");
        }
        
        return sb.toString();
    }
    
    @SuppressWarnings("unchecked")
    static <T extends Throwable> T findRootCause(Throwable t, Class<T> clazz) {
        T root = null;
        Throwable current = t;
        while (current != null) {
            if (clazz.isAssignableFrom(current.getClass())) {
                root = (T) current;
            }
            
            current = current.getCause();
        }
        
        return root;
    }
    
    
    @ResponseBody
    @RequestMapping(value = "/transport", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    public String handle(HttpServletRequest request, HttpServletResponse response) {
        return this.handleRequestImpl(request, false);
    }
    
    private String handleRequestImpl(HttpServletRequest request, boolean unsecured) {
        ServiceRequest serviceRequest = null;
        XStream xstream = createXStream();
        try {
            InputStreamReader reader = new InputStreamReader(request.getInputStream(), "UTF-8");
            serviceRequest = (ServiceRequest) xstream.fromXML(reader);
            reader.close();
        } catch (Exception e) {
            System.err.println("Problematic XML: \n");
            throw new RuntimeException(e);
        }
        

        
        ServiceResponse serviceResponse = new ServiceResponse();
        
        Object result;
        try {
            result = evaluate(serviceRequest, unsecured);
            serviceResponse.setResult(result);
            //System.out.println("xml: " + xstream.toXML(serviceResponse));
        } catch (Exception e) {
            e.printStackTrace();
            
            Error error = new Error();
            
            Throwable t = e;
            error.setName(t.getClass().getName());
            error.setMessage(t.getMessage());
            error.setDetails(ExceptionUtils.getFullStackTrace(t));
            
            serviceResponse.setError(error);
        } finally {
        }
        
        try {
            return toJson(serviceResponse);
        } catch (StackOverflowError e) {
            e.printStackTrace();
            xstream.toXML(serviceResponse, System.out);
            return null;
        }
    }
    private Object evaluate(ServiceRequest request, boolean unsecured) throws Exception {
        this.ensureRegistration();
        
        Object requestedTarget = registry.get(request.getServiceName());
        if (requestedTarget == null) throw new Exception("Unknown service: " + request.getServiceName());
        //if (!requestedTarget.allowedEntries.contains(request.getEntryName())) throw new Exception("Entry not allowed: " + request.getEntryName());
        
        Object invocationTarget = requestedTarget;
        Class<?> clazz = invocationTarget.getClass();
        
        Method method = null;
        Method[] methods = clazz.getMethods();
        Entry entry = null;
        for (Method m : methods) {
            Entry e = m.getAnnotation(Entry.class);
            if (m.getName().equals(request.getEntryName()) && e != null && (!unsecured || !e.secured())) {
                method = m;
                entry = e;
                break;
            }
        }
        
        if (method == null) throw new Exception("Unknown entry: " + request.getServiceName() + "." + request.getEntryName());
        Class<?>[] parameterTypes = method.getParameterTypes();
        
        ServiceArg[] arguments = request.getArguments();
        if (arguments.length != parameterTypes.length) {
            throw new Exception("Wrong number of arguments. Expected " + parameterTypes.length + ", got " + arguments.length + ", method: " + request.getServiceName() + "." + request.getEntryName());
        }
        
        Object[] values = new Object[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            ServiceArg arg = arguments[i];
            Object value = arg.getValue();
            values[i] = value;
            
//            System.out.printf("  %d %s -> %s\n", i, value == null ? "NULL" : value.getClass().getName(), parameterTypes[i].getName());
        }

        method.setAccessible(true);
        
        Object object = method.invoke(invocationTarget, values);
        
        Annotation[] annotations = method.getAnnotations();
        List<String> ignoredPaths = new ArrayList<String>();
        List<String> acceptedPaths = new ArrayList<String>();
        for (Annotation annotation : annotations) {
            if (annotation instanceof Ignore) {
                Ignore ignore = (Ignore) annotation;
                for (String path : ignore.path()) {
                    ignoredPaths.add(path);
                }
            }
            if (annotation instanceof Accept) {
                Accept accept = (Accept) annotation;
                for (String path : accept.path()) {
                    acceptedPaths.add(path);
                }
            }
        }

        TransferableUtil.ignoredPaths.set(ignoredPaths);
        TransferableUtil.acceptedPaths.set(acceptedPaths);
        TransferableUtil.currentPath.set("");
        
        try {
            IGatewayResultProcessor processor = this.getProcessor(entry.processor());
            if (processor != null && object != null) object = processor.process(object);
        } finally {
            TransferableUtil.ignoredPaths.set(null);
            TransferableUtil.acceptedPaths.set(null);
        }

        return object;
    }

}
