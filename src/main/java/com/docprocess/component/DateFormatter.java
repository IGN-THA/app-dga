package com.docprocess.component;

import com.docprocess.config.ErrorConfig;
import com.docprocess.manager.DocumentRenderException;
import com.docprocess.manager.docx.TemplateFormatter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ThaiBuddhistChronology;
import java.time.chrono.ThaiBuddhistDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class DateFormatter {

    Logger logger = LogManager.getLogger(DateFormatter.class);
    public String format(OffsetDateTime dateTime, Locale locale) {
        logger.info("date to format3 " + dateTime);
        return format(dateTime.toLocalDate(), locale);
    }
    public String format(String dob, Locale locale){
        DateTimeFormatter mainFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        //System.out.println("date to format "+dob);
        if(dob.contains("-")) {
            dob=mainFormatter.format(LocalDate.parse(dob, DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }else{
            if(locale.getLanguage().equalsIgnoreCase("en") || locale.getLanguage().equalsIgnoreCase("zh")) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy").withChronology(ThaiBuddhistChronology.INSTANCE);
                    ChronoLocalDate thaiDate = ChronoLocalDate.from(LocalDate.parse(dob, formatter));
                    dob= mainFormatter.format(thaiDate);
                }catch (Exception ex){

                    String errorMessage = ErrorConfig.getErrorMessages(DateFormatter.class.getName(), "format", ex);
                    logger.error(errorMessage);
                }
                //return dob;
            }
        }
        return dob;
    }
    public String format(LocalDate dateTime, Locale locale) {
        //System.out.println("date to format2 "+dateTime);
        if (locale.getLanguage().equalsIgnoreCase("en")) {
            return dateTime.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH));
        } else if (locale.getLanguage().equalsIgnoreCase("th")) {
            return ThaiBuddhistDate.from(dateTime).format(DateTimeFormatter.ofPattern("dd MMM yyyy").withLocale(new Locale("th")));
        } else {
            return dateTime.format(DateTimeFormatter.ofPattern("dd'th' MMMM yyyy", Locale.CHINESE));
        }
    }
}
