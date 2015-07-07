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

package com.mobandme.android.transformer.compiler.internal;

public class Tools {
    public final static String PACKAGE_PATTERN = "package %s;";
    public final static String CLASS_PATTERN = "public class %s {";
    public final static String TRANSFORMER_CLASS_NAME = "Transformer";
    public final static String TRANSFORMER_PACKAGE_PATTERN = "%s.transformer";
    public final static String TRANSFORMER_CLASS_PATTERN = "public final class %s extends AbstractTransformer {";
    public final static String IMPORT_PATTERN = "import %s.%s;";
    public final static String MAPPER_PACKAGE_PATTERN = "%s.mapper";
    public final static String MAPPER_CLASS_NAME_PATTERN = "%sMapper";
    public final static String MAPPER_CLASS_VAR_CONSTANT_PATTERN = "private final %s %s = new %s();";
    public final static String MAPPER_FIELD_PATTERN = "result.set%s(data.%s());";
    public final static String MAPPER_FIELD_COMPOSITE_PATTERN = "result.set%s(%s.transform(data.%s()));";
    public final static String MAPPER_FIELD_WITH_PARSER_PATTERN = "result.set%s(new %s().parse(data.%s()));";
}
