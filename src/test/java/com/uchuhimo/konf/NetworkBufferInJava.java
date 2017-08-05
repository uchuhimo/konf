/*
 * Copyright 2017 the original author or authors.
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

public class NetworkBufferInJava {
  public static final ConfigSpec spec = new ConfigSpec("network.buffer");

  public static final RequiredItem<Integer> size =
      new RequiredItem<Integer>(spec, "size", "size of buffer in KB") {};

  public static final LazyItem<Integer> maxSize =
      new LazyItem<Integer>(
          spec, "maxSize", config -> config.get(size) * 2, "max size of buffer in KB") {};

  public static final OptionalItem<String> name =
      new OptionalItem<String>(spec, "name", "buffer", "name of buffer") {};

  public static final OptionalItem<NetworkBuffer.Type> type =
      new OptionalItem<NetworkBuffer.Type>(
          spec,
          "type",
          NetworkBuffer.Type.OFF_HEAP,
          "type of network buffer.\n"
              + "two type:\n"
              + "- on-heap\n"
              + "- off-heap\n"
              + "buffer is off-heap by default.") {};
}
