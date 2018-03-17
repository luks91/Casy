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

package com.luks91.casy

import com.github.luks91.casy.annotations.Prioritized
import com.luks91.casy.rawsample.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlin.properties.Delegates

class SynchronizablesTest {

    private var testedSynchronizables by Delegates.notNull<Synchronizables>()
    private var emitterAccountSynchronizer by Delegates.notNull<AccountSynchronizer>()
    private var emitterColorsSynchronizer by Delegates.notNull<ColorsSynchronizer>()
    private var emitterGlobalConfigurationSynchronizer by Delegates.notNull<GlobalConfigurationSynchronizer>()
    private var emitterLocalConfigurationSynchronizer by Delegates.notNull<LocalConfigurationSynchronizer>()
    private var emitterNotesSynchronizer by Delegates.notNull<NotesSynchronizer>()
    private var emitterObjectsDetailsSynchronizer by Delegates.notNull<ObjectsDetailsSynchronizer>()
    private var emitterObjectsSynchronizer by Delegates.notNull<ObjectsSynchronizer>()
    private var emitterPhotosSynchronizer by Delegates.notNull<PhotosSynchronizer>()
    private var emitterShadowsSynchronizer by Delegates.notNull<ShadowsSynchronizer>()
    private var emitterShapesSynchronizer by Delegates.notNull<ShapesSynchronizer>()

    private var expectedPrioritizedAccountSynchronizer by Delegates.notNull<Prioritized<AccountSynchronizer>>()
    private var expectedPrioritizedColorsSynchronizer by Delegates.notNull<Prioritized<ColorsSynchronizer>>()
    private var expectedPrioritizedGlobalConfigurationSynchronizer by Delegates.notNull<Prioritized<GlobalConfigurationSynchronizer>>()
    private var expectedPrioritizedLocalConfigurationSynchronizer by Delegates.notNull<Prioritized<LocalConfigurationSynchronizer>>()
    private var expectedPrioritizedNotesSynchronizer by Delegates.notNull<Prioritized<NotesSynchronizer>>()
    private var expectedPrioritizedObjectsDetailsSynchronizer by Delegates.notNull<Prioritized<ObjectsDetailsSynchronizer>>()
    private var expectedPrioritizedObjectsSynchronizer by Delegates.notNull<Prioritized<ObjectsSynchronizer>>()
    private var expectedPrioritizedPhotosSynchronizer by Delegates.notNull<Prioritized<PhotosSynchronizer>>()
    private var expectedPrioritizedShadowsSynchronizer by Delegates.notNull<Prioritized<ShadowsSynchronizer>>()
    private var expectedPrioritizedShapesSynchronizer by Delegates.notNull<Prioritized<ShapesSynchronizer>>()

    @Before
    fun setUp() {
        emitterAccountSynchronizer = AccountSynchronizer()
        emitterColorsSynchronizer = ColorsSynchronizer()
        emitterGlobalConfigurationSynchronizer = GlobalConfigurationSynchronizer()
        emitterLocalConfigurationSynchronizer = LocalConfigurationSynchronizer()
        emitterNotesSynchronizer = NotesSynchronizer()
        emitterObjectsDetailsSynchronizer = ObjectsDetailsSynchronizer()
        emitterObjectsSynchronizer = ObjectsSynchronizer()
        emitterPhotosSynchronizer = PhotosSynchronizer()
        emitterShadowsSynchronizer = ShadowsSynchronizer()
        emitterShapesSynchronizer = ShapesSynchronizer()

        expectedPrioritizedAccountSynchronizer = Prioritized(emitterAccountSynchronizer, 1)
        expectedPrioritizedGlobalConfigurationSynchronizer = Prioritized(emitterGlobalConfigurationSynchronizer, 1)
        expectedPrioritizedLocalConfigurationSynchronizer = Prioritized(emitterLocalConfigurationSynchronizer, 2)
        expectedPrioritizedColorsSynchronizer = Prioritized(emitterColorsSynchronizer, 2)
        expectedPrioritizedNotesSynchronizer = Prioritized(emitterNotesSynchronizer, 2)
        expectedPrioritizedObjectsSynchronizer = Prioritized(emitterObjectsSynchronizer, 4)
        expectedPrioritizedObjectsDetailsSynchronizer = Prioritized(emitterObjectsDetailsSynchronizer, 5)
        expectedPrioritizedPhotosSynchronizer = Prioritized(emitterPhotosSynchronizer, 2)
        expectedPrioritizedShadowsSynchronizer = Prioritized(emitterShadowsSynchronizer, 2)
        expectedPrioritizedShapesSynchronizer = Prioritized(emitterShapesSynchronizer, 3)

        testedSynchronizables = Synchronizables(
                emitterAccountSynchronizer, emitterColorsSynchronizer,
                emitterGlobalConfigurationSynchronizer, emitterLocalConfigurationSynchronizer,
                emitterNotesSynchronizer, emitterObjectsDetailsSynchronizer,
                emitterObjectsSynchronizer, emitterPhotosSynchronizer, emitterShadowsSynchronizer,
                emitterShapesSynchronizer
        )
    }

    @Test
    fun generatedCorrectConfigContentGroup() {
        assertEquals(
                setOf(expectedPrioritizedGlobalConfigurationSynchronizer,
                        expectedPrioritizedLocalConfigurationSynchronizer),
                testedSynchronizables.allConfigContent()
        )
    }

    @Test
    fun generatedCorrectUserContentGroup() {
        assertEquals(
                setOf(expectedPrioritizedNotesSynchronizer, expectedPrioritizedPhotosSynchronizer),
                testedSynchronizables.allUserContent()
        )
    }

    @Test
    fun returnsAllEmitters() {
        assertEquals(10, testedSynchronizables.all().size)
        assertEquals(
                setOf(
                         expectedPrioritizedAccountSynchronizer,
                         expectedPrioritizedColorsSynchronizer,
                         expectedPrioritizedGlobalConfigurationSynchronizer,
                         expectedPrioritizedLocalConfigurationSynchronizer,
                         expectedPrioritizedNotesSynchronizer,
                         expectedPrioritizedObjectsDetailsSynchronizer,
                         expectedPrioritizedObjectsSynchronizer,
                         expectedPrioritizedPhotosSynchronizer,
                         expectedPrioritizedShadowsSynchronizer,
                         expectedPrioritizedShapesSynchronizer
                ),
                testedSynchronizables.all().toSet()
        )
    }

    @Test
    fun returnsCorrectEmittersForWorkTopics() {
        assertEquals(
                setOf(
                        expectedPrioritizedAccountSynchronizer,
                        expectedPrioritizedNotesSynchronizer,
                        expectedPrioritizedObjectsDetailsSynchronizer,
                        expectedPrioritizedObjectsSynchronizer,
                        expectedPrioritizedPhotosSynchronizer
                ),
                testedSynchronizables.allBy(listOf("sync.account")).toSet()
        )
        assertEquals(
                setOf(expectedPrioritizedNotesSynchronizer),
                testedSynchronizables.allBy(listOf("sync.notes")).toSet()
        )
        assertEquals(
                setOf(
                        expectedPrioritizedGlobalConfigurationSynchronizer,
                        expectedPrioritizedLocalConfigurationSynchronizer
                ),
                testedSynchronizables.allBy(listOf("sync.global_config")).toSet()
        )
        assertEquals(
                setOf(expectedPrioritizedShadowsSynchronizer),
                testedSynchronizables.allBy(listOf("sync.shadows")).toSet()
        )
        assertEquals(
                setOf(
                        expectedPrioritizedShadowsSynchronizer,
                        expectedPrioritizedShapesSynchronizer,
                        expectedPrioritizedColorsSynchronizer
                ),
                testedSynchronizables.allBy(listOf("sync.drawables")).toSet()
        )
        assertEquals(
                setOf(expectedPrioritizedLocalConfigurationSynchronizer),
                testedSynchronizables.allBy(listOf("sync.local_config")).toSet()
        )
        assertEquals(
                setOf(expectedPrioritizedObjectsDetailsSynchronizer),
                testedSynchronizables.allBy(listOf("object_details")).toSet()
        )
        assertEquals(
                setOf(expectedPrioritizedShapesSynchronizer),
                testedSynchronizables.allBy(listOf("sync.shape")).toSet()
        )
        assertEquals(
                setOf(expectedPrioritizedPhotosSynchronizer),
                testedSynchronizables.allBy(listOf("sync.photos")).toSet()
        )
        assertEquals(
                setOf(expectedPrioritizedColorsSynchronizer),
                testedSynchronizables.allBy(listOf("sync.colors")).toSet()
        )

    }

    @Test
    fun returnsCorrectlyAllEmittersTopic() {
        assertEquals(10, testedSynchronizables.allBy(listOf("all")).size)
        assertEquals(
                setOf(
                        expectedPrioritizedAccountSynchronizer,
                        expectedPrioritizedColorsSynchronizer,
                        expectedPrioritizedGlobalConfigurationSynchronizer,
                        expectedPrioritizedLocalConfigurationSynchronizer,
                        expectedPrioritizedNotesSynchronizer,
                        expectedPrioritizedObjectsDetailsSynchronizer,
                        expectedPrioritizedObjectsSynchronizer,
                        expectedPrioritizedPhotosSynchronizer,
                        expectedPrioritizedShadowsSynchronizer,
                        expectedPrioritizedShapesSynchronizer
                ),
                testedSynchronizables.allBy(listOf("all")).toSet()
        )
    }

    @Test
    fun returnsCorrectlyNonTopicEmitters() {
        assertEquals(
                listOf(
                        expectedPrioritizedObjectsDetailsSynchronizer,
                        expectedPrioritizedObjectsSynchronizer
                ),
                testedSynchronizables.allBy(listOf("all_non_push"))
        )
    }
}