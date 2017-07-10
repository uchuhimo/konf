package com.uchuhimo.konf;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("test Java API of Config")
class ConfigJavaApiTest {
  private Config config;

  @BeforeEach
  void initConfig() {
    config = Configs.create();
    config.addSpec(NetworkBufferInJava.spec);
  }

  @Test
  @DisplayName("test `Configs.create`")
  void create() {
    final Config config = Configs.create();
    assertThat(config.getItems().size(), equalTo(0));
  }

  @Test
  @DisplayName("test `Configs.create` with init block")
  void createWithInit() {
    final Config config = Configs.create(it -> it.addSpec(NetworkBufferInJava.spec));
    assertThat(config.getItems().size(), equalTo(4));
  }

  @Test
  @DisplayName("test fluent API to load from map")
  void loadFromMap() {
    final HashMap<String, Integer> map = new HashMap<>();
    map.put(NetworkBufferInJava.size.getName(), 1024);
    final Config newConfig = config.loadFrom().map.kv(map);
    assertThat(newConfig.get(NetworkBufferInJava.size), equalTo(1024));
  }

  @Test
  @DisplayName("test fluent API to load from loader")
  void loadFromLoader() {
    final Config newConfig =
        config.loadFrom().hocon.string(NetworkBufferInJava.size.getName() + " = 1024");
    assertThat(newConfig.get(NetworkBufferInJava.size), equalTo(1024));
  }

  @Test
  @DisplayName("test fluent API to load from system properties")
  void loadFromSystem() {
    System.setProperty(NetworkBufferInJava.size.getName(), "1024");
    final Config newConfig = config.loadFrom().systemProperties();
    assertThat(newConfig.get(NetworkBufferInJava.size), equalTo(1024));
  }

  @Test
  @DisplayName("test `get(Item<T>)`")
  void getWithItem() {
    final String name = config.get(NetworkBufferInJava.name);
    assertThat(name, equalTo("buffer"));
  }

  @Test
  @DisplayName("test `get(String)`")
  void getWithName() {
    final NetworkBuffer.Type type = config.get(NetworkBufferInJava.type.getName());
    assertThat(type, equalTo(NetworkBuffer.Type.OFF_HEAP));
  }

  @Test
  @DisplayName("test `set(Item<T>, T)`")
  void setWithItem() {
    config.set(NetworkBufferInJava.size, 1024);
    assertThat(config.get(NetworkBufferInJava.size), equalTo(1024));
  }

  @Test
  @DisplayName("test `set(String, T)`")
  void setWithName() {
    config.set(NetworkBufferInJava.size.getName(), 1024);
    assertThat(config.get(NetworkBufferInJava.size), equalTo(1024));
  }

  @Test
  @DisplayName("test `lazySet(Item<T>, Function1<ConfigGetter, T>)`")
  void lazySetWithItem() {
    config.lazySet(NetworkBufferInJava.maxSize, it -> it.get(NetworkBufferInJava.size) * 4);
    config.set(NetworkBufferInJava.size, 1024);
    assertThat(config.get(NetworkBufferInJava.maxSize), equalTo(1024 * 4));
  }

  @Test
  @DisplayName("test `lazySet(String, Function1<ConfigGetter, T>)`")
  void lazySetWithName() {
    config.lazySet(
        NetworkBufferInJava.maxSize.getName(), it -> it.get(NetworkBufferInJava.size) * 4);
    config.set(NetworkBufferInJava.size, 1024);
    assertThat(config.get(NetworkBufferInJava.maxSize), equalTo(1024 * 4));
  }
}
