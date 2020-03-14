package vn.evolus.simpleapi.gateway;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.thoughtworks.xstream.converters.SingleValueConverter;

public class XStreamDateConverter implements SingleValueConverter {
    private static final String PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private static final String PATTERN2 = "yyyy-MM-dd HH:mm:ss";

    @SuppressWarnings({ "rawtypes" })
    @Override
    public boolean canConvert(Class clazz) {
        return Date.class.isAssignableFrom(clazz);
    }

    @Override
    public Object fromString(String s) {
        return parseDate(s);
    }

    public static Date parseDate(String s) {
        if (s == null || "null".equalsIgnoreCase(s)) return null;
        try {
            return new SimpleDateFormat(PATTERN).parse(s);
        } catch (Exception e) {
            try {
                return new SimpleDateFormat(PATTERN2).parse(s);
            } catch (Exception ex) {
                System.err.println("Failed to parse the following date: '" + s + "'");
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public String toString(Object object) {
        // System.out.println("Converting object: " + object);
        if (!(object instanceof Date)) return "" + object;
        return new SimpleDateFormat(PATTERN).format((Date) object);
    }
}
