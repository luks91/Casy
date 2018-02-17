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

package com.github.luks91.casy.annotations

import java.lang.annotation.Inherited
import kotlin.reflect.KClass

@Inherited
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
/**
 * All the classes that are responsible for synchronizing individual server endpoints should be annotated
 * with this annotation. Note that all the {@link SyncEmitter}s must implement/extend common root
 * annotated by {@link SyncRoot}. Please see the {@link SyncRoot} for more details. Upon compilation,
 * the annotations processor embedded in Casy will aggregate all the SyncEmitters, and build a dependency
 * tree based on triggeredBy/syncsAfter dependencies provided in each of the emitters. The dependency
 * will be modeled in form of one or more trees, with roots being the {@link SyncEmitter}s that
 * do not syncAfter an emitters and none emitters are triggering them. Then each of the emitters,
 * depending on it's order in hierarchy will be assigned a priority - with 1 being the "top level" priority
 * and subsequent, higher values for lower-priority emitters. The emitter along with its priority
 * information is wrapped into {@link Prioritized} class instance.
 * <br>
 * The framework idea is that upon retrieving a {@link Collection} of {@link Prioritized} {@link SyncEmitter}s,
 * the client should then synchronize them accordingly to the priotiries. Meaning that e.g. emitters
 * with priority 2 can be synchronized only after all the emitters with priority 1 are synchronized.
 * The assumption is also that the client synchronizes {@link SyncEmitter}s belonging to the same priority
 * level in parallel.
 * <br>
 * Output of the annotation processing during compilation is a class allowing the user to retrieve
 * {@link Prioritized} {@link SyncEmitter}s based on various criteria. This includes and is not limited to:
 * list of emitters per topic, all emitters, all non-topic emitters, specific groups of emitters.
 * Please refer to {@link SyncRoot} and {@link SyncGroup} for more details.
 *
 * @param triggeredBy Specifies that the data endpoint annotated should always synchronize once
 * any of the emitters being a part of the array are synchronized. Note that in this case it is also
 * guaranteed that this emitter will have higher priority number than the emitters its triggered by.
 * This means this emitter will be always synchronized after its triggers.
 * An example could be a list of pull requests from BitBucket and looking up for a list of users for
 * each of them. We synchronize the pull requests first, obtaining a list of users assigned to each of them.
 * Then knowing which users we need to obtain, we should query the users endpoint. At any time the
 * pull request - users assignment may change, so the users endpoint query should be always triggered by
 * the pull requests synchronization.
 *
 * @param syncsAfter Specifies that the data endpoint annotated should always be synchronized after
 * the emitters specified in the array provided. This means that this emitter will always have it's
 * {@link Prioritized#priority} higher than the emitters syncs after. Note that the triggeredBy dependency
 * also applies the same rule as syncsAfter. This means that emitters present in triggeredBy do not need
 * to be also added to syncsAfteer.
 *
 * @param topics Specifies a list of topics which should trigger synchronization of this emitter.
 * Upon compilation, the Casy annotation processor generates a class (See {@link SyncRoot} for more),
 * that will expose a "allBy(topics: List<String>): Collection<Prioritized<SyncEmitter>>" method.
 * For a given list of topics, Casy will provide emitters that should be triggered by them.
 * Additionally, emitters that are triggeredBy them (and down recursively) will also be present
 * in the returned collection. Note that there's an exception to this rule for a special topic -
 * allNonPushEmittersTopic specified by {@link SyncRoot}. Please see the class doc for more details.
 */
annotation class SyncEmitter(
        val triggeredBy: Array<KClass<*>> = [],
        val syncsAfter: Array<KClass<*>> = [],
        val topics: Array<String> = []
)

@Inherited
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
/**
 * Annotation used to mark common interface / class for all the specified
 * {@link SyncEmitter}s in the project. The type annotated will be used as a parametrized type
 * of all the collections returned by the generated class methods. Also, name of the annotated type will be
 * used to name the generated class (e.g. root class com.package.Synchronizable will result in the generated
 * generated class being named com.package.Synchronizables). Note that there must be exactly one {@link SyncRoot}
 * class in the project if at least one {@link SyncEmitter} is defined.
 * Presence of more roots will cause a compilation error.
 *
 * @param allEmittersTopic Generated class will provide a method allowing consumers to retrieve all the
 * {@link SyncEmitter} instances. The idea here is that the topics represent actual push topics
 * the data should be synchronized upon. The allEmittersTopic topic, once provided to the generated
 * allBy method will return all the known emitters, wrapped in {@link Prioritized} class,
 * reflecting their sync priority. Priorities are calculated explicitly based
 * on {@link SyncEmitter}'s triggeredBy and syncsAfter priorities. Please see {@link SyncEmitter} for more details.
 * In case no value or an empty string is provided for this parameter, default value of "all" will be used.
 *
 * @param allNonPushEmittersTopic Similarly to allEmittersTopic, this parameter will drive additional
 * topic to retrieve a collection of emitters. The difference is that this topic will return all {@link Prioritized}
 * instances of {@link SyncEmitter} that have no topics specified.
 * In case no value or an empty string is provided for this parameter, default value of "allNonTopic" will be used.
 */
annotation class SyncRoot(
        val allEmittersTopic: String = "all",
        val allNonPushEmittersTopic: String = "allNonTopic"
)

@Inherited
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.ANNOTATION_CLASS)
/**
 * Annotation used to annotate custom Annotation that groups emitters into a custom category.
 * The annotated annotation class' simple name will be used to name a allXXX generated method that will
 * return {@link Prioritized} {@link Collection} of all the {@link SyncEmitter}s annotated with the
 * custom annotation. Note that there is no limitation on how many custom annotations can be created.
 *
 * E.g. emitters that synchronize Bitbucket information could be groupped into PullRequest group.
 * Then to retrieve all the PullRequest related {@link Prioritized} {@link SyncEmitter}s,
 * a method allPullRequest should be used.
 */
annotation class SyncGroup