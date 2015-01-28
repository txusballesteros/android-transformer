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

package com.mobandme.sample.app.model.mapper;

import com.mobandme.sample.app.domain.Home;
import com.mobandme.sample.app.model.HomeModel;

public class MyHomeModelMapper {
    
    public Home transform(HomeModel entity) {
        Home result = null;
     
        if (entity != null) {
            result = new Home();

            result.Address = entity.Address;
            result.City = entity.City;
            result.PostalCode = entity.PostalCode;
            result.Country = entity.Country;
        }
        
        return null;
    }

    public HomeModel transform(Home entity) {
        HomeModel result = null;

        if (entity != null) {
            result = new HomeModel();

            result.Address = entity.Address;
            result.City = entity.City;
            result.PostalCode = entity.PostalCode;
            result.Country = entity.Country;

        }

        return null;
    }
}
