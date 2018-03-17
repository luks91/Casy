# Casy (CAtegorized SYnchronization)
A library that shields users from complex dependency management when application data is obtained from multiple backend endpoints that have various relationships between each other.

[![Maven Central](https://img.shields.io/maven-central/v/com.github.luks91/casy.svg?style=flat)](https://mvnrepository.com/artifact/com.github.luks91)
[![Travis](https://travis-ci.org/luks91/Casy.svg?branch=master)](https://travis-ci.org/luks91/Casy)
[![codecov.io](http://codecov.io/github/luks91/Casy/coverage.svg?branch=master)](https://codecov.io/gh/luks91/Casy/branch/master)


## How does it help?
Many modern server architectures expose multiple endpoints to synchronize data for clients. Let's take an example of Bitbucket pull requests, their details and users assigned to them. These would be represented by 3 server endpoints:
- [GET] Pull Requests
- [GET] Users for given IDs
- [GET] Pull Request Details for given IDs

Before we even attempt to synchronize users and details endpoints, we have to acquire the pull requests first. Once we retrieve a list of data from the server, we can extract the needed IDs and query the server for additional data. What's important here is that the following conditions take place:
- Users and Pull Requests Details endpoints are always synchronized after Pull Requests.
- Each time we synchronize Pull Requests, we should query the other endpoints as the data may have changed (e.g. new user had been assigned to a pull request).
- Once pull requests data is retrieved, the order in which we synchronize Users and Pull Request Details does not matter. In fact these can be synchronized in parallel.

3 endpoints sound pretty easy but it's never a case for enterprise applications. We are usually dealing with over 30 endpoints the data is coming from. Building and managing dependencies between them turns into a spaghetti code very quick.

This is where Casy helps. The library assumptions are that:
- Each endpoint is synchronized separately in a dedicated class. We will call these emitters going forward.
- There is a common interface defined for all the emitters.
- Each emitter specifies its dependencies with other emitters (triggeredBy and syncsAfter).
- Emitters can be groupped into topics (reflecting push topics). When we obtain an emitter for a given topic, we are also going to receive all the other emitters that are triggered by the emitter, recursively.
- Emitters can be groupped into custom sets.
- The library generates a class that shields user from all the complex dependencies, allowing easy lookup of the emitters.

## Getting started

First include Casy to your project.
```groovy
apply plugin: 'kotlin-kapt'

android {
    //...
    sourceSets {
        debug.java.srcDirs += 'build/generated/source/kaptKotlin/debug'
        release.java.srcDirs += 'build/generated/source/kaptKotlin/release'
    }
}

kapt 'com.github.luks91:casy:1.2.1'
implementation 'com.github.luks91:casy-annotations:1.2.1'
```

Then define common interface/class for all your emitter classes. Note that it's totally up to you how the interface will look like and what operations will it expose. That could be e.g. [Retrofit](http://square.github.io/retrofit/) interface.
```kotlin
import com.github.luks91.casy.annotations.SyncRoot

@SyncRoot(
        allEmittersTopic = "all",
        allNonPushEmittersTopic = "all_non_push"
)
interface Synchronizable {
    fun synchronize(): String
}
```

The next step is to implement your emitters classes and specify relationships between them. Note that they all must implement/extend the entity annotated as the @SyncRoot. The ```allEmittersTopic``` and ```allNonPushEmittersTopic``` parameters are optional. Casy will use them to simulate additional topics that once passed to the ```allBy``` generated method, will return all the emitters and all the emitters that have no topics defined accoringly.
```kotlin
import com.github.luks91.casy.annotations.SyncEmitter

@SyncEmitter(topics = ["sync.account"] )
class AccountSynchronizer: Synchronizable {
    override fun synchronize(): String = "Accounts"
}

@SyncEmitter(
        topics = ["sync.colors", "sync.drawables"],
        syncsAfter = [AccountSynchronizer::class]
)
class ColorsSynchronizer: Synchronizable {
    override fun synchronize(): String = "Colors"
}

@SyncEmitter(
        topics = ["sync.shadows", "sync.drawables"],
        syncsAfter = [AccountSynchronizer::class]
)
class ShadowsSynchronizer : Synchronizable {
    override fun synchronize(): String = "Shadows"
}

@SyncEmitter(
        topics = ["sync.shape", "sync.drawables"],
        syncsAfter = [AccountSynchronizer::class]
)
class ShapesSynchronizer: Synchronizable {
    override fun synchronize(): String = "Shapes"
}

@SyncEmitter(
        syncsAfter = [ColorsSynchronizer::class, ShadowsSynchronizer::class,
            ShapesSynchronizer::class],
        triggeredBy = [AccountSynchronizer::class]
)
class ObjectsSynchronizer: Synchronizable {
    override fun synchronize(): String = "Objects"
}
```

All the SyncEmitter parameters are optional.
Once your project is built, Casy annotation processor will read all the information and generate a class in @SyncRoot entity's package. 
```kotlin
import com.github.luks91.casy.annotations.Prioritized
import java.util.Collections
import kotlin.String
import kotlin.collections.Collection
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.Set

class Synchronizables internal constructor(
      emitterAccountSynchronizer: AccountSynchronizer,
      emitterShadowsSynchronizer: ShadowsSynchronizer,
      emitterColorsSynchronizer: ColorsSynchronizer,
      emitterShapesSynchronizer: ShapesSynchronizer,
      emitterObjectsSynchronizer: ObjectsSynchronizer
) {
   private val prioritizedAccountSynchronizer: Prioritized<Synchronizable> = 
         Prioritized<Synchronizable>(emitterAccountSynchronizer, 1)

   private val prioritizedShadowsSynchronizer: Prioritized<Synchronizable> = 
         Prioritized<Synchronizable>(emitterShadowsSynchronizer, 2)

   private val prioritizedColorsSynchronizer: Prioritized<Synchronizable> = 
         Prioritized<Synchronizable>(emitterColorsSynchronizer, 2)

   private val prioritizedShapesSynchronizer: Prioritized<Synchronizable> = 
         Prioritized<Synchronizable>(emitterShapesSynchronizer, 2)

   private val prioritizedObjectsSynchronizer: Prioritized<Synchronizable> = 
         Prioritized<Synchronizable>(emitterObjectsSynchronizer, 3)

   private val topicsToEmitters: Map<String, Set<Prioritized<Synchronizable>>>

   init {
      val tempMap = mutableMapOf<String, Set<Prioritized<Synchronizable>>>()
      tempMap.put("sync.account", setOf(prioritizedAccountSynchronizer,
            prioritizedObjectsSynchronizer))
      tempMap.put("sync.shadows", setOf(prioritizedShadowsSynchronizer))
      tempMap.put("sync.drawables", setOf(prioritizedShadowsSynchronizer,
            prioritizedShapesSynchronizer,
            prioritizedColorsSynchronizer))
      tempMap.put("sync.shape", setOf(prioritizedShapesSynchronizer))
      tempMap.put("sync.colors", setOf(prioritizedColorsSynchronizer))
      tempMap.put("all", setOf(prioritizedAccountSynchronizer,
            prioritizedShadowsSynchronizer,
            prioritizedObjectsSynchronizer,
            prioritizedShapesSynchronizer,
            prioritizedColorsSynchronizer))
      tempMap.put("all_non_push", setOf(prioritizedObjectsSynchronizer))
      topicsToEmitters = tempMap
   }

   fun all(): Collection<Prioritized<Synchronizable>> = Collections.unmodifiableList(topicsToEmitters.values.flatMap { it }.distinct())

   fun allBy(topics: List<String>): Collection<Prioritized<Synchronizable>> {
      if (topics.isEmpty()) {
         return all()
      } else {
         return Collections.unmodifiableList(topics.flatMap { topicsToEmitters[it] ?: setOf() }.distinct())
      }
   }
}

```

Note the Prioritized class here. It wraps both the emitters and its priority (category). The lower the priority the sooner the endpoint should be synchronized. Also no emitters with priority N can be synchronized until all the considered emitters with priority less that N have completed synchronizing. Emitters with the same priority can be synchronized in parallel with any framework / custom synchronization mechanim of your choice. You can also use any sort of dependency injection framework - like [Dagger](https://google.github.io/dagger/) - to instantiate all the emitters classes and use them to create instance of the generated class.

## Generated class methods overview
- ```all()``` - returns all the emitters classes wrapped in Prioritized objects.
- ```allBy(topics: List<String>)``` - given a list of topics, returns all the emitters that have defined the topic. In addition, it appends recursively all the emitters that had them annotated with triggeredBy.
- ```allNonTopic()``` - returns a list of emitters that have no topics defined.

## Specifying custom emitters groups

In addition to generated methods, Casy allows users to specify custom methods that will return a specific set of emitters.
In order to do so, the @SyncGroup annotation should be used - see the example below.
```kotlin
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

@SyncEmitter(topics = ["sync.account"] )
class AccountSynchronizer: Synchronizable {
    override fun synchronize(): String = "Accounts"
}

@UserContent
@SyncEmitter(
        topics = ["sync.notes"],
        triggeredBy = [AccountSynchronizer::class]
)
class NotesSynchronizer: Synchronizable {
    override fun synchronize(): String = "Notes"
}

@UserContent
@SyncEmitter(
        topics = ["sync.photos"],
        triggeredBy = [AccountSynchronizer::class]
)
class PhotosSynchronizer: Synchronizable {
    override fun synchronize(): String = "Photos"
}

@ConfigContent
@SyncEmitter(
        topics = ["sync.global_config"]
)
class GlobalConfigurationSynchronizer: Synchronizable {
    override fun synchronize(): String = "GlobalConfiguration"
}

@ConfigContent
@SyncEmitter(
        topics = ["sync.local_config"],
        triggeredBy = [GlobalConfigurationSynchronizer::class]
)
class LocalConfigurationSynchronizer: Synchronizable {
    override fun synchronize(): String = "LocalConfiguration"
}
```

The generated class will look the following:
```kotlin
import com.github.luks91.casy.annotations.Prioritized
import java.util.Collections
import kotlin.String
import kotlin.collections.Collection
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.Set

class Synchronizables internal constructor(
      emitterAccountSynchronizer: AccountSynchronizer,
      emitterNotesSynchronizer: NotesSynchronizer,
      emitterGlobalConfigurationSynchronizer: GlobalConfigurationSynchronizer,
      emitterLocalConfigurationSynchronizer: LocalConfigurationSynchronizer,
      emitterPhotosSynchronizer: PhotosSynchronizer
) {
   private val prioritizedAccountSynchronizer: Prioritized<Synchronizable> = 
         Prioritized<Synchronizable>(emitterAccountSynchronizer, 1)

   private val prioritizedNotesSynchronizer: Prioritized<Synchronizable> = 
         Prioritized<Synchronizable>(emitterNotesSynchronizer, 2)

   private val prioritizedGlobalConfigurationSynchronizer: Prioritized<Synchronizable> = 
         Prioritized<Synchronizable>(emitterGlobalConfigurationSynchronizer, 1)

   private val prioritizedLocalConfigurationSynchronizer: Prioritized<Synchronizable> = 
         Prioritized<Synchronizable>(emitterLocalConfigurationSynchronizer, 2)

   private val prioritizedPhotosSynchronizer: Prioritized<Synchronizable> = 
         Prioritized<Synchronizable>(emitterPhotosSynchronizer, 2)

   private val topicsToEmitters: Map<String, Set<Prioritized<Synchronizable>>>

   init {
      val tempMap = mutableMapOf<String, Set<Prioritized<Synchronizable>>>()
      tempMap.put("sync.account", setOf(prioritizedAccountSynchronizer,
            prioritizedNotesSynchronizer,
            prioritizedPhotosSynchronizer))
      tempMap.put("sync.notes", setOf(prioritizedNotesSynchronizer))
      tempMap.put("sync.global_config", setOf(prioritizedGlobalConfigurationSynchronizer,
            prioritizedLocalConfigurationSynchronizer))
      tempMap.put("sync.local_config", setOf(prioritizedLocalConfigurationSynchronizer))
      tempMap.put("sync.photos", setOf(prioritizedPhotosSynchronizer))
      tempMap.put("all", setOf(prioritizedAccountSynchronizer,
            prioritizedNotesSynchronizer,
            prioritizedGlobalConfigurationSynchronizer,
            prioritizedLocalConfigurationSynchronizer,
            prioritizedPhotosSynchronizer))
      tempMap.put("all_non_push", setOf())
      topicsToEmitters = tempMap
   }

   fun all(): Collection<Prioritized<Synchronizable>> = Collections.unmodifiableList(topicsToEmitters.values.flatMap { it }.distinct())

   fun allBy(topics: List<String>): Collection<Prioritized<Synchronizable>> {
      if (topics.isEmpty()) {
         return all()
      } else {
         return Collections.unmodifiableList(topics.flatMap { topicsToEmitters[it] ?: setOf() }.distinct())
      }
   }

   fun allUserContent(): Collection<Prioritized<Synchronizable>> = Collections.unmodifiableList(
       setOf(
           prioritizedNotesSynchronizer,
           prioritizedPhotosSynchronizer)
   )

   fun allConfigContent(): Collection<Prioritized<Synchronizable>> = Collections.unmodifiableList(
       setOf(
           prioritizedGlobalConfigurationSynchronizer,
           prioritizedLocalConfigurationSynchronizer)
   )
}
```

## LICENSE

    Copyright (c) 2018-present, Casy Contributors.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
