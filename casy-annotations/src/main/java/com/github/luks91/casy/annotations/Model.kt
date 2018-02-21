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

/**
 * Main data class that is returned by all the methods in Casy-generated synchronization class.
 *
 * @param item Specific instance of [SyncEmitter] class represented by this object. Note that
 * the type T will be equivalent to the type annotated with [SyncRoot].
 * @param priority Priority of the [SyncEmitter] calculated during the compilation process.
 * Note that emitters with lower priority should be synchronized always before the higher priorities.
 * See [SyncEmitter] for more details.
 */
data class Prioritized<out T>(
        @JvmField val item: T,
        @JvmField val priority: Long)