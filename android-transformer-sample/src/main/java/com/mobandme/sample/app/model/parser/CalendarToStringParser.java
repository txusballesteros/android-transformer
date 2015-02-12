package com.mobandme.sample.app.model.parser;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import com.mobandme.android.transformer.parser.AbstractParser;

public class CalendarToStringParser extends AbstractParser<Calendar, String> {
    
    @Override
    protected String onParse(Calendar value) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormatter.format(value.getTime());
    }
}
