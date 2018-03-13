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

package com.uchuhimo.konf

import com.fasterxml.jackson.databind.type.TypeFactory
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.isIn
import com.natpryce.hamkrest.sameInstance
import com.natpryce.hamkrest.throws
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import kotlin.test.assertFalse
import kotlin.test.assertTrue

object ConfigSpecSpek : Spek({
    given("a configSpec") {
        fun testItem(spec: Spec, item: Item<Int>, description: String) {
            group("for $description, as an item") {
                on("add to a configSpec") {
                    it("should be in the spec") {
                        assertThat(item, isIn(spec.items))
                    }
                    it("should have the specified description") {
                        assertThat(item.description, equalTo("description"))
                    }
                    it("should name without prefix") {
                        assertThat(item.name, equalTo("c.int"))
                    }
                    it("should have a valid path") {
                        assertThat(item.path, equalTo(listOf("c", "int")))
                    }
                    it("should point to the spec") {
                        assertThat(item.spec, equalTo(spec))
                    }
                    it("should have specified type") {
                        assertThat(item.type,
                            equalTo(TypeFactory.defaultInstance()
                                .constructType(Int::class.javaObjectType)))
                    }
                }
            }
        }

        val specForRequired = object : ConfigSpec("a.b") {
            val item by required<Int>("c.int", "description")
        }
        testItem(specForRequired, specForRequired.item, "a required item")
        group("for a required item") {
            val spec = specForRequired
            on("add to a configSpec") {
                it("should still be a required item") {
                    assertTrue(spec.item.isRequired)
                    assertFalse(spec.item.isOptional)
                    assertFalse(spec.item.isLazy)
                    assertThat(spec.item.asRequiredItem, sameInstance(spec.item))
                    assertThat({ spec.item.asOptionalItem }, throws<ClassCastException>())
                    assertThat({ spec.item.asLazyItem }, throws<ClassCastException>())
                }
            }
        }
        val specForOptional = object : ConfigSpec("a.b") {
            val item by optional(1, "c.int", "description")
        }
        testItem(specForOptional, specForOptional.item, "an optional item")
        group("for an optional item") {
            val spec = specForOptional
            on("add to a configSpec") {
                it("should still be an optional item") {
                    assertFalse(spec.item.isRequired)
                    assertTrue(spec.item.isOptional)
                    assertFalse(spec.item.isLazy)
                    assertThat({ spec.item.asRequiredItem }, throws<ClassCastException>())
                    assertThat(spec.item.asOptionalItem, sameInstance(spec.item))
                    assertThat({ spec.item.asLazyItem }, throws<ClassCastException>())
                }
                it("should contain the specified default value") {
                    assertThat(spec.item.default, equalTo(1))
                }
            }
        }
        val specForLazy = object : ConfigSpec("a.b") {
            val item by lazy("c.int", "description") { 2 }
        }
        val config = Config { addSpec(specForLazy) }
        testItem(specForLazy, specForLazy.item, "a lazy item")
        group("for a lazy item") {
            val spec = specForLazy
            on("add to a configSpec") {
                it("should still be a lazy item") {
                    assertFalse(spec.item.isRequired)
                    assertFalse(spec.item.isOptional)
                    assertTrue(spec.item.isLazy)
                    assertThat({ spec.item.asRequiredItem }, throws<ClassCastException>())
                    assertThat({ spec.item.asOptionalItem }, throws<ClassCastException>())
                    assertThat(spec.item.asLazyItem, sameInstance(spec.item))
                }
                it("should contain the specified thunk") {
                    assertThat(specForLazy.item.thunk(config), equalTo(2))
                }
            }
        }
        on("add repeated item") {
            val spec = ConfigSpec()
            val item by Spec.dummy.required<Int>()
            spec.addItem(item)
            it("should throw RepeatedItemException") {
                assertThat({ spec.addItem(item) },
                    throws(has(RepeatedItemException::name, equalTo("item"))))
            }
        }
        val spec = object : ConfigSpec("a.bb") {
            @Suppress("unused")
            val item by required<Int>("int", "description")
        }
        group("get operation") {
            on("get an empty path") {
                it("should return itself") {
                    assertThat(spec[""], equalTo<Spec>(spec))
                }
            }
            on("get a valid path") {
                it("should return a config spec with proper prefix") {
                    assertThat(spec["a"].prefix, equalTo("bb"))
                    assertThat(spec["a.bb"].prefix, equalTo(""))
                }
            }
            on("get an invalid path") {
                it("should throw NoSuchPathException") {
                    assertThat({ spec["b"] }, throws(has(NoSuchPathException::path, equalTo("b"))))
                    assertThat({ spec["a."] }, throws<IllegalStateException>())
                    assertThat({ spec["a.b"] }, throws(has(NoSuchPathException::path, equalTo("a.b"))))
                }
            }
        }
        group("prefix operation") {
            on("prefix with an empty path") {
                it("should return itself") {
                    assertThat(Prefix("") + spec, equalTo<Spec>(spec))
                }
            }
            on("prefix with a non-empty path") {
                it("should return a config spec with proper prefix") {
                    assertThat((Prefix("c") + spec).prefix, equalTo("c.a.bb"))
                    assertThat((Prefix("c") + spec["a.bb"]).prefix, equalTo("c"))
                }
            }
        }
        group("plus operation") {
            val spec1 = object : ConfigSpec("a") {
                val item1 by required<Int>()
            }
            val spec2 = object : ConfigSpec("b") {
                val item2 by required<Int>()
            }
            @Suppress("NAME_SHADOWING")
            val spec by memoized { spec1 + spec2 }
            on("add a valid item") {
                it("should contains the item in the facade spec") {
                    val item by Spec.dummy.required<Int>()
                    spec.addItem(item)
                    assertThat(item, isIn(spec.items))
                    assertThat(item, isIn(spec2.items))
                }
            }
            on("add a repeated item") {
                it("should throw RepeatedItemException") {
                    assertThat({ spec.addItem(spec1.item1) },
                        throws(has(RepeatedItemException::name, equalTo("item1"))))
                }
            }
            on("get the list of items") {
                it("should contains all items in both the facade spec and the fallback spec") {
                    assertThat(spec.items, equalTo(spec1.items + spec2.items))
                }
            }
            on("qualify item name") {
                it("should add proper prefix") {
                    assertThat(spec.qualify(spec1.item1), equalTo("a.item1"))
                    assertThat(spec.qualify(spec2.item2), equalTo("b.item2"))
                }
            }
        }
    }
})
