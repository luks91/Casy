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
import com.github.luks91.casy.annotations.SyncGroup
import java.lang.annotation.Inherited

@Inherited
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
@SyncGroup
annotation class UserContent

@Inherited
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
@SyncGroup
annotation class ConfigContent

@UserContent
@SyncEmitter(
        topics = ["sync.notes"],
        triggeredBy = [AccountSynchronizer::class]
)
class NotesSynchronizer: Synchronizable {
    override fun synchronize(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

@UserContent
@SyncEmitter(
        topics = ["sync.photos"],
        triggeredBy = [AccountSynchronizer::class]
)
class PhotosSynchronizer: Synchronizable {
    override fun synchronize(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

@ConfigContent
@SyncEmitter(
        topics = ["sync.global_config"]
)
class GlobalConfigurationSynchronizer: Synchronizable {
    override fun synchronize(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

@ConfigContent
@SyncEmitter(
        topics = ["sync.local_config"],
        triggeredBy = [GlobalConfigurationSynchronizer::class]
)
class LocalConfigurationSynchronizer: Synchronizable {
    override fun synchronize(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

