/*
 * Copyright (c) 2015 Selltag. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Selltag ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with 
 * the terms of the license agreement you entered into with Selltag.
 */
package com.mobandme.sample.app.model;

import com.mobandme.android.transformer.Mappable;
import com.mobandme.android.transformer.Mapped;
import com.mobandme.sample.app.domain.HomeColor;

@Mappable( with = HomeColor.class )
public class HomeColorModel {
    @Mapped
    public String colorName;
    @Mapped
    public String colorHex;

    @Override public String toString() {
        return "HomeColorModel{" +
                "colorName='" + colorName + '\'' +
                ", colorHex='" + colorHex + '\'' +
                '}';
    }
}
