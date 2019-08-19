/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.uchuhimo.konf.source.base

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.Feature
import com.uchuhimo.konf.source.ConfigForLoad
import com.uchuhimo.konf.source.SourceLoadSpec
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek
import org.jetbrains.spek.subject.itBehavesLike
import java.util.Arrays
import kotlin.test.assertTrue

object FlatSourceLoadSpec : SubjectSpek<Config>({

    subject {
        Config {
            addSpec(ConfigForLoad)
            addSpec(FlatConfigForLoad)
            enable(Feature.FAIL_ON_UNKNOWN_PATH)
        }.from.map.flat(loadContent)
    }

    itBehavesLike(SourceLoadSpec)

    given("a flat source") {
        on("load the source into config") {
            it("should contain every value specified in the source") {
                val classForLoad = ClassForLoad(
                    stringWithComma = "string,with,comma",
                    emptyList = listOf(),
                    emptySet = setOf(),
                    emptyArray = intArrayOf(),
                    emptyObjectArray = arrayOf(),
                    singleElementList = listOf(1),
                    multipleElementsList = listOf(1, 2)
                )
                assertThat(subject[FlatConfigForLoad.emptyList], equalTo(listOf()))
                assertThat(subject[FlatConfigForLoad.emptySet], equalTo(setOf()))
                assertTrue(Arrays.equals(subject[FlatConfigForLoad.emptyArray], intArrayOf()))
                assertTrue(Arrays.equals(subject[FlatConfigForLoad.emptyObjectArray], arrayOf()))
                assertThat(subject[FlatConfigForLoad.singleElementList], equalTo(listOf(1)))
                assertThat(subject[FlatConfigForLoad.multipleElementsList], equalTo(listOf(1, 2)))
                assertThat(subject[FlatConfigForLoad.flatClass].stringWithComma,
                    equalTo(classForLoad.stringWithComma))
            }
        }
    }
})

object FlatSourceReloadSpec : SubjectSpek<Config>({

    subject {
        val config = Config {
            addSpec(ConfigForLoad)
            addSpec(FlatConfigForLoad)
        }.from.map.flat(loadContent)
        Config {
            addSpec(ConfigForLoad)
            addSpec(FlatConfigForLoad)
        }.from.map.flat(config.toFlatMap())
    }

    itBehavesLike(FlatSourceLoadSpec)
})

private val loadContent = mapOf(
    "empty" to "null",
    "literalEmpty" to "null",
    "present" to "1",
    "boolean" to "false",
    "int" to "1",
    "short" to "2",
    "byte" to "3",
    "bigInteger" to "4",
    "long" to "4",
    "double" to "1.5",
    "float" to "-1.5",
    "bigDecimal" to "1.5",
    "char" to "a",
    "string" to "string",
    "offsetTime" to "10:15:30+01:00",
    "offsetDateTime" to "2007-12-03T10:15:30+01:00",
    "zonedDateTime" to "2007-12-03T10:15:30+01:00[Europe/Paris]",
    "localDate" to "2007-12-03",
    "localTime" to "10:15:30",
    "localDateTime" to "2007-12-03T10:15:30",
    "date" to "2007-12-03T10:15:30Z",
    "year" to "2007",
    "yearMonth" to "2007-12",
    "instant" to "2007-12-03T10:15:30.00Z",
    "duration" to "P2DT3H4M",
    "simpleDuration" to "200millis",
    "size" to "10k",
    "enum" to "LABEL2",
    "list" to "1,2,3",
    "mutableList" to "1,2,3",
    "listOfList.0" to "1,2",
    "listOfList.1" to "3,4",
    "set" to "1,2,1",
    "sortedSet" to "2,1,1,3",
    "map.a" to "1",
    "map.b" to "2",
    "map.c" to "3",
    "sortedMap.c" to "3",
    "sortedMap.b" to "2",
    "sortedMap.a" to "1",
    "nested.0.0.0.a" to "1",
    "listOfMap.0.a" to "1",
    "listOfMap.0.b" to "2",
    "listOfMap.1.a" to "3",
    "listOfMap.1.b" to "4",
    "array.boolean" to "true,false",
    "array.byte" to "1,2,3",
    "array.short" to "1,2,3",
    "array.int" to "1,2,3",
    "array.long" to "4,5,6",
    "array.float" to "-1, 0.0, 1",
    "array.double" to "-1, 0.0, 1",
    "array.char" to "a,b,c",
    "array.object.boolean" to "true,false",
    "array.object.int" to "1,2,3",
    "array.object.string" to "one,two,three",
    "array.object.enum" to "LABEL1,LABEL2,LABEL3",
    "pair.first" to "1",
    "pair.second" to "2",
    "clazz.empty" to "null",
    "clazz.literalEmpty" to "null",
    "clazz.present" to "1",
    "clazz.boolean" to "false",
    "clazz.int" to "1",
    "clazz.short" to "2",
    "clazz.byte" to "3",
    "clazz.bigInteger" to "4",
    "clazz.long" to "4",
    "clazz.double" to "1.5",
    "clazz.float" to "-1.5",
    "clazz.bigDecimal" to "1.5",
    "clazz.char" to "a",
    "clazz.string" to "string",
    "clazz.offsetTime" to "10:15:30+01:00",
    "clazz.offsetDateTime" to "2007-12-03T10:15:30+01:00",
    "clazz.zonedDateTime" to "2007-12-03T10:15:30+01:00[Europe/Paris]",
    "clazz.localDate" to "2007-12-03",
    "clazz.localTime" to "10:15:30",
    "clazz.localDateTime" to "2007-12-03T10:15:30",
    "clazz.date" to "2007-12-03T10:15:30Z",
    "clazz.year" to "2007",
    "clazz.yearMonth" to "2007-12",
    "clazz.instant" to "2007-12-03T10:15:30.00Z",
    "clazz.duration" to "P2DT3H4M",
    "clazz.simpleDuration" to "200millis",
    "clazz.size" to "10k",
    "clazz.enum" to "LABEL2",
    "clazz.booleanArray" to "true,false",
    "clazz.nested.0.0.0.a" to "1",
    "emptyList" to "",
    "emptySet" to "",
    "emptyArray" to "",
    "emptyObjectArray" to "",
    "singleElementList" to "1",
    "multipleElementsList" to "1,2",
    "flatClass.stringWithComma" to "string,with,comma",
    "flatClass.emptyList" to "",
    "flatClass.emptySet" to "",
    "flatClass.emptyArray" to "",
    "flatClass.emptyObjectArray" to "",
    "flatClass.singleElementList" to "1",
    "flatClass.multipleElementsList" to "1,2"
).mapKeys { (key, _) -> "level1.level2.$key" }
