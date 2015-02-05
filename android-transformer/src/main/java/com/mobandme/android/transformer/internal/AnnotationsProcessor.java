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

import com.mobandme.android.transformer.Mappable;
import com.mobandme.android.transformer.Mapped;
import com.mobandme.android.transformer.parser.AbstractParser;

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
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedAnnotationTypes({
        "com.mobandme.android.transformer.Mapping",
        "com.mobandme.android.transformer.Mappable"
})
public class AnnotationsProcessor extends AbstractProcessor {

    RoundEnvironment roundEnvironment;
    Map<String, MapperInfo> mappersList;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        roundEnvironment = roundEnv;
        mappersList = new HashMap<>();

        processMappableAnnotationElements();

        processMappedAnnotationElements();

        buildMapperObjects();

        buildTransformerJavaFile();

        return true;
    }

    private void buildTransformerJavaFile() {
        try {

            if (mappersList.size() > 0) {

                String className = Tools.TRANSFORMER_CLASS_NAME;

                writeTrace(String.format("Generating source file for Transformer class with name %s.%s", Tools.TRANSFORMER_PACKAGE, className));

                JavaFileObject javaFileObject = processingEnv.getFiler().createSourceFile(className);
                BufferedWriter buffer = new BufferedWriter(javaFileObject.openWriter());

                buffer.append(String.format(Tools.PACKAGE_PATTERN, Tools.TRANSFORMER_PACKAGE));
                buffer.newLine();

                //region "Class Imports Generation"

                buffer.newLine();
                buffer.append(String.format(Tools.IMPORT_PATTERN, "com.mobandme.android.transformer.internal", "AbstractTransformer"));
                for (MapperInfo mapper : mappersList.values()) {

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
                for (MapperInfo mapper : this.mappersList.values()) {
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

        } catch (IOException error) {
            throw new RuntimeException(error);
        }
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
                String parseWithClassName = mapperField.parseWithClassName;
                if (!parseWithClassName.equals(AbstractParser.class.getSimpleName()))
                    mapperImports.add(String.format(Tools.IMPORT_PATTERN, mapperField.parseWithPackageName, mapperField.parseWithClassName));
                else
                    parseWithClassName = null;

                String originFieldName = mapperField.fieldName;
                String destinationFieldName = mapperField.fieldName;

                if (mapperField.withFieldName != null && !mapperField.withFieldName.trim().equals(""))
                    destinationFieldName = mapperField.withFieldName;

                if (parseWithClassName == null) {
                    MapperInfo mapperInfo = mapperForMapperField(mapperField);
                    if (mapperInfo != null) {
                        mapperImports.add(String.format(Tools.IMPORT_PATTERN, mapperInfo.mapperPackageName, mapperInfo.mapperClassName));
                        classVars.add(String.format(Tools.MAPPER_CLASS_VAR_CONSTANT_PATTERN, mapperInfo.mapperClassName, toCamelCase(mapperInfo.mapperClassName), mapperInfo.mapperClassName));
                        directFields.add(String.format(Tools.MAPPER_FIELD_COMPOSITE_PATTERN, destinationFieldName, toCamelCase(mapperInfo.mapperClassName), originFieldName));
                        inverseFields.add(String.format(Tools.MAPPER_FIELD_COMPOSITE_PATTERN, originFieldName, toCamelCase(mapperInfo.mapperClassName), destinationFieldName));
                    } else {
                        directFields.add(String.format(Tools.MAPPER_FIELD_PATTERN, destinationFieldName, originFieldName));
                        inverseFields.add(String.format(Tools.MAPPER_FIELD_PATTERN, originFieldName, destinationFieldName));
                    }
                } else {

                    String directParserTypeName = String.format("result.%s.getClass()", destinationFieldName);
                    String inverserParserTypeName = String.format("result.%s.getClass()", originFieldName);

                    directFields.add(String.format(Tools.MAPPER_FIELD__WITH_PARSER_PATTERN, destinationFieldName, parseWithClassName, directParserTypeName, originFieldName));
                    inverseFields.add(String.format(Tools.MAPPER_FIELD__WITH_PARSER_PATTERN, originFieldName, parseWithClassName, inverserParserTypeName, destinationFieldName));
                }
            }

            generateMapperJavaFile(mapper, classVars, mapperImports, directFields, inverseFields);
        }
    }

    private MapperInfo mapperForMapperField(MapperFieldInfo mapperField) {
        for (MapperInfo mapperInfo : mappersList.values()) {
            if (mapperField.fieldType.equals(mapperInfo.mappableClassName)) {
                return mapperInfo;
            }
        }
        return null;
    }

    private String toCamelCase(String className) {
        return className.substring(0, 1).toLowerCase() + className.substring(1);
    }

    private void generateMapperJavaFile(MapperInfo mapper, Collection<String> classVars, Collection<String> imports, Collection<String> directFields, Collection<String> inverseFields) {

        try {

            writeTrace(String.format("Generating source file for Mapper with name %s.%s", mapper.mapperPackageName, mapper.mapperClassName));

            JavaFileObject javaFileObject = processingEnv.getFiler().createSourceFile(mapper.mapperClassName);
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

    private void processMappableAnnotationElements() {
        for (Element mappableElement : roundEnvironment.getElementsAnnotatedWith(Mappable.class)) {
            if (mappableElement.getKind() == ElementKind.CLASS) {

                AnnotationMirror annotationMirror = getAnnotationMirror(mappableElement, Mappable.class);
                AnnotationValue  annotationValue = getAnnotationValue(annotationMirror, "with");
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

                //region "Reading custom parsers configuration"

                AnnotationMirror annotationMirror = getAnnotationMirror(mappedElement, Mapped.class);
                AnnotationValue  annotationValue = getAnnotationValue(annotationMirror, "parseWith");
                TypeElement paserWithElement = getTypeElement(annotationValue);
                ClassInfo parseWithClassInfo = new ClassInfo(AbstractTransformer.class.getPackage().getName(), AbstractParser.class.getSimpleName());
                if (paserWithElement != null)
                    parseWithClassInfo = extractClassInformation(paserWithElement);

                //endregion

                String fieldName = mappedElement.getSimpleName().toString();
                String toFieldName = mappedAnnotation.toField();

                MapperFieldInfo mappingFieldInfo = new MapperFieldInfo(fieldName, mappedElement.asType().toString(), toFieldName, parseWithClassInfo.packageName, parseWithClassInfo.className);

                ClassInfo classInfo = extractClassInformationFromField(mappedElement);
                getMapper(classInfo)
                        .getFields()
                        .add(mappingFieldInfo);
            }
        }
    }

    private boolean haveMapper(ClassInfo classInfo) {
        String mapperClassFullName = classInfo.getFullName();
        boolean result = mappersList.containsKey(mapperClassFullName);
        return result;
    }

    private MapperInfo createMapper(String mappableClassName, ClassInfo classInfo, ClassInfo linkedClassInfo) {
        MapperInfo mapper = new MapperInfo(mappableClassName, classInfo.packageName, classInfo.className, linkedClassInfo.packageName, linkedClassInfo.className);
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
        public final String mapperClassName;
        public final String mapperPackageName;
        public final String linkedClassName;
        public final String linkedPackageName;
        public final String mappableClassName;

        private List<MapperFieldInfo> mappingsList = new ArrayList<>();

        public List<MapperFieldInfo> getFields() { return mappingsList; }

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
        public final String parseWithPackageName;
        public final String parseWithClassName;

        public MapperFieldInfo(String fieldName, String fieldType, String withFieldName, String parseWithPackageName, String parseWithClassName) {
            this.fieldName = fieldName;
            this.fieldType = fieldType;
            this.withFieldName = withFieldName;
            this.parseWithPackageName = parseWithPackageName;
            this.parseWithClassName = parseWithClassName;
        }
    }
}
