package com.uchuhimo.konf;

public class NetworkBufferInJava {
  public static final ConfigSpec spec = new ConfigSpec("network.buffer");

  public static final RequiredItem<Integer> size =
      new RequiredItem<Integer>(spec, "size", "size of buffer in KB") {};

  public static final LazyItem<Integer> maxSize =
      new LazyItem<Integer>(
          spec,
          "maxSize",
          config -> config.get(size) * 2,
          "${size.name} * 2",
          "max size of buffer in KB") {};

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
