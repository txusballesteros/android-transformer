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

import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
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

    RoundEnvironment roundEnvironment;
            
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        roundEnvironment = roundEnv;
        
        writeTrace("Processing android transformer annotations.");

        processMappableAnnotationElements();

        processMappingAnnotationElements();
        
        return true;
    }
    
    private void processMappableAnnotationElements() {
        for (Element mappableElement : roundEnvironment.getElementsAnnotatedWith(Mappable.class)) {
            if (mappableElement.getKind() == ElementKind.CLASS) {

                AnnotationMirror annotationMirror = getAnnotationMirror(mappableElement, Mappable.class);
                AnnotationValue  annotationValue = getAnnotationValue(annotationMirror, "with");
                TypeElement linkedElement = getTypeElement(annotationValue);
                
                ClassInfo classInfo = extractClassInformation(mappableElement);
                ClassInfo linkedClassInfo = extractClassInformation(linkedElement);

                writeTrace(String.format("\tProcessing class %s.%s linked to %s.%s", classInfo.classPackage, classInfo.className, linkedClassInfo.classPackage, linkedClassInfo.className));
            }
        }
    }
    
    private void processMappingAnnotationElements() {
        for (Element mappingElement : roundEnvironment.getElementsAnnotatedWith(Mapping.class)) {
            if (mappingElement.getKind() == ElementKind.FIELD) {
                VariableElement variableElement = (VariableElement)mappingElement;
                
                Mapping mappingAnnotation = variableElement.getAnnotation(Mapping.class);
                
                String fieldName = mappingElement.getSimpleName().toString();
                String fieldTypeName = variableElement.asType().toString();
                String linkToFieldName = mappingAnnotation.withFieldName();
                
                writeTrace(String.format("\t\tProcessing field %s, %s linked to %s", fieldName, fieldTypeName, linkToFieldName));
            }
        }
    }
    
    private ClassInfo extractClassInformation(Element element) {
        PackageElement packageElement = (PackageElement)element.getEnclosingElement();
        String className = element.getSimpleName().toString();
        String packageName = packageElement.getQualifiedName().toString();
        
        return new ClassInfo(packageName, className);
    }
    
    private AnnotationMirror getAnnotationMirror(Element element, Class<?> annotationType) {
        AnnotationMirror result = null;
        
        String annotationClassName = annotationType.getName();
        for(AnnotationMirror mirror : element.getAnnotationMirrors()) {
            if(mirror.getAnnotationType().toString().equals(annotationClassName)) {
                result = mirror;
                break;
            }
        }
        
        return result;
    }
    
    private AnnotationValue getAnnotationValue(AnnotationMirror annotation, String field) {
        AnnotationValue result = null;

        if (annotation != null) {
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotation.getElementValues().entrySet()) {
                if (entry.getKey().getSimpleName().toString().equals(field)) {
                    return entry.getValue();
                }
            }
        }
        
        return result;
    }

    private TypeElement getTypeElement(AnnotationValue value) {
        TypeElement result = null;
        
        if (value != null) {
            TypeMirror typeMirror = (TypeMirror)value.getValue();
            Types TypeUtils = processingEnv.getTypeUtils();
            result = (TypeElement)TypeUtils.asElement(typeMirror);
        }
        
        return result;
    }
    
    private void writeTrace(String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, message);
    }
    
    private class ClassInfo {
        String classPackage;
        String className;
        
        public ClassInfo(String pacakge, String name) {
            classPackage = pacakge;
            className = name;
        }
    }
}
