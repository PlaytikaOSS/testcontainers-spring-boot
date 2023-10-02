package com.playtika.testcontainer.keydb.wait;

import com.playtika.testcontainer.common.checks.AbstractCommandWaitStrategy;
import com.playtika.testcontainer.keydb.KeyDbProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class KeyDbStatusCheck extends AbstractCommandWaitStrategy {

  private final KeyDbProperties properties;

  @Override
  public String[] getCheckCommand() {
    if (properties.isRequirepass()) {
      return new String[]{
        "keydb-cli", "-a", properties.getPassword(), "-p", String.valueOf(properties.getPort()), "ping"
      };
    } else {
      return new String[]{
        "keydb-cli", "-p", String.valueOf(properties.getPort()), "ping"
      };
    }
  }
}
