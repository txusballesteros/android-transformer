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

import com.mobandme.android.transformer.compiler.Mappable;
import com.mobandme.android.transformer.compiler.Mapped;
import com.mobandme.android.transformer.compiler.Parse;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;


@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedAnnotationTypes({
        "com.mobandme.android.transformer.compiler.Mapping",
        "com.mobandme.android.transformer.compiler.Mappable",
        "com.mobandme.android.transformer.compiler.Parse"
})
public class AnnotationsProcessor extends AbstractProcessor {

    final String BOOLEAN_FIELD_PREFIX = "is";
    final String SOME_FIELD_PREFIX = "get";

    RoundEnvironment roundEnvironment;
    Map<String, MapperInfo> mappersList;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        roundEnvironment = roundEnv;
        mappersList = new HashMap<>();

        processMappableAnnotationElements();
        processMappedAnnotationElements();
        processParseAnnotationElements();

        buildMapperObjects();
        generateTransformersJavaFiles();

        return true;
    }

    private void buildMapperObjects() {
        for (MapperInfo mapper : this.mappersList.values()) {
            Collection<String> mapperImports = new ArrayList<>();
            Collection<String> classVars = new ArrayList<>();
            Collection<String> directFields = new ArrayList<>();
            Collection<String> inverseFields = new ArrayList<>();

            mapperImports.add("import java.util.ArrayList;");
            mapperImports.add("import java.util.Collection;");
            mapperImports.add(String.format(Tools.IMPORT_PATTERN, mapper.packageName, mapper.className));
            mapperImports.add(String.format(Tools.IMPORT_PATTERN, mapper.linkedPackageName, mapper.linkedClassName));

            for (MapperFieldInfo mapperField : mapper.getFields()) {
                String originFieldName = mapperField.fieldName;
                String destinationFieldName = mapperField.fieldName;

                if (mapperField.withFieldName != null && !mapperField.withFieldName.trim().equals(""))
                    destinationFieldName = mapperField.withFieldName;

                if (!mapperField.isPublicField) {
                    destinationFieldName = toUpperCamelCase(destinationFieldName);
                    originFieldName = toUpperCamelCase(originFieldName);
                }

                if (mapperField.originToDestinationParserClassName == null && mapperField.destinationToOriginParserClassName == null) {
                    MapperInfo mapperInfo = mapperForMapperField(mapperField);
                    if (mapperInfo != null) {
                        mapperImports.add(String.format(Tools.IMPORT_PATTERN, mapperInfo.mapperPackageName, mapperInfo.mapperClassName));
                        classVars.add(String.format(Tools.MAPPER_CLASS_VAR_CONSTANT_PATTERN, mapperInfo.mapperClassName, toLowerCamelCase(mapperInfo.mapperClassName), mapperInfo.mapperClassName));

                        String mapperCompositePattern = getMapperCompositePattern(mapperField);
                        directFields.add(String.format(mapperCompositePattern, destinationFieldName, toLowerCamelCase(mapperInfo.mapperClassName), returnedFieldPrefix(mapperField, originFieldName)));
                        inverseFields.add(String.format(mapperCompositePattern, originFieldName, toLowerCamelCase(mapperInfo.mapperClassName), returnedFieldPrefix(mapperField, destinationFieldName)));
                    } else {
                        String mapperFieldPattern = getMapperFieldPattern(mapperField);
                        directFields.add(String.format(mapperFieldPattern, destinationFieldName, returnedFieldPrefix(mapperField, originFieldName)));
                        inverseFields.add(String.format(mapperFieldPattern, originFieldName, returnedFieldPrefix(mapperField, destinationFieldName)));
                    }
                } else {
                    mapperImports.add(String.format(Tools.IMPORT_PATTERN, mapperField.originToDestinationParserPackageName, mapperField.originToDestinationParserClassName));
                    mapperImports.add(String.format(Tools.IMPORT_PATTERN, mapperField.destinationToOriginParserPackageName, mapperField.destinationToOriginParserClassName));

                    String mapperFieldWithParserPattern = getMapperFieldWithParserPattern(mapperField);
                    directFields.add(String.format(mapperFieldWithParserPattern, destinationFieldName, mapperField.originToDestinationParserClassName, returnedFieldPrefix(mapperField, originFieldName)));
                    inverseFields.add(String.format(mapperFieldWithParserPattern, originFieldName, mapperField.destinationToOriginParserClassName, returnedFieldPrefix(mapperField, destinationFieldName)));
                }
            }

            generateMapperJavaFile(mapper, classVars, mapperImports, directFields, inverseFields);
        }
    }

    private String getMapperCompositePattern(MapperFieldInfo mapperField){
        String result;

        if (mapperField.isPublicField){
            result = Tools.MAPPER_FIELD_COMPOSITE_PATTERN;
        }else {
            result = Tools.MAPPER_STANDARD_FIELD_COMPOSITE_PATTERN;
        }

        return result;
    }

    private String getMapperFieldWithParserPattern(MapperFieldInfo mapperField){
        String result;

        if (mapperField.isPublicField){
            result = Tools.MAPPER_FIELD_WITH_PARSER_PATTERN;
        }else {
            result = Tools.MAPPER_STANDARD_FIELD_WITH_PARSER_PATTERN;
        }

        return result;
    }

    private String getMapperFieldPattern(MapperFieldInfo mapperField){
        String result;

        if (mapperField.isPublicField){
            result = Tools.MAPPER_FIELD_PATTERN;
        }else {
            result = Tools.MAPPER_STANDARD_FIELD_PATTERN;
        }

        return result;
    }

    private MapperInfo mapperForMapperField(MapperFieldInfo mapperField) {
        for (MapperInfo mapperInfo : mappersList.values()) {
            if (mapperField.fieldType.equals(mapperInfo.mappableClassName)) {
                return mapperInfo;
            }
        }
        return null;
    }

    private String toLowerCamelCase(String className) {
        return className.substring(0, 1).toLowerCase().concat(className.substring(1));
    }

    private String toUpperCamelCase(String fielName){
        return fielName.substring(0, 1).toUpperCase().concat(fielName.substring(1));
    }

    private String returnedFieldPrefix(MapperFieldInfo mapperField, String fieldName){
        if (mapperField.isPublicField)
            return fieldName;

        return returnedFieldPrefix(mapperField.fieldType, fieldName);
    }

    private String returnedFieldPrefix(String fieldType, String fieldName){
        String result;

        if (fieldType.equals("boolean")){
            if (fieldName.toLowerCase().startsWith(BOOLEAN_FIELD_PREFIX)){
                result = toLowerCamelCase(fieldName);
            }else{
                result = BOOLEAN_FIELD_PREFIX.concat(fieldName);
            }
        }else{
            result = SOME_FIELD_PREFIX.concat(fieldName);
        }

        return result;
    }

    private void generateMapperJavaFile(MapperInfo mapper, Collection<String> classVars, Collection<String> imports, Collection<String> directFields, Collection<String> inverseFields) {

        try {

            String mapperCanonicalName = String.format("%s.%s", mapper.mapperPackageName, mapper.mapperClassName);
            writeTrace(String.format("Generating source file for Mapper with name %s", mapperCanonicalName));

            JavaFileObject javaFileObject = processingEnv.getFiler().createSourceFile(mapperCanonicalName);
            BufferedWriter buffer = new BufferedWriter(javaFileObject.openWriter());

            buffer.append(String.format(Tools.PACKAGE_PATTERN, mapper.mapperPackageName));
            buffer.newLine();

            for (String classImport : imports) {
                buffer.newLine();
                buffer.append(classImport);
            }

            buffer.newLine();
            buffer.newLine();
            buffer.append(String.format(Tools.CLASS_PATTERN, mapper.mapperClassName));

            if (classVars.size() > 0) {
                buffer.newLine();
                for (String classVar : classVars) {
                    buffer.newLine();
                    buffer.append("\t").append(classVar);
                }
            }

            generateTransformMethod(buffer, mapper.className, mapper.linkedClassName, directFields);
            generateTransformMethod(buffer, mapper.linkedClassName, mapper.className, inverseFields);

            buffer.newLine();
            buffer.append("}");
            buffer.close();

        } catch (IOException error) {
            throw new RuntimeException(error);
        }
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

    private void generateTransformersJavaFiles() {
        Map<String, TransformerInfo> transformersList = new HashMap<>();
        
        if (mappersList.size() > 0) {
            for (MapperInfo mapper : mappersList.values()) {
                if (!transformersList.containsKey(mapper.packageName)) {
                    String packageName = String.format(Tools.TRANSFORMER_PACKAGE_PATTERN, mapper.packageName);
                    String className = Tools.TRANSFORMER_CLASS_NAME;
                    TransformerInfo transformer = new TransformerInfo(packageName, className);
                    transformersList.put(mapper.packageName, transformer);
                }
                
                TransformerInfo transformer = transformersList.get(mapper.packageName);
                transformer.getMappers().add(mapper);
            }
            
            generateTransformerJavaFile(transformersList);
        }
    }
    
    private void generateTransformerJavaFile(Map<String, TransformerInfo> transformers) {
        try {

            if (transformers.size() > 0) {
                for (TransformerInfo transformer : transformers.values()) {
                    String packageName = transformer.packageName;
                    String className = transformer.className;

                    String transformerCanonicalName = String.format("%s.%s", packageName, className);
                    writeTrace(String.format("Generating source file for Transformer class with name %s", transformerCanonicalName));

                    JavaFileObject javaFileObject = processingEnv.getFiler().createSourceFile(transformerCanonicalName);
                    BufferedWriter buffer = new BufferedWriter(javaFileObject.openWriter());

                    buffer.append(String.format(Tools.PACKAGE_PATTERN, packageName));
                    buffer.newLine();

                    //region "Class Imports Generation"

                    buffer.newLine();
                    buffer.append(String.format(Tools.IMPORT_PATTERN, "com.mobandme.android.transformer.internal", "AbstractTransformer"));
                    for (MapperInfo mapper : transformer.getMappers()) {
                        buffer.newLine();
                        buffer.append(String.format(Tools.IMPORT_PATTERN, mapper.mapperPackageName, mapper.mapperClassName));
                    }

                    //endregion

                    //region "Class Generation"

                    buffer.newLine();
                    buffer.newLine();
                    buffer.append(String.format(Tools.TRANSFORMER_CLASS_PATTERN, className));

                    //region "Constructor Generation"

                    buffer.newLine();
                    buffer.append(String.format("\tpublic %s() {", className));
                    buffer.newLine();
                    buffer.append("\t\tsuper();");

                    //region "Variable Inicialization"

                    buffer.newLine();
                    for (MapperInfo mapper : transformer.getMappers()) {
                        buffer.newLine();
                        buffer.append(String.format("\t\taddMapper(\"%s.%s\", new %s());", mapper.packageName, mapper.className, mapper.mapperClassName));
                        buffer.newLine();
                        buffer.append(String.format("\t\taddMapper(\"%s.%s\", new %s());", mapper.linkedPackageName, mapper.linkedClassName, mapper.mapperClassName));
                    }

                    //endregion

                    buffer.newLine();
                    buffer.append("\t}");

                    //endregion

                    buffer.newLine();
                    buffer.append("}");

                    //endregion

                    buffer.close();
                }
            }

        } catch (IOException error) {
            throw new RuntimeException(error);
        }
    }

    private void processMappableAnnotationElements() {
        for (Element mappableElement : roundEnvironment.getElementsAnnotatedWith(Mappable.class)) {
            if (mappableElement.getKind() == ElementKind.CLASS) {

                AnnotationMirror mappableAnnotationMirror = getAnnotationMirror(mappableElement, Mappable.class);
                AnnotationValue  annotationValue = getAnnotationValue(mappableAnnotationMirror, "with");
                TypeElement linkedElement = getTypeElement(annotationValue);

                ClassInfo mappableClassInfo = extractClassInformation(mappableElement);
                ClassInfo linkedClassInfo = extractClassInformation(linkedElement);

                if (!haveMapper(mappableClassInfo))
                    createMapper(mappableElement.asType().toString(), mappableClassInfo, linkedClassInfo);
            }
        }
    }

    private void processMappedAnnotationElements() {
        for (Element mappedElement : roundEnvironment.getElementsAnnotatedWith(Mapped.class)) {
            if (mappedElement.getKind() == ElementKind.FIELD) {
                Mapped mappedAnnotation = mappedElement.getAnnotation(Mapped.class);

                String fieldName = mappedElement.getSimpleName().toString();
                String fieldType = mappedElement.asType().toString();
                boolean isPublicField = mappedElement.getModifiers().contains(Modifier.PUBLIC);
                String toFieldName = mappedAnnotation.toField();

                MapperFieldInfo mappingFieldInfo = new MapperFieldInfo(fieldName, fieldType, toFieldName, isPublicField);

                ClassInfo classInfo = extractClassInformationFromField(mappedElement);
                getMapper(classInfo)
                            .getFields()
                                .add(mappingFieldInfo);
            }
        }
    }

    private void processParseAnnotationElements() {
        for (Element parseElement : roundEnvironment.getElementsAnnotatedWith(Parse.class)) {
            if (parseElement.getKind() == ElementKind.FIELD) {

                AnnotationMirror parseAnnotationMirror = getAnnotationMirror(parseElement, Parse.class);
                AnnotationValue  originToDestinationWithValue = getAnnotationValue(parseAnnotationMirror, "originToDestinationWith");
                AnnotationValue  destinationToOriginWithValue = getAnnotationValue(parseAnnotationMirror, "destinationToOriginWith");
                TypeElement originToDestinationValue = getTypeElement(originToDestinationWithValue);
                TypeElement destinationToOriginValue = getTypeElement(destinationToOriginWithValue);

                String fieldName = parseElement.getSimpleName().toString();
                ClassInfo ownerClass = extractClassInformationFromField(parseElement);
                ClassInfo originToDestinationParserClass = extractClassInformation(originToDestinationValue);
                ClassInfo destinationToOriginParserClass = extractClassInformation(destinationToOriginValue);

                MapperFieldInfo mapperField = getMapper(ownerClass).getField(fieldName);
                if (mapperField != null) {
                    mapperField.originToDestinationParserPackageName = originToDestinationParserClass.packageName;
                    mapperField.originToDestinationParserClassName = originToDestinationParserClass.className;
                    mapperField.destinationToOriginParserPackageName = destinationToOriginParserClass.packageName;
                    mapperField.destinationToOriginParserClassName = destinationToOriginParserClass.className;
                } else {
                    writeError(String.format("You have configured a @Parse annotation without a @Mapped annotation on %s.%s.", ownerClass.getFullName(), fieldName));
                }
            }
        }
    }

    private boolean haveMapper(ClassInfo classInfo) {
        String mapperClassFullName = classInfo.getFullName();
        return mappersList.containsKey(mapperClassFullName);
    }

    private MapperInfo createMapper(String mappableClassName, ClassInfo classInfo, ClassInfo linkedClassInfo) {
        MapperInfo mapper = new MapperInfo(mappableClassName, classInfo.packageName, classInfo.className, linkedClassInfo.packageName, linkedClassInfo.className);
        mappersList.put(mapper.getFullName(), mapper);
        return mapper;
    }

    private MapperInfo getMapper(ClassInfo classInfo) {
        return mappersList.get(classInfo.getFullName());
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
        if (annotation != null) {
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotation.getElementValues().entrySet()) {
                if (entry.getKey().getSimpleName().toString().equals(field)) {
                    return entry.getValue();
                }
            }
        }
        
        return null;
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

    private void writeError(String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message);
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

    private class TransformerInfo extends ClassInfo {
        private List<MapperInfo> mappers;
       
        public List<MapperInfo> getMappers() {
            return mappers;     
        }
        
        public TransformerInfo(String packageName, String className) {
            super(packageName, className);
            mappers = new ArrayList<>();
        }
    }
    
    private class MapperInfo extends ClassInfo {
        public final String mapperClassName;
        public final String mapperPackageName;
        public final String linkedClassName;
        public final String linkedPackageName;
        public final String mappableClassName;

        private List<MapperFieldInfo> mappedFieldsList = new ArrayList<>();

        public List<MapperFieldInfo> getFields() { return mappedFieldsList; }

        public MapperFieldInfo getField(String fieldName) {
            MapperFieldInfo result = null;
            for (MapperFieldInfo field : mappedFieldsList) {
                if (field.fieldName.equals(fieldName)){
                    result = field;
                    break;
                }
            }
            return result;
        }
        
        public MapperInfo(String mappableClassName, String packageName, String className, String linkedPackageName, String linkedClassName) {
            super(packageName, className);

            this.mappableClassName = mappableClassName;
            this.mapperClassName = String.format(Tools.MAPPER_CLASS_NAME_PATTERN, className);
            this.mapperPackageName = String.format(Tools.MAPPER_PACKAGE_PATTERN, packageName);
            this.linkedPackageName = linkedPackageName;
            this.linkedClassName = linkedClassName;
        }
    }

    private class MapperFieldInfo {
        public final String fieldName;
        public final String fieldType;
        public final String withFieldName;
        public final boolean isPublicField;
        public String originToDestinationParserPackageName;
        public String originToDestinationParserClassName;
        public String destinationToOriginParserPackageName;
        public String destinationToOriginParserClassName;

        public MapperFieldInfo(String fieldName, String fieldType, String withFieldName, boolean isPublicField) {
            this.fieldName = fieldName;
            this.fieldType = fieldType;
            this.withFieldName = withFieldName;
            this.isPublicField = isPublicField;
        }
    }
}
