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

package com.mobandme.android.transformer;

import com.mobandme.android.transformer.internal.AbstractTransformer;
import com.mobandme.android.transformer.internal.Tools;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

public final class Transformer {
    public static class Builder {
        
        public Transformer build(Class<?> type) {
            if (type == null)
                throw new IllegalArgumentException("The 'type' parameter cannot be null.");
            
            return new Transformer(type);
        }
    }
    
    private Class<?> transformerType;
    
    private Transformer(Class<?> type) {
        this.transformerType = type;
    }



    /**
     * Use this method to transform your POJO object to the linked POJO object.
     * @param value Instance of the source object.
     * @return An instance of the converted object.
     */
    public Object transform(Object value) {
        return transform(value, null);
    }

    /**
     * Use this method to transform your POJO object to the linked POJO object.
     * @param value Instance of the source object.
     * @param expectedReturnType Use this argument to set the return expected type.*
     * @param <T> Generic type
     * @return An instance of the converted object.
     */
    public <T> T transform(Object value, Class<T> expectedReturnType) {
        Object result;

        if (value == null)
            throw new IllegalArgumentException("The 'value' parameter cannot be null.");
        
        try {

            String transformerCanonicalName = getTransformerCanonicalName();
            AbstractTransformer transformer = getTransformerInstance(transformerCanonicalName);
            Object mapperInstance = getMapperInstance(transformer, value);
            result = executeTransformation(mapperInstance, value);
            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        return (T)result;
    }
    
    private String getTransformerCanonicalName() {
        String result;
        
        String packageName = String.format(Tools.TRANSFORMER_PACKAGE_PATTERN, transformerType.getPackage().getName());
        String className = Tools.TRANSFORMER_CLASS_NAME;
        result = String.format("%s.%s", packageName, className);
        
        return result;
    }
    
    private AbstractTransformer getTransformerInstance(String transformerCanonicalName) {
        AbstractTransformer result = null;
        
        try {
            
            Class<?> transformerClass = Class.forName(transformerCanonicalName);
            Constructor transformerConstructor = transformerClass.getConstructor();
            result = (AbstractTransformer)transformerConstructor.newInstance();
            
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
        
        return result;
    }
    
    private Object getMapperInstance(AbstractTransformer transformer, Object value) {
        Object result = transformer.getMapper(value);
        return result;
    }
    
    private Object executeTransformation(Object mapper, Object value) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Object result = null;
        Method transforMethod = mapper.getClass().getMethod("transform", value.getClass());
        if (transforMethod != null)
            result = transforMethod.invoke(mapper, value);
        
        return result;
    }
}
