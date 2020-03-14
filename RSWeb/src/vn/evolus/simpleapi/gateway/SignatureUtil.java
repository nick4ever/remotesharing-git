package vn.evolus.simpleapi.gateway;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SignatureUtil {
    private static Map<String, Class<?>> extraGenericTypeRegistry = new HashMap<String, Class<?>>();
    private static List<Class<?>> stopTypes = new ArrayList<Class<?>>();
    
    static {
        stopTypes.add(java.util.Date.class);
    }
    
    private static Map<String, Map<String, String>> propertyMap = new HashMap<String, Map<String, String>>();
    private static Map<String, List<String>> methodParamTypeMap = new HashMap<String, List<String>>();
    
    private static List<String> currentClasses = new ArrayList<String>();
    private static void register(Class<?> clazz) {
        if (clazz.getName().startsWith("java.lang")
                || propertyMap.containsKey(clazz)
                || clazz.isEnum()
                || clazz.isPrimitive()
                || stopTypes.contains(clazz)
                || currentClasses.contains(clazz.getName())) return;
        
        currentClasses.add(clazz.getName());
        Map<String, String> map = new HashMap<String, String>();
        putFieldInfo(clazz, map);
        propertyMap.put(clazz.getName(), map);
        currentClasses.remove(clazz.getName());
    }
    
    public static void registerExtraGenericType(Class<?> clazz, String field, Class<?> genericType) {
        extraGenericTypeRegistry.put(clazz.getName() + "@" + field, genericType);
    }
    
    public static Class<?> getGenericType(Field field) {
        Type genericType = field.getGenericType();
        if (genericType != null) {
            if (genericType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) genericType;
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                Type type = actualTypeArguments[0];
                if (type instanceof Class<?>) return (Class<?>) type;
            }
        }
        
        Typed typed = field.getAnnotation(Typed.class);
        if (typed != null) {
            return typed.value();
        }
        
        String entry = field.getDeclaringClass().getName() + "@" + field.getName();
        if (extraGenericTypeRegistry.containsKey(entry)) {
            return extraGenericTypeRegistry.get(entry);
        }
        
        return null;
    }
    
    private static void putFieldInfo(Class<?> clazz, Map<String, String> map) {
        if (clazz.isEnum()) return;
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            
            String typeName = field.getType().getName();
            if (Collection.class.isAssignableFrom(field.getType())) {
                Class<?> type = getGenericType(field);
                if (type != null) {
                    typeName += "#" + type.getName();
                    register(type);
                }
            } else {
                register(field.getType());
            }
            map.put(field.getName(), typeName);
        }
        
        Class<?> parent = clazz.getSuperclass();
        if (parent != null) putFieldInfo(parent, map);
    }
    
    public static void registerMethodType(String serviceName, Method method) {
        String name = serviceName + "." + method.getName();
        List<String> types = new ArrayList<String>();
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> type = parameterTypes[i];
            Type genericType = method.getGenericParameterTypes()[i];
            String typeName = type.getName();
            if (genericType != null) {
                if (genericType instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) genericType;
                    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                    Class<?> genericClass = (Class<?>) actualTypeArguments[0];
                    typeName += "#" + genericClass.getName();
                    register(genericClass);
                }
            }
            
            if (type.isEnum()) {
                typeName = "enum:" + typeName;
            }
            
            types.add(typeName);
            register(type);
        }
        
        methodParamTypeMap.put(name, types);
    }
    
    public static String getTypeInformationJSON() {
        StringBuilder sb = new StringBuilder();
        sb.append("var METHOD_ENTRY_TYPE_MAP = ");
        sb.append(GatewayController.toJson(methodParamTypeMap));
        sb.append(";\n");

        sb.append("var FIELD_TYPE_MAP = ");
        sb.append(GatewayController.toJson(propertyMap));
        sb.append(";\n");
        
        return sb.toString();
    }
}
