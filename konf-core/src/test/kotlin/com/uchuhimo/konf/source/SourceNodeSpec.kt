/*
 * Copyright 2017-2021 the original author or authors.
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

package com.uchuhimo.konf.source

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.uchuhimo.konf.EmptyNode
import com.uchuhimo.konf.TreeNode
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek

object ListSourceNodeSpec : SubjectSpek<ListSourceNode>({
    subject { ListSourceNode(listOf(EmptyNode, EmptyNode)) }
    on("get children") {
        it("should return a map indexed by integer") {
            assertThat(
                subject.children,
                equalTo(
                    mutableMapOf<String, TreeNode>(
                        "0" to EmptyNode,
                        "1" to EmptyNode
                    )
                )
            )
        }
    }
})
