/*
 * Copyright 2017-2020 the original author or authors.
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

import java.util.function.Consumer;

/** Helper class for {@link com.uchuhimo.konf.Config Config}. */
public final class Configs {
  private Configs() {}

  /**
   * Create a new root config.
   *
   * @return a new root config
   */
  public static Config create() {
    return Config.Companion.invoke();
  }

  /**
   * Create a new root config and initiate it.
   *
   * @param init initial action
   * @return a new root config
   */
  public static Config create(Consumer<Config> init) {
    final Config config = create();
    init.accept(config);
    return config;
  }
}
