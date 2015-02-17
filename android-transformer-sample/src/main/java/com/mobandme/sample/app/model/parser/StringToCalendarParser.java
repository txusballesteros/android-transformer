package com.mobandme.sample.app.model.parser;

import com.mobandme.android.transformer.parser.AbstractParser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class StringToCalendarParser extends AbstractParser<String, Calendar> {
    @Override
    protected Calendar onParse(String value) {
        Calendar calendar = GregorianCalendar.getInstance();
        
        try {
            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
            calendar.setTime(dateFormatter.parse(value));
        } catch (ParseException e) { }
        
        return calendar;
    }
}
