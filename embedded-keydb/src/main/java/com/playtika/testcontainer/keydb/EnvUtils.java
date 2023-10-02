package com.playtika.testcontainer.keydb;

import lombok.experimental.UtilityClass;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.playtika.testcontainer.keydb.EmbeddedKeyDbBootstrapConfiguration.KEYDB_NETWORK_ALIAS;

@UtilityClass
public class EnvUtils {

  static Map<String, Object> registerKeyDbEnvironment(ConfigurableEnvironment environment, GenericContainer<?> keyDb,
                                                      KeyDbProperties properties, int port) {
    String host = keyDb.getHost();

    LinkedHashMap<String, Object> map = new LinkedHashMap<>();
    map.put("embedded.keydb.port", port);
    map.put("embedded.keydb.host", host);
    map.put("embedded.keydb.password", properties.getPassword());
    map.put("embedded.keydb.user", properties.getUser());
    map.put("embedded.keydb.networkAlias", KEYDB_NETWORK_ALIAS);
    MapPropertySource propertySource = new MapPropertySource("embeddedKeyDbInfo", map);
    environment.getPropertySources().addFirst(propertySource);
    return map;
  }

}
