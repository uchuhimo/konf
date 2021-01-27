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

package com.uchuhimo.konf;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.uchuhimo.konf.source.hocon.HoconProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("test Java API of loader")
class LoaderJavaApiTest {
  private Config config;

  @BeforeEach
  void initConfig() {
    config = Configs.create();
    config.addSpec(NetworkBufferInJava.spec);
  }

  @Test
  @DisplayName("test fluent API to load from default loader")
  void loadFromDefaultLoader() {
    final Config newConfig =
        config
            .from()
            .source(HoconProvider.get())
            .string(config.nameOf(NetworkBufferInJava.size) + " = 1024");
    assertThat(newConfig.get(NetworkBufferInJava.size), equalTo(1024));
  }
}
