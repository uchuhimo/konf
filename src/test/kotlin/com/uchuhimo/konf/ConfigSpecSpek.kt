package com.uchuhimo.konf

import com.fasterxml.jackson.databind.type.TypeFactory
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isIn
import com.natpryce.hamkrest.sameInstance
import com.natpryce.hamkrest.throws
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

object ConfigSpecSpek : Spek({
    given("a configSpec") {
        fun testItem(spec: ConfigSpec, item: Item<Int>, description: String) {
            group("for $description") {
                on("add to a configSpec") {
                    it("should be in the spec") {
                        assertThat(item, isIn(spec.items))
                    }
                    it("should have the specified description") {
                        assertThat(item.description, equalTo("description"))
                    }
                    it("should name with prefix") {
                        assertThat(item.name, equalTo("a.b.int"))
                    }
                    it("should have a valid path") {
                        assertThat(item.path, equalTo(listOf("a", "b", "int")))
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
            val item = required<Int>("int", "description")
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
            val item = optional("int", 1, "description")
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
            val item = lazy("int", "description", "placeholder") { 2 }
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
                it("should contain the specified placeholder") {
                    assertThat(specForLazy.item.placeholder, equalTo("placeholder"))
                }
                it("should contain the specified thunk") {
                    assertThat(specForLazy.item.thunk(config), equalTo(2))
                }
            }
        }
    }
})
