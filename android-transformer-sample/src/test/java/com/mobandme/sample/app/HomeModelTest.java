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
package com.mobandme.sample.app;

import com.mobandme.android.transformer.Transformer;
import com.mobandme.sample.app.domain.Home;
import com.mobandme.sample.app.domain.HomeColor;
import com.mobandme.sample.app.model.HomeColorModel;
import com.mobandme.sample.app.model.HomeModel;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class HomeModelTest extends BaseTest {
    public static final String STREET_VALUE = "My Street, 65, 3";
    public static final String CITY_VALUE = "Barcelona";
    public static final String COUNTRY_VALUE = "Spain";
    public static final String POSTCODE_VALUE = "08006";
    public static final Calendar DATE_VALUE = GregorianCalendar.getInstance();
    public static final HomeColorModel HOME_COLOR_MODEL = new HomeColorModel();
    public static final HomeColor HOME_COLOR = new HomeColor();
    public static final String COLOR_HEX_VALUE = "#FF0000";
    public static final String COLOR_NAME_VALUE = "Red";
    public static final String DATE_AS_STRING_VALUE = "1980-08-23";

    @Override
    public void setup() { }

    @Test
    public void canBuildForwardTransformer() {
        final Transformer transformer = new Transformer
                                                .Builder()
                                                .build(HomeModel.class);
        assertNotNull(transformer);
    }

    @Test
    public void canBuildReverseTransformer() {
        final Transformer transformer = new Transformer
                                                .Builder()
                                                .build(Home.class);

        assertNotNull(transformer);
    }

    @Test
    public void canTransformForward() {
        HomeModel homeModel = new HomeModel();
        homeModel.Address = STREET_VALUE;
        homeModel.City = CITY_VALUE;
        homeModel.Country = COUNTRY_VALUE;
        homeModel.PostalCode = POSTCODE_VALUE;
        homeModel.Date = DATE_VALUE;
        homeModel.HomeColor = HOME_COLOR_MODEL;
        homeModel.HomeColor.setColorHex(COLOR_HEX_VALUE);
        homeModel.HomeColor.setColorName(COLOR_NAME_VALUE);

        final Transformer transformer = new Transformer
                                                .Builder()
                                                .build(HomeModel.class);
        Home home = (Home)transformer.transform(homeModel);

        assertNotNull(home);
        assertEquals(home.PostalAddress, STREET_VALUE);
        assertEquals(home.City, CITY_VALUE);
        assertEquals(home.PostalCode, POSTCODE_VALUE);
        assertEquals(home.Country, COUNTRY_VALUE);
        assertEquals(home.HomeColor.getColorHex(), COLOR_HEX_VALUE);
        assertEquals(home.HomeColor.getColorName(), COLOR_NAME_VALUE);
    }

    @Test
    public void canParseForward() {
        String expectedValue = new SimpleDateFormat("yyyy-MM-dd").format(DATE_VALUE.getTime());
        HomeModel homeModel = new HomeModel();
        homeModel.Date = DATE_VALUE;

        final Transformer transformer = new Transformer
                .Builder()
                .build(HomeModel.class);
        Home home = (Home)transformer.transform(homeModel);

        assertNotNull(home);
        assertEquals(home.Date, expectedValue);
    }

    @Test
    public void canTransformReverse() {
        Home home = new Home();
        home.PostalAddress = STREET_VALUE;
        home.City = CITY_VALUE;
        home.Country = COUNTRY_VALUE;
        home.PostalCode = POSTCODE_VALUE;
        home.Date = DATE_AS_STRING_VALUE;
        home.HomeColor = HOME_COLOR;
        home.HomeColor.setColorHex(COLOR_HEX_VALUE);
        home.HomeColor.setColorName(COLOR_NAME_VALUE);

        final Transformer transformer = new Transformer
                                                .Builder()
                                                .build(HomeModel.class);
        HomeModel homeModel = (HomeModel)transformer.transform(home);

        assertNotNull(homeModel);
        assertEquals(homeModel.Address, STREET_VALUE);
        assertEquals(homeModel.City, CITY_VALUE);
        assertEquals(homeModel.Country, COUNTRY_VALUE);
        assertEquals(homeModel.PostalCode, POSTCODE_VALUE);
        assertNotNull(homeModel.HomeColor);
        assertEquals(homeModel.HomeColor.getColorHex(), COLOR_HEX_VALUE);
        assertEquals(homeModel.HomeColor.getColorName(), COLOR_NAME_VALUE);
    }

    @Test
    public void canParseReverse() {
        Home home = new Home();
        home.Date = DATE_AS_STRING_VALUE;

        final Transformer transformer = new Transformer
                                                .Builder()
                                                .build(HomeModel.class);
        HomeModel homeModel = (HomeModel)transformer.transform(home);
        String expectedValue = new SimpleDateFormat("yyyy-MM-dd").format(homeModel.Date.getTime());

        assertNotNull(homeModel);
        assertEquals(expectedValue, DATE_AS_STRING_VALUE);
    }
}
