package com.playtika.testcontainer.keydb.wait;

import com.playtika.testcontainer.keydb.KeyDbProperties;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;

public class DefaultKeyDbClusterWaitStrategy extends WaitAllStrategy implements KeyDbClusterWaitStrategy {
  public DefaultKeyDbClusterWaitStrategy(KeyDbProperties properties) {
    withStrategy(new KeyDbStatusCheck(properties))
      .withStrategy(new KeyDbClusterStatusCheck(properties));
  }
}
