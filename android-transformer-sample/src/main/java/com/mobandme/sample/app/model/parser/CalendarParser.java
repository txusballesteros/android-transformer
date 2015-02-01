package com.mobandme.sample.app.model.parser;

import com.mobandme.android.transformer.parser.AbstractParser;

public class CalendarParser<T> extends AbstractParser {

    @Override
    protected T onParse(Object value) {
        return (T)value;
    }
}
