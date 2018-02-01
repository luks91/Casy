/**
 * Copyright (c) 2018-present, Casy Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package com.github.luks91.casy

import com.github.luks91.casy.annotations.Prioritized
import com.github.luks91.casy.annotations.SyncEmitter
import com.github.luks91.casy.annotations.SyncGroup
import com.squareup.kotlinpoet.*
import java.io.File
import java.util.Collections
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

private const val ALL_FUNCTION_NAME = "all"
private const val ALL_BY_FUNCTION_NAME = "allBy"
private const val ALL_NON_TOPIC_FUNCTION_NAME = "allNonTopic"
private const val TOPICS_TO_EMITTERS_PROPERTY = "topicsToEmitters"

internal fun generateEmittersClass(roundEnv: RoundEnvironment, processingEnv: ProcessingEnvironment,
                                   envData: EnvironmentData) {

    var constructorBuilder = FunSpec.constructorBuilder().addModifiers(KModifier.INTERNAL)
    val emitterClassToField = mutableMapOf<String, PropertySpec>()
    val properties = mutableListOf<PropertySpec>()
    val rootElement = envData.rootData.element
    val rootClass = rootElement.asType().asTypeName()
    val prioritizedType = ParameterizedTypeName.get(Prioritized::class.asTypeName(), rootClass)

    envData.nodePriorities.forEach {
        val emitterFullName = it.key
        val className = ClassName.bestGuess(emitterFullName)
        val constructorParameterName = "emitter${className.simpleName()}"
        constructorBuilder = constructorBuilder.addParameter(constructorParameterName, className)

        val emitterProperty = PropertySpec
                .builder("prioritized${className.simpleName()}", prioritizedType)
                .addModifiers(KModifier.PRIVATE)
                .initializer("\n%T($constructorParameterName, ${it.value})", prioritizedType)
                .build()
        emitterClassToField.put(emitterFullName, emitterProperty)
        properties.add(emitterProperty)
    }

    val packageName = processingEnv.elementUtils.getPackageOf(rootElement).toString()
    val className = with(rootElement.simpleName) { "$this${if (endsWith('s')) "es" else "s"}"}

    val setType = ParameterizedTypeName.get(Set::class.asTypeName(), prioritizedType)

    properties.add(generateTopicsToEmittersProperty(setType))
    val functions = mutableListOf<FunSpec>()

    val returnType = ParameterizedTypeName.get(Collection::class.asTypeName(), prioritizedType)
    functions.add(generateAllMethod(returnType))
    functions.add(generateAllByMethod(returnType))
    functions.add(generateAllNonTopicMethod(envData.adjacency, emitterClassToField, returnType))
    functions.addAll(generateGroupMethods(returnType, roundEnv, processingEnv.messager, emitterClassToField))

    val file = FileSpec.builder(packageName, className)
            .indent("   ")
            .addType(TypeSpec.classBuilder(className)
                    .primaryConstructor(constructorBuilder.build())
                    .addInitializerBlock(generateInitBlock(setType, emitterClassToField, envData))
                    .addProperties(properties)
                    .addFunctions(functions)
                    .build())
            .build()

    val kaptKotlinGeneratedDir = processingEnv.options[Processor.KAPT_KOTLIN_GENERATED_OPTION_NAME]
    file.writeTo(File(kaptKotlinGeneratedDir))
}

private fun generateTopicsToEmittersProperty(setType: ParameterizedTypeName): PropertySpec {
    val type = ParameterizedTypeName.get(Map::class.asTypeName(), String::class.asTypeName(), setType)
    return PropertySpec
            .builder(TOPICS_TO_EMITTERS_PROPERTY, type)
            .addModifiers(KModifier.PRIVATE)
            .build()
}

private fun generateInitBlock(setType: ParameterizedTypeName, emitterClassToField: Map<String, PropertySpec>,
                              envData: EnvironmentData): CodeBlock {

    val topicsToEmitters = envData.adjacency
            .flatMap { (name, node) ->
                node.topics.flatMap { topic ->
                    listOf(topic to name)
                            .plus((envData.paths[name] ?: listOf()).map { topic to it })
                }
            }
            .groupBy({ it.first }, { it.second })
            .plus((envData.rootData.allEmittersTopic.takeIf { !it.isBlank() } ?: "all") to envData.adjacency.keys)
            .plus((envData.rootData.allNonPushEmittersTopic.takeIf { !it.isBlank() } ?: "allNonTopic")
                    to envData.adjacency.filter { it.value.topics.isEmpty() }.keys
            )

    val tempMap = "tempMap"
    val initializer = CodeBlock.builder()
            .addStatement("val $tempMap = mutableMapOf<%T, %T>()", String::class.asTypeName(), setType)

    topicsToEmitters.forEach { (topic, emitters) ->
        initializer.addStatement("$tempMap.put(\"$topic\", " +
                "${emitters.map { emitterClassToField[it]!!.name }.distinct()
                        .joinToString(",\n", "setOf(", ")")})")
    }

    initializer.addStatement("$TOPICS_TO_EMITTERS_PROPERTY = $tempMap")
    return initializer.build()
}

private fun generateAllMethod(emittersType: TypeName): FunSpec {
    return FunSpec.builder(ALL_FUNCTION_NAME)
            .returns(emittersType)
            .addStatement("return %T.unmodifiableList($TOPICS_TO_EMITTERS_PROPERTY.values.flatMap { it }.distinct())",
                    Collections::class.asTypeName())
            .build()
}

private fun generateAllByMethod(emittersType: TypeName): FunSpec {
    val topicsName = "topics"
    val topicsListType = ParameterizedTypeName.get(List::class.asTypeName(), String::class.asTypeName())
    return FunSpec.builder(ALL_BY_FUNCTION_NAME)
            .returns(emittersType)
            .addParameter(topicsName, topicsListType)
            .addCode(CodeBlock.builder()
                    .beginControlFlow("if ($topicsName.isEmpty())")
                    .addStatement("return $ALL_FUNCTION_NAME()")
                    .nextControlFlow("else")
                    .addStatement(
                            "return %T.unmodifiableList($topicsName.flatMap { $TOPICS_TO_EMITTERS_PROPERTY[it] ?: setOf() }" +
                                    ".distinct())", Collections::class.asTypeName())
                    .endControlFlow()
                    .build())
            .build()
}

private fun generateAllNonTopicMethod(adjacency: Map<String, Node>, emitterClassToField: Map<String, PropertySpec>,
                                      emittersType: TypeName): FunSpec {

    val emitters = adjacency.filterValues { it.topics.isEmpty() }.map { (key, _) -> emitterClassToField[key]!!.name }
    val emittersStatement = emitters.joinToString(",\n", "    setOf(\n", ")", transform = {"        $it"})
    val methodStatement = if (emitters.isEmpty()) {
        "return setOf()"
    } else {
        "return %T.unmodifiableList(\n$emittersStatement.distinct()\n)"
    }

    return FunSpec.builder(ALL_NON_TOPIC_FUNCTION_NAME)
            .returns(emittersType)
            .addCode(CodeBlock.builder()
                    .addStatement(methodStatement, Collections::class.asTypeName())
                    .build())
            .build()
}

private fun generateGroupMethods(emittersType: TypeName, roundEnv: RoundEnvironment, messager: Messager,
                                 emitterClassToField: Map<String, PropertySpec>): List<FunSpec> {
    val groups = roundEnv.getElementsAnnotatedWith(SyncGroup::class.java)
    val returnList = mutableListOf<FunSpec>()
    groups.forEach { group ->
        val emitters = roundEnv.getElementsAnnotatedWith(group as TypeElement)
                .map {
                    val emitterClazz = it.asType().asTypeName().toString()
                    val emitter = emitterClassToField[emitterClazz]
                    if (emitter == null) {
                        val error = "$emitterClazz is annotated with ${group.simpleName} but not ${SyncEmitter::class.simpleName}"
                        messager.printMessage(Diagnostic.Kind.ERROR, error)
                        throw IllegalStateException(error)
                    }
                    return@map emitter.name
                }
                .joinToString(",\n", "    setOf(\n", ")", transform = {"        $it"})

        returnList.add(FunSpec.builder("all${group.simpleName}")
                .returns(emittersType)
                .addCode(CodeBlock.builder()
                        .addStatement("return %T.unmodifiableList(\n$emitters.distinct()\n)", Collections::class.asTypeName())
                        .build())
                .build())
    }
    return returnList
}