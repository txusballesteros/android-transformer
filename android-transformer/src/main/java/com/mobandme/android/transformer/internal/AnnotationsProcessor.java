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

package com.mobandme.android.transformer.internal;

import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.AbstractProcessor;

import com.mobandme.android.transformer.Mapping;
import com.mobandme.android.transformer.Mappable;
import javax.annotation.processing.SupportedSourceVersion;
import javax.annotation.processing.SupportedAnnotationTypes;

@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedAnnotationTypes({
    "com.mobandme.android.transformer.Mapping",
    "com.mobandme.android.transformer.Mappable"
})
public class AnnotationsProcessor extends AbstractProcessor {
    
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        writeTrace("Processing android transformer annotations.");

        processMappableElements(roundEnv);

        processMappingElements(roundEnv);
        
        return true;
    }
    
    private void processMappableElements(RoundEnvironment roundEnv) {
        for (Element mappableElement : roundEnv.getElementsAnnotatedWith(Mappable.class)) {
            if (mappableElement.getKind() == ElementKind.CLASS) {
                TypeElement classElement = (TypeElement)mappableElement;
                PackageElement packageElement = (PackageElement)classElement.getEnclosingElement();
                
                Mappable mappableAnnotation = mappableElement.getAnnotation(Mappable.class);

                String withName = mappableAnnotation.with().toString();
                String className = classElement.getSimpleName().toString();
                String packageName = packageElement.getQualifiedName().toString();
                        
                writeTrace(String.format("\tProcessing class %s.%s linked to %s", packageName, className, withName));
            }
        }
    }
    
    private void processMappingElements(RoundEnvironment roundEnv) {
        for (Element mappingElement : roundEnv.getElementsAnnotatedWith(Mapping.class)) {
            if (mappingElement.getKind() == ElementKind.FIELD) {
                VariableElement variableElement = (VariableElement)mappingElement;
                String fieldName = mappingElement.getSimpleName().toString();
                String fieldTypeName = variableElement.asType().toString();

                writeTrace(String.format("\t\tProcessing field %s, %s", fieldName, fieldTypeName));
            }
        }
    }
    
    private void writeTrace(String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, message);
    }
}
