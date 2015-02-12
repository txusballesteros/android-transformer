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

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import com.mobandme.android.transformer.Transformer;
import com.mobandme.sample.app.data.entity.HomeEntity;
import com.mobandme.sample.app.domain.Home;
import com.mobandme.sample.app.model.HomeColorModel;
import com.mobandme.sample.app.model.HomeModel;

import java.util.GregorianCalendar;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {

            Transformer homeModelTransformer = new Transformer
                                                        .Builder()
                                                            .build(HomeModel.class);

            Transformer homeEntityTransformer = new Transformer
                                                        .Builder()
                                                                .build(HomeEntity.class);
            
            HomeModel homeModel = new HomeModel();
            homeModel.Address = "My Street, 65, 3";
            homeModel.City = "Bilbao";
            homeModel.Country = "Spain";
            homeModel.PostalCode = "48903";
            homeModel.Date = GregorianCalendar.getInstance();
            homeModel.HomeColor = new HomeColorModel();
            homeModel.HomeColor.colorHex = "#FF0000";
            homeModel.HomeColor.colorName = "Red";

            Home homeDomain = null;
            HomeEntity homeEntity = null;
            homeDomain = homeModelTransformer.transform(homeModel, Home.class);
            homeEntity = homeEntityTransformer.transform(homeDomain, HomeEntity.class);

            Log.d(TAG, homeDomain.toString());
            Log.d(TAG, homeModel.toString());
            homeDomain = null;
            homeModel = null;
            homeDomain = homeEntityTransformer.transform(homeEntity, Home.class);
            homeModel = homeModelTransformer.transform(homeDomain, HomeModel.class);

            Log.d(TAG, homeDomain.toString());
            Log.d(TAG, homeModel.toString());

        } catch (Exception e) {
            Log.e("android-transformer", e.getMessage(), e);
        }
    }
}
