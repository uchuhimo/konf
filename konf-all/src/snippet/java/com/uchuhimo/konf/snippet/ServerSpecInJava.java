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

package com.uchuhimo.konf.snippet;

import com.uchuhimo.konf.ConfigSpec;
import com.uchuhimo.konf.OptionalItem;
import com.uchuhimo.konf.RequiredItem;

public class ServerSpecInJava {
  public static final ConfigSpec spec = new ConfigSpec("server");

  public static final OptionalItem<String> host =
      new OptionalItem<String>(spec, "host", "0.0.0.0") {};

  public static final RequiredItem<Integer> port = new RequiredItem<Integer>(spec, "port") {};
}
