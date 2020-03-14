package vn.evolus.simpleapi.gateway;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.collection.AbstractPersistentCollection;
import org.hibernate.proxy.HibernateProxy;

public class TransferableUtil {
    public static ThreadLocal<String> currentPath = new ThreadLocal<String>();
    public static ThreadLocal<List<String>> ignoredPaths = new ThreadLocal<List<String>>();
    public static ThreadLocal<List<String>> acceptedPaths = new ThreadLocal<List<String>>();

    public static ThreadLocal<Boolean> debug = new ThreadLocal<Boolean>();

    public static boolean isDebug() {
//        return true;
        Boolean d = debug.get();
        return d != null && d.booleanValue();
    }

    public static void log(Object o) {
        if (isDebug()) System.out.println("" + o);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Object createTransferable(Object source) {
        if (source == null) return null;
        
        try {
            
            Object dest = source;
            
            if (dest instanceof HibernateProxy) {
                log(" > target is a hibernate proxy, getting implementation from " + dest.getClass().getName());
                dest = ((HibernateProxy) dest).getHibernateLazyInitializer().getImplementation();
                log("   > after converting: " + dest.getClass().getName());
            }
            
            //handling lazy collection
            if (dest instanceof Collection<?>) {
                log(" > coverting collection: " + dest.getClass().getName());
                Collection collection = (dest instanceof Set<?>) ? new LinkedHashSet() : new ArrayList();
                Collection original = (Collection) dest;
                
                String path = currentPath.get();
                try {
                    String newPath = path == null ? "" : path;
                    if (!newPath.endsWith("/")) newPath += "/";
                    newPath += "@item";
                    currentPath.set(newPath);

                    for (Object item : original) {  //this loop will trigger lazyinitialier
                        log(" > item class: " + item.getClass().getName());
                        collection.add(createTransferable(item));
                    }
                } finally {
                    currentPath.set(path);
                }
                
                return collection;
            }
            
            Class<?> clazz = dest.getClass();
            
            if (!clazz.getName().startsWith("com.aeroscout.mobileview.model.")
                    || clazz.isEnum()
                    || clazz.isPrimitive()) return dest;
                    
            Object dolly = null;
            
            try {
                dolly = clazz.newInstance();
            } catch (Throwable e) {
                return dest;
            }

            BeanInfo beanInfo = Introspector.getBeanInfo(clazz);
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                String path = currentPath.get();
                try {
                    String newPath = path == null ? "" : path;
                    if (!newPath.endsWith("/")) newPath += "/";
                    newPath += propertyDescriptor.getName();
                    currentPath.set(newPath);
                    
                    log(" > handling attribute at: " + newPath);

                    List<String> ignoredPathList = ignoredPaths.get();
                    boolean ignoredByMethod = false;
                    if (ignoredPathList != null) {
                        for (String ignoredPathSpec : ignoredPathList) {
                            if (ignoredPathSpec.equals(newPath)) {
                                ignoredByMethod = true;
                                break;
                            }
                            
                            if (ignoredPathSpec.endsWith("/*")
                                    && newPath.startsWith(ignoredPathSpec.substring(0, ignoredPathSpec.length() - 1))) {
                                ignoredByMethod = true;
                                break;
                            }
                        }
                    }
                    
                    if (ignoredByMethod) {
                        List<String> acceptedPathList = acceptedPaths.get();
                        if (acceptedPathList != null) {
                            for (String acceptedPathSpec : acceptedPathList) {
                                if (acceptedPathSpec.equals(newPath)) {
                                    ignoredByMethod = false;
                                    break;
                                }
                                
                                if (acceptedPathSpec.endsWith("/*")
                                        && newPath.startsWith(acceptedPathSpec.substring(0, acceptedPathSpec.length() - 1))) {
                                    ignoredByMethod = false;
                                    break;
                                }
                            }
                        }
                    }
                    
                    Method readMethod = propertyDescriptor.getReadMethod();
                    Method writeMethod = propertyDescriptor.getWriteMethod();
                    if (readMethod == null || writeMethod == null) {
                        log("    > bad property");
                        continue;
                    }
                    
                    if (ignoredByMethod) {
                        log("    > ignored.");
                        continue;
                    }
                    
                    Object value = readMethod.invoke(source);
                    if (value == null) {
                        log("    > NULL");
                        continue;
                    }
                    
                    log("    > class: " + value.getClass().getName());
                    if (value instanceof HibernateProxy
                            || value instanceof AbstractPersistentCollection
                            || (clazz.getName().startsWith("com.aeroscout.mobileview.model.")
                                    && !clazz.isEnum())
                                    && !clazz.isPrimitive()) {
                        Object transferable = createTransferable(value);
                        log("      > after deep clone: " + transferable);
                        writeMethod.invoke(dolly, transferable);
                        continue;
                    } else {
                        writeMethod.invoke(dolly, value);
                        continue;
                    }
                } finally {
                    currentPath.set(path);
                }
            }

            return dolly;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
