package vn.evolus.simpleapi.gateway;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.mapper.Mapper;

public class XStreamRestrictedObjectConverter extends ReflectionConverter {
    private static List<String> SUPPORTED_CLASSES = null;
    
    /**
     * This list contains supported class names that can be mentioned in the incoming XStream messages
     * Transfering of model object of classes that are not in this list is forbidden.
     */
    static {
        SUPPORTED_CLASSES = new ArrayList<String>(Arrays.asList(
                        ServiceRequest.class.getName(),
                        ServiceArg.class.getName()
                        ));
    }
    
    public XStreamRestrictedObjectConverter(Mapper mapper, ReflectionProvider reflectionProvider) {
        super(mapper, reflectionProvider);
    }
    
    @SuppressWarnings("rawtypes")
    @Override
    public boolean canConvert(Class type) {
        boolean convertable =
                type != null &&
                !type.isArray() &&
                !type.isPrimitive() &&
                !type.isEnum() &&
                !List.class.isAssignableFrom(type) &&
                !Set.class.isAssignableFrom(type) &&
                !Map.class.isAssignableFrom(type) &&
                !type.getName().startsWith("java.lang.");
        return convertable;
                
    }
    
    @Override
    protected Object instantiateNewInstance(HierarchicalStreamReader reader, UnmarshallingContext context) {
        try {
            String className = reader.getAttribute("class");
            if (className == null) className = reader.getNodeName();
            
            String packageName = className.replaceAll("\\.[^\\.]+$", ".*");
            
            if (!SUPPORTED_CLASSES.contains(className)
                    && !SUPPORTED_CLASSES.contains(packageName)) {
                System.err.println("UNSUPPORTED CLASS: " + className);
                System.err.println("  > To support this class, add it to XStreamRestrictedObjectConverter.SUPPORTED_CLASSES");
                throw new RuntimeException("Invalid attempt to create: " + className);
            }
            
            Class<?> clazz = mapper.realClass(className);
            Object obj = clazz == null ? null : clazz.newInstance();
            if	(obj == null) {
            	//System.out.println("Could not create new instance of " + className);
            }
            return obj;
        } catch (Exception e) {
            throw new ConversionException("Could not create instance of class " + reader.getNodeName(), e);
        }
    }
    public static void addAllowedDataModelClass(Class<?> clazz) {
        SUPPORTED_CLASSES.add(clazz.getName());
    }
    public static void addAllowedDataModelClass(String className) {
        SUPPORTED_CLASSES.add(className);
    }
    public static void addAllowedDataModelPackage(String packageName) {
        SUPPORTED_CLASSES.add(packageName + ".*");
    }

}
