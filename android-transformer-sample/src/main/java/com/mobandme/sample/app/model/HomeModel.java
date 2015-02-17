/* 
 * Copyright Txus Ballesteros 2015 (@txusballesteros)
 *
 * This file is part of some open source application.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * Contact: Txus Ballesteros <txus.ballesteros@gmail.com>
 */

package com.mobandme.sample.app.model;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.mobandme.android.transformer.compiler.Mappable;
import com.mobandme.android.transformer.compiler.Mapped;
import com.mobandme.android.transformer.compiler.Parse;
import com.mobandme.sample.app.domain.Home;
import com.mobandme.sample.app.model.parser.CalendarToStringParser;
import com.mobandme.sample.app.model.parser.StringToCalendarParser;

@Mappable( with = Home.class )
public class HomeModel {
    
    @Mapped(toField = "PostalAddress") public String Address;
    @Mapped public HomeColorModel HomeColor;
    @Mapped public String City;
    @Mapped public String PostalCode;
    @Mapped public String Country;

    @Parse(
        originToDestinationWith = CalendarToStringParser.class,
        destinationToOriginWith = StringToCalendarParser.class 
    )
    @Mapped public Calendar Date;

    @Override public String toString() {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
        
        return "HomeModel{" +
                "Address='" + Address + '\'' +
                ", HomeColor=" + HomeColor +
                ", City='" + City + '\'' +
                ", PostalCode='" + PostalCode + '\'' +
                ", Country='" + Country + '\'' +
                ", Date=" + dateFormatter.format(Date.getTime()) +
                '}';
    }
}
