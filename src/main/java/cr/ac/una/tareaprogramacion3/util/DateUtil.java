package cr.ac.una.tareaprogramacion3.util;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Date;
import java.util.GregorianCalendar;

public final class DateUtil {
    private DateUtil() {}

    /** Convierte XMLGregorianCalendar -> java.util.Date (o null). */
    public static Date fromXml(XMLGregorianCalendar xcal) {
        if (xcal == null) return null;
        return xcal.toGregorianCalendar().getTime();
    }

    /** Convierte java.util.Date -> XMLGregorianCalendar (o null). */
    public static XMLGregorianCalendar toXml(Date date) {
        if (date == null) return null;
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(date);
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException("Error convirtiendo Date a XMLGregorianCalendar: " + e.getMessage(), e);
        }
    }
}
