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
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName
import java.util.Collections

private const val ALL_FUNCTION_NAME = "all"
private const val ALL_BY_FUNCTION_NAME = "allBy"
private const val TOPICS_TO_EMITTERS_PROPERTY = "topicsToEmitters"

internal fun generateEmittersClass(envData: EnvironmentData): FileSpec {

    var constructorBuilder = FunSpec.constructorBuilder().addModifiers(KModifier.INTERNAL)
    val emitterClassToField = mutableMapOf<String, PropertySpec>()
    val properties = mutableListOf<PropertySpec>()
    val prioritizedType = ParameterizedTypeName.get(
            Prioritized::class.asTypeName(), envData.rootTypeName)

    envData.nodePriorities.toSortedMap().forEach {
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
    val setType = ParameterizedTypeName.get(Set::class.asTypeName(), prioritizedType)

    properties.add(generateTopicsToEmittersProperty(setType))
    val functions = mutableListOf<FunSpec>()

    val returnType = ParameterizedTypeName.get(Collection::class.asTypeName(), prioritizedType)
    functions.add(generateAllMethod(returnType))
    functions.add(generateAllByMethod(returnType))
    functions.addAll(generateGroupMethods(returnType, emitterClassToField, envData.groups))

    return FileSpec.builder(envData.rootPackageName, envData.emittersName)
            .indent("   ")
            .addType(TypeSpec.classBuilder(envData.emittersName)
                    .primaryConstructor(constructorBuilder.build())
                    .addInitializerBlock(generateInitBlock(setType, emitterClassToField,
                            envData.topicsToEmitters))
                    .addProperties(properties)
                    .addFunctions(functions)
                    .build())
            .build()
}

private fun generateTopicsToEmittersProperty(setType: ParameterizedTypeName): PropertySpec {
    val type = ParameterizedTypeName.get(Map::class.asTypeName(), String::class.asTypeName(), setType)
    return PropertySpec
            .builder(TOPICS_TO_EMITTERS_PROPERTY, type)
            .addModifiers(KModifier.PRIVATE)
            .build()
}

private fun generateInitBlock(setType: ParameterizedTypeName, emitterClassToField: Map<String, PropertySpec>,
                              topicsToEmitters: Map<String, Collection<String>>): CodeBlock {

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

private fun generateGroupMethods(emittersType: TypeName, emitterClassToField: Map<String, PropertySpec>,
                                 groups: Map<String, List<String>>): List<FunSpec> {
    val returnList = mutableListOf<FunSpec>()

    groups.forEach { (groupName, emitters) ->
        val appendedEmitters = emitters
                .map { emitterClassToField[it]!!.name }
                .joinToString(",\n", "    setOf(\n", ")", transform = { "        $it" })

        returnList.add(FunSpec.builder("all$groupName")
                .returns(emittersType)
                .addCode(CodeBlock.builder()
                        .addStatement("return %T.unmodifiableSet(\n$appendedEmitters\n)",
                                Collections::class.asTypeName())
                        .build())
                .build())
    }
    return returnList
}