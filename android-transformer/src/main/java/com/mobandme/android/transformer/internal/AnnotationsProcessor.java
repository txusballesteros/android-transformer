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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.IOException;
import java.util.Collection;
import java.io.BufferedWriter;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import javax.lang.model.util.Types;
import javax.lang.model.SourceVersion;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
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

    private final static String PACKAGE_PATTERN = "package %s;";
    private final static String CLASS_PATTERN = "public class %s {";
    private final static String MAPPER_PACKAGE_PATTERN = "%s.mapper";
    private final static String IMPORT_PATTERN = "import %s.%s;";
    private final static String MAPPER_CLASS_NAME_PATTERN = "%sMapper";
    private final static String MAPPER_FIELD_PATTERN = "result.%s = data.%s;";
    
    RoundEnvironment roundEnvironment;
    Map<String, MapperInfo> mappersList;
    
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        roundEnvironment = roundEnv;
        mappersList = new HashMap<>();
        
        writeTrace("Processing android transformer annotations.");

        processMappableAnnotationElements();

        processMappingAnnotationElements();

        buildMappers();
        
        return true;
    }
    
    private void buildMappers() {
        for (MapperInfo mapper : this.mappersList.values()) {
            Collection<String> mapperImports = new ArrayList<>();
            Collection<String> directFields = new ArrayList<>();
            Collection<String> inverseFields = new ArrayList<>();
            
            String mapperPackage = String.format(MAPPER_PACKAGE_PATTERN, mapper.packageName);
            String mapperClassName = String.format(MAPPER_CLASS_NAME_PATTERN, mapper.className);

            mapperImports.add("import java.util.ArrayList;");
            mapperImports.add("import java.util.Collection;");
            mapperImports.add(String.format(IMPORT_PATTERN, mapper.packageName, mapper.className));
            mapperImports.add(String.format(IMPORT_PATTERN, mapper.linkedPackageName, mapper.linkedClassName));

            writeTrace(String.format("Building mapper %s.%s", mapperPackage, mapperClassName));
            
            for (MapperFieldInfo mapperField : mapper.getFields()) {
                String originFieldName = mapperField.fieldName;
                String destinationFieldName = mapperField.fieldName;
                
                if (mapperField.withFieldName != null && !mapperField.withFieldName.trim().equals(""))
                    destinationFieldName = mapperField.withFieldName;
                
                directFields.add(String.format(MAPPER_FIELD_PATTERN, originFieldName, destinationFieldName));
                inverseFields.add(String.format(MAPPER_FIELD_PATTERN, destinationFieldName, originFieldName));
            }

            generateMapperJavaFile(mapper, mapperPackage, mapperClassName, mapperImports, directFields, inverseFields);
        }
    }
    
    private void generateMapperJavaFile(MapperInfo mapper, String packageName, String className, Collection<String> imports, Collection<String> directFields, Collection<String> inversFields) {
        writeTrace(String.format("Generating source file for mapper %s.%s", packageName, className));

        try {

            JavaFileObject javaFileObject = processingEnv.getFiler().createSourceFile(className);
            BufferedWriter buffer = new BufferedWriter(javaFileObject.openWriter());
            
            buffer.append(String.format(PACKAGE_PATTERN, packageName));
            buffer.newLine();
            
            for (String classImport : imports) {
                buffer.newLine();
                buffer.append(classImport);
            }

            buffer.newLine();
            buffer.newLine();
            buffer.append(String.format(CLASS_PATTERN, className));

            generateTransformListMethod(buffer, mapper.className, mapper.linkedClassName);
            //generateTransformListMethod(buffer, mapper.linkedClassName, mapper.className);
            generateTransformMethod(buffer, mapper.className, mapper.linkedClassName, directFields);
            generateTransformMethod(buffer, mapper.linkedClassName, mapper.className, inversFields);

            buffer.newLine();
            buffer.append("}");
            buffer.close();
            
        } catch (IOException error) {
            throw new RuntimeException(error);
        }
    }

    private void generateTransformListMethod(BufferedWriter buffer, String className, String linkedClassName) throws IOException {
        buffer.newLine();
        buffer.newLine();
        buffer.append(String.format("\tpublic Collection<%s> transform(Collection<%s> data) {", linkedClassName, className));
        buffer.newLine();
        buffer.append(String.format("\t\tCollection<%s> result = new ArrayList<%s>();", linkedClassName, linkedClassName));
        buffer.newLine();
        buffer.newLine();
        buffer.append(String.format("\t\tfor (%s element : data) {", className));
        buffer.newLine();
        buffer.append(String.format("\t\t\t%s transformedElement = transform(element);", linkedClassName));
        buffer.newLine();
        buffer.append("\t\t\tif (transformedElement != null)");
        buffer.newLine();
        buffer.append("\t\t\t\tresult.add(transformedElement);");

        buffer.newLine();
        buffer.append("\t\t}");
        buffer.newLine();
        buffer.newLine();
        buffer.append("\t\treturn result;");
        buffer.newLine();
        buffer.append("\t}");
    }
    
    private void generateTransformMethod(BufferedWriter buffer, String className, String linkedClassName, Collection<String> fields) throws IOException {
        buffer.newLine();
        buffer.newLine();
        buffer.append(String.format("\tpublic %s transform(%s data) {", linkedClassName, className));
        buffer.newLine();
        buffer.append(String.format("\t\t%s result = null;", linkedClassName));

        buffer.newLine();
        buffer.newLine();
        buffer.append("\t\tif (data != null) {");

        buffer.newLine();
        buffer.append(String.format("\t\t\tresult = new %s();", linkedClassName));
        buffer.newLine();
        
        for(String field : fields) {
            buffer.newLine();
            buffer.append(String.format("\t\t\t%s", field));
        }

        buffer.newLine();
        buffer.append("\t\t}");
        buffer.newLine();
        buffer.newLine();
        buffer.append("\t\treturn result;");
        buffer.newLine();
        buffer.append("\t}");
    }
    
    private void processMappableAnnotationElements() {
        for (Element mappableElement : roundEnvironment.getElementsAnnotatedWith(Mappable.class)) {
            if (mappableElement.getKind() == ElementKind.CLASS) {

                AnnotationMirror annotationMirror = getAnnotationMirror(mappableElement, Mappable.class);
                AnnotationValue  annotationValue = getAnnotationValue(annotationMirror, "with");
                TypeElement linkedElement = getTypeElement(annotationValue);
                
                ClassInfo mappableClassInfo = extractClassInformation(mappableElement);
                ClassInfo linkedClassInfo = extractClassInformation(linkedElement);

                if (!haveMapper(mappableClassInfo))
                    createMapper(mappableClassInfo, linkedClassInfo);
                
                writeTrace(String.format("\tProcessing class %s.%s linked to %s.%s", mappableClassInfo.packageName, mappableClassInfo.className, linkedClassInfo.packageName, linkedClassInfo.className));
            }
        }
    }

    private void processMappingAnnotationElements() {
        for (Element mappingElement : roundEnvironment.getElementsAnnotatedWith(Mapping.class)) {
            if (mappingElement.getKind() == ElementKind.FIELD) {
                Mapping mappingAnnotation = mappingElement.getAnnotation(Mapping.class);

                String fieldName = mappingElement.getSimpleName().toString();
                String withFieldName = mappingAnnotation.withFieldName();
                
                MapperFieldInfo mappingFieldInfo = new MapperFieldInfo(fieldName, withFieldName);
                
                ClassInfo classInfo = extractClassInformationFromField(mappingElement);
                getMapper(classInfo)
                        .getFields()
                            .add(mappingFieldInfo);
                
                writeTrace(String.format("\t\tProcessing field %s.%s linked to %s", classInfo.getFullName(), fieldName, withFieldName));
            }
        }
    }

    private boolean haveMapper(ClassInfo classInfo) {
        String mapperClassFullName = classInfo.getFullName();
        boolean result = mappersList.containsKey(mapperClassFullName);
        return result;
    }

    private MapperInfo createMapper(ClassInfo classInfo, ClassInfo linkedClassInfo) {
        MapperInfo mapper = new MapperInfo(classInfo.packageName, classInfo.className, linkedClassInfo.packageName, linkedClassInfo.className);
        mappersList.put(mapper.getFullName(), mapper);
        return mapper;
    }

    private MapperInfo getMapper(ClassInfo classInfo) {
        MapperInfo result = mappersList.get(classInfo.getFullName());
        return result;
    }
    
    private ClassInfo extractClassInformationFromField(Element element) {
        Element classElement = element.getEnclosingElement();
        return extractClassInformation(classElement);
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
        public final String className;
        public final String packageName;
        
        public ClassInfo(String packageName, String className) {
            this.packageName = packageName;
            this.className = className;
        }

        public String getFullName() {
            return String.format("%s.%s", packageName, className);
        }
        
        @Override
        public String toString() {
            return String.format("%s.%s", packageName, className);
        }
    }
    
    private class MapperInfo extends ClassInfo {
        
        public final String linkedClassName;
        public final String linkedPackageName;

        private List<MapperFieldInfo> mappingsList = new ArrayList<>();
        
        public List<MapperFieldInfo> getFields() { return mappingsList; }
        
        public MapperInfo(String packageName, String className, String linkedPackageName, String linkedClassName) {
            super(packageName, className);
            
            this.linkedPackageName = linkedPackageName;
            this.linkedClassName = linkedClassName;
        }
    }
    
    private class MapperFieldInfo {
        public final String fieldName;
        public final String withFieldName;
        
        public MapperFieldInfo(String fieldName, String withFieldName) {
            this.fieldName = fieldName;
            this.withFieldName = withFieldName;
        }
    }
}
