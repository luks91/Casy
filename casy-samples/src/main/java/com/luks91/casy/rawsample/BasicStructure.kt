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

package com.luks91.casy.rawsample

import com.github.luks91.casy.annotations.SyncEmitter

@SyncEmitter(topics = ["sync.account"] )
class AccountSynchronizer: Synchronizable {
    override fun synchronize(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

@SyncEmitter(
        topics = ["sync.colors", "sync.drawables"],
        syncsAfter = [AccountSynchronizer::class]
)
class ColorsSynchronizer: Synchronizable {
    override fun synchronize(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

@SyncEmitter(
        topics = ["sync.shadows", "sync.drawables"],
        syncsAfter = [AccountSynchronizer::class]
)
class ShadowsSynchronizer : Synchronizable {
    override fun synchronize(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

@SyncEmitter(
        topics = ["sync.shape", "sync.drawables"],
        syncsAfter = [AccountSynchronizer::class]
)
class ShapesSynchronizer: Synchronizable {
    override fun synchronize(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

@SyncEmitter(
        syncsAfter = [ColorsSynchronizer::class, ShadowsSynchronizer::class,
            ShapesSynchronizer::class],
        triggeredBy = [AccountSynchronizer::class]
)
class ObjectsSynchronizer: Synchronizable {
    override fun synchronize(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
@SyncEmitter(
        topics = ["object_details"],
        triggeredBy = [ObjectsSynchronizer::class]
)
class ObjectsDetailsSynchronizer: Synchronizable {
    override fun synchronize(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}