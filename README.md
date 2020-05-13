
[![CircleCI](https://circleci.com/gh/testcontainers/testcontainers-spring-boot/tree/master.svg?style=shield&circle-token=d229579db6903be702f2416a357d1a01fb5c5fc0)](https://circleci.com/gh/testcontainers/testcontainers-spring-boot/tree/master)
[![codecov](https://codecov.io/gh/testcontainers/testcontainers-spring-boot/branch/master/graph/badge.svg)](https://codecov.io/gh/Playtika/testcontainers-spring-boot)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/add0d68eb8e040a1833a8f457189af7b)](https://www.codacy.com/app/PlaytikaCodacy/testcontainers-spring-boot?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=testcontainers/testcontainers-spring-boot&amp;utm_campaign=Badge_Grade)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.playtika.testcontainers/testcontainers-spring-boot/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.playtika.testcontainers/testcontainers-spring-boot)
# Data services library

If you are writing services using spring boot (and maybe spring cloud) and you do [medium sized](https://testing.googleblog.com/2010/12/test-sizes.html) tests during build process, then this set of spring boot auto-configurations might be handy.
By adding module into classpath, you will get stateful service, like couchbase or kafka, auto-started and available for connection from your application service w/o wiring any additional code.
[Docker](https://www.docker.com/) and [TestContainers](https://www.testcontainers.org/) are used to bootstrap stateful service using spring cloud [bootstrap phase](https://cloud.spring.io/spring-cloud-static/spring-cloud.html#_the_bootstrap_application_context).
Usage of spring cloud in your production code is optional, but you will need it in tests. See "how to" below.

# Table of Contents
1. [How to use](#how-to-use)
2. [List of properties per data service](#list-of-properties-per-data-service)
   1. [embedded-mariadb](#embedded-mariadb)
   2. [embedded-couchbase](#embedded-couchbase)
   3. [embedded-kafka](#embedded-kafka)
   4. [embedded-rabbitmq](#embedded-rabbitmq)
   5. [embedded-aerospike](#embedded-aerospike)
   6. [embedded-memsql](#embedded-memsql)
   7. [embedded-redis](#embedded-redis)
   8. [embedded-neo4j](#embedded-neo4j)
   9. [embedded-zookeeper](#embedded-zookeeper)
   10. [embedded-postgresql](#embedded-postgresql)
   11. [embedded-elasticsearch](#embedded-elasticsearch)
   12. [embedded-dynamodb](#embedded-dynamodb)
   13. [embedded-minio](#embedded-minio)
   14. [embedded-mongodb](#embedded-mongodb)
   15. [embedded-google-pubsub](#embedded-google-pubsub)
   16. [embedded-keycloak](#embedded-keycloak)
   17. [embedded-influxdb](#embedded-influxdb)
   18. [embedded-vault](#embedded-vault)
   19. [embedded-oracle-xe](#embedded-oracle-xe)
   20. [embedded-mysql](#embedded-mysql)
   21. [embedded-localstack](#embedded-localstack)

3. [How to contribute](#how-to-contribute)

## How to use
#### Make sure you have [spring boot and spring cloud](http://projects.spring.io/spring-cloud/#quick-start) in classpath of your tests. In case if you need to [pick version](https://mvnrepository.com/artifact/org.springframework.cloud/spring-cloud-starter).
```xml
<project>
...
      <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter</artifactId>
            <version>PICK LATEST VERSION</version>
            ...
        </dependency>
...
</project>
```
#### If you do not use spring cloud - make it work for tests only
```xml
<project>
...
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
            <version>PICK LATEST VERSION</version>
            ...
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter</artifactId>
            <version>PICK LATEST VERSION</version>          
            ...
            <scope>test</scope>
        </dependency>
...
</project>
```
#### Add data service library
```xml
<project>
...
        <dependency>
            <groupId>com.playtika.testcontainers</groupId>
            <artifactId>embedded-kafka</artifactId>
            <scope>test</scope>
        </dependency>
...
</project>
```
#### Use produced properties in your configuration
#### Example:
```properties
/src/test/resources/application.properties
```

```properties
spring.kafka.bootstrap-servers=${embedded.kafka.brokerList}
```
 /src/test/resources/bootstrap.properties
```properties
embedded.kafka.topicsToCreate=some_topic
```

## List of properties per data service

### embedded-mariadb
##### Maven dependency
```xml
<dependency>
    <groupId>com.playtika.testcontainers</groupId>
    <artifactId>embedded-mariadb</artifactId>
    <scope>test</scope>
</dependency>
```
##### Consumes (via bootstrap.properties)
* embedded.mariadb.enabled `(true|false, default is 'true')`
* embedded.mariadb.encoding `(default is 'utf8mb4')`
* embedded.mariadb.collation `(default is 'utf8mb4_unicode_ci')`
* embedded.mariadb.dockerImage `(default is set to 'mariadb:10.3.2')`
  * You can pick wanted version on [dockerhub](https://hub.docker.com/r/library/mariadb/tags/)
* embedded.mariadb.waitTimeoutInSeconds `(default is 60 seconds)`
* embedded.mariadb.initScriptPath `(default is null)`
##### Produces
* embedded.mariadb.port
* embedded.mariadb.host
* embedded.mariadb.schema
* embedded.mariadb.user
* embedded.mariadb.password

### embedded-couchbase
##### Maven dependency
```xml
<dependency>
    <groupId>com.playtika.testcontainers</groupId>
    <artifactId>embedded-couchbase</artifactId>
    <scope>test</scope>
</dependency>
```
##### Consumes (via bootstrap.properties)
* `embedded.couchbase.enabled` (true|false, default is 'true')
* `embedded.couchbase.services` (comma separated list, default is 'kv,index,n1ql,fts')
* `embedded.couchbase.clusterRamMb` (default is set to '256')
* `embedded.couchbase.bucketRamMb` (default is set to '100')
* `embedded.couchbase.dockerImage` (default is set to 'couchbase:community-4.5.1')
  * You can pick wanted version on [dockerhub](https://hub.docker.com/r/library/couchbase/tags/)
  * NOTE: Versions of couchbase 2.x/3.x are not functional via docker, consider use of [CouchbaseMock](https://github.com/couchbase/CouchbaseMock)
* `embedded.couchbase.bucketType` (default is set to 'couchbase')
  * Used for test [bucket creation](https://developer.couchbase.com/documentation/server/3.x/admin/REST/rest-bucket-create.html)
* `embedded.couchbase.waitTimeoutInSeconds` (default is 60 seconds)
##### Produces
* `embedded.couchbase.bootstrapHttpDirectPort`
  * Please note that this also produced as System property for [couchbase java client](https://github.com/couchbase/couchbase-jvm-core/blob/master/src/main/java/com/couchbase/client/core/env/DefaultCoreEnvironment.java)
* `embedded.couchbase.bootstrapCarrierDirectPort`
  * Please note that this also produced as System property for [couchbase java client](https://github.com/couchbase/couchbase-jvm-core/blob/master/src/main/java/com/couchbase/client/core/env/DefaultCoreEnvironment.java)
* `embedded.couchbase.host`
* `embedded.couchbase.bucket`
* `embedded.couchbase.user`
* `embedded.couchbase.password`

### embedded-kafka
##### Maven dependency
```xml
<dependency>
    <groupId>com.playtika.testcontainers</groupId>
    <artifactId>embedded-kafka</artifactId>
    <scope>test</scope>
</dependency>
```
##### Consumes (via bootstrap.properties)
* `embedded.zookeeper.enabled` (true|false, default is 'true')
* `embedded.zookeeper.waitTimeoutInSeconds` (default is 60 seconds)
* `embedded.zookeeper.dockerImage` (default : confluentinc/cp-zookeeper:4.1.2)
  * To use another zookeper version pick corresponding docker image on [dockerhub](https://hub.docker.com/r/confluentinc/cp-zookeeper/tags)
* `embedded.kafka.enabled` (true|false, default is 'true')
* `embedded.kafka.topicsToCreate` (comma separated list of topic names, default is empty)
* `embedded.kafka.secureTopics` (subset of embedded.kafka.topicsToCreate that should be secured with ACLs)
* `embedded.kafka.dockerImage` (default: confluentinc/cp-kafka:4.1.2. Kafka version is 1.1.x.)
  * To use another kafka version pick corresponding docker image on  [dockerhub](https://hub.docker.com/r/confluentinc/cp-kafka/tags)
  * [Confluent Platform and Apache Kafka Compatibility](https://docs.confluent.io/current/installation/versions-interoperability.html#cp-and-apache-kafka-compatibility)
* `embedded.kafka.waitTimeoutInSeconds` (default is 60 seconds)

##### Filesystem bindings
Containers for `embedded-kafka` and `embedded-zookeper` bind their volumes to host filesystem. By default, to your projects `target` folder. You can configure binding using properties:
* `embedded.zookeeper.fileSystemBind.enabled` (true|false, default is 'true')
* `embedded.zookeeper.fileSystemBind.dataFolder` (default : target/embedded-zk-data)
* `embedded.zookeeper.fileSystemBind.txnLogsFolder` (default : target/embedded-zk-txn-logs)
* `embedded.kafka.enabled (true|false, default is 'true')
* `embedded.kafka.fileSystemBind.dataFolder` (default : target/embedded-kafka-data)

##### Produces
* `embedded.zookeeper.zookeeperConnect`
* `embedded.kafka.brokerList`
* `embedded.kafka.saslPlaintext.brokerList`
* `embedded.kafka.saslPlaintext.user`
* `embedded.kafka.saslPlaintext.password`

##### Using SASL_PLAINTEXT
Note that only `SASL_PLAINTEXT` protocol supported, meaning TLS is not used.  
embedded-kafka configuration (via bootstrap.properties):
```properties
embedded.kafka.topicsToCreate=nonSecureTopic,secureTopic
embedded.kafka.secureTopics=secureTopic
```
Application configuration:
```properties
# Common for Producer and Consumer 
bootstrap.servers=${embedded.kafka.saslPlaintext.brokerList}
security.protocol=SASL_PLAINTEXT
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required \
  username="${embedded.kafka.saslPlaintext.user}" \
  password="${embedded.kafka.saslPlaintext.password}";

# Consumer group.id could be any as ACLs are applied to wildcard group
```


### embedded-rabbitmq
##### Maven dependency
```xml
<dependency>
    <groupId>com.playtika.testcontainers</groupId>
    <artifactId>embedded-rabbitmq</artifactId>
    <scope>test</scope>
</dependency>
```
##### Consumes (via bootstrap.properties)
* `embedded.rabbitmq.enabled` (true|false, default is 'true')
* `embedded.rabbitmq.password` (default : rabbitmq)
* `embedded.rabbitmq.vhost` (virtual host, default: '/')
* `embedded.rabbitmq.dockerImage` (default: rabbitmq:3-alpine)
  * You can pick wanted version on [dockerhub](https://hub.docker.com/r/library/rabbitmq/tags/)
* `embedded.rabbitmq.waitTimeoutInSeconds` (default is 60 seconds)
##### Produces
* `embedded.rabbitmq.host`
* `embedded.rabbitmq.port`
* `embedded.rabbitmq.user`
* `embedded.rabbitmq.password`
* `embedded.rabbitmq.vhost`

### embedded-aerospike
##### Maven dependency
```xml
<dependency>
    <groupId>com.playtika.testcontainers</groupId>
    <artifactId>embedded-aerospike</artifactId>
    <scope>test</scope>
</dependency>
```
##### Consumes (via bootstrap.properties)
* aerospike [client library](https://mvnrepository.com/artifact/com.aerospike/aerospike-client) 
* `embedded.aerospike.enabled` (true|false, default is 'true')
* `embedded.aerospike.dockerImage` (default is set to 'aerospike/aerospike-server:4.3.0.8')
  * You can pick wanted version on [dockerhub](https://hub.docker.com/r/library/aerospike/tags/)
* `embedded.aerospike.waitTimeoutInSeconds` (default is 60 seconds)
##### Produces
* `embedded.aerospike.host`
* `embedded.aerospike.port`
* `embedded.aerospike.namespace`
* AerospikeTimeTravelService
  * timeTravelTo
  * nextDay
  * addDays
  * addHours
  * rollbackTime

### embedded-memsql
##### Maven dependency
```xml
<dependency>
    <groupId>com.playtika.testcontainers</groupId>
    <artifactId>embedded-memsql</artifactId>
    <scope>test</scope>
</dependency>
```
##### Consumes (via bootstrap.properties)
* `embedded.memsql.enabled` (true|false, default is 'true')
* `embedded.memsql.dockerImage` (default is set to 'memsql/quickstart:minimal-6.0.8')
  * You can pick wanted version on [dockerhub](https://hub.docker.com/r/memsql/quickstart/tags/)
* `embedded.memsql.waitTimeoutInSeconds` (default is 60 seconds)
##### Produces
* `embedded.memsql.port`
* `embedded.memsql.host`
* `embedded.memsql.schema`
* `embedded.memsql.user`
* `embedded.memsql.password`
##### Notes
* Images without "minimal" tag do no start withing 30 secs, so they are unusable
* There should be at least 1.5 GB of RAM available for memsql to start
* You can enable debug logs for com.playtika.test category to troubleshoot issues

### embedded-redis
##### Maven dependency
```xml
<dependency>
    <groupId>com.playtika.testcontainers</groupId>
    <artifactId>embedded-redis</artifactId>
    <scope>test</scope>
</dependency>
```
##### Consumes (via bootstrap.properties)
* `embedded.redis.enabled` (true|false, default is 'true')
* `embedded.redis.dockerImage` (default is set to 'redis:4.0.12')
  * You can pick wanted version on [dockerhub](https://hub.docker.com/r/library/redis/tags/)
* `embedded.redis.waitTimeoutInSeconds` (default is 60 seconds)  
* `embedded.redis.clustered` (default is 'false')
  * If 'true' Redis is started in cluster mode
* `embedded.redis.requirepass` (default is 'true')
##### Produces
* `embedded.redis.host`
* `embedded.redis.port`
* `embedded.redis.user`
* `embedded.redis.password` 

### embedded-neo4j
##### Maven dependency
```xml
<dependency>
    <groupId>com.playtika.testcontainers</groupId>
    <artifactId>embedded-neo4j</artifactId>
    <scope>test</scope>
</dependency>
```
##### Consumes (via bootstrap.properties)
* `embedded.neo4j.enabled` (true|false, default is 'true')
* `embedded.neo4j.dockerImage` (default is set to 'neo4j:3.2.7')
  * You can pick wanted version on [dockerhub](https://hub.docker.com/r/library/neo4j/tags/)
* `embedded.neo4j.waitTimeoutInSeconds` (default is 60 seconds)
##### Produces
* `embedded.neo4j.user`
* `embedded.neo4j.password`
* `embedded.neo4j.httpsPort`
* `embedded.neo4j.httpPort`
* `embedded.neo4j.boltPort`

### embedded-zookeeper
##### Under construction...
##### Consumes (via bootstrap.properties)
* 
##### Produces
* 

### embedded-postgresql
##### Maven dependency
```xml
<dependency>
    <groupId>com.playtika.testcontainers</groupId>
    <artifactId>embedded-postgresql</artifactId>
    <scope>test</scope>
</dependency>
```
##### Consumes (via bootstrap.properties)
* `embedded.postgresql.enabled` (true|false, default is 'true')
* `embedded.postgresql.dockerImage` (default is set to 'postgres:10-alpine')
  * You can pick wanted version on [dockerhub](https://hub.docker.com/r/library/postgres/tags/)
* `embedded.postgresql.waitTimeoutInSeconds` (default is 60 seconds)
* `embedded.postgresql.database`
* `embedded.postgresql.user`
* `embedded.postgresql.password`
* `embedded.postgresql.initScriptPath` (default is null)
##### Produces
* `embedded.postgresql.port`
* `embedded.postgresql.host`
* `embedded.postgresql.schema`
* `embedded.postgresql.user`
* `embedded.postgresql.password`

### embedded-elasticsearch
##### Maven dependency
```xml
<dependency>
    <groupId>com.playtika.testcontainers</groupId>
    <artifactId>embedded-elasticsearch</artifactId>
    <scope>test</scope>
</dependency>
```
##### Consumes (via bootstrap.properties)
* `embedded.elasticsearch.enabled` (true|false, default is 'true')
* `embedded.elasticsearch.dockerImage` (default is set to 'docker.elastic.co/elasticsearch/elasticsearch:6.2.4')
* `embedded.elasticsearch.indices` (indices to create, no indices are created by default)
  * You can pick wanted version on [docker.elastic.co](https://www.docker.elastic.co)
* `embedded.elasticsearch.waitTimeoutInSeconds` (default is 60 seconds)
##### Produces
* `embedded.elasticsearch.clusterName`
* `embedded.elasticsearch.host`
* `embedded.elasticsearch.httpPort`
* `embedded.elasticsearch.transportPort`

### embedded-dynamodb
##### Maven dependency
```xml
<dependency>
    <groupId>com.playtika.testcontainers</groupId>
    <artifactId>embedded-dynamodb</artifactId>
    <scope>test</scope>
</dependency>
```
##### Consumes (via bootstrap.properties)
* `embedded.dynamodb.enabled` (true|false, default is 'true')
* `embedded.dynamodb.dockerImage` (default is set to 'amazon/dynamodb-local:latest')
  * You can pick wanted version on [dockerhub](https://hub.docker.com/r/amazon/dynamodb-local/)
##### Produces
* `embedded.dynamodb.host`
* `embedded.dynamodb.port`
* `embedded.dynamodb.accessKey`
* `embedded.dynamodb.secretKey`

### embedded-voltdb
##### Maven dependency
```xml
<dependency>
    <groupId>com.playtika.testcontainers</groupId>
    <artifactId>embedded-voltdb</artifactId>
    <scope>test</scope>
</dependency>
```
##### Consumes (via bootstrap.properties)
* `embedded.voltdb.enabled` (true|false, default is 'true')
* `embedded.voltdb.dockerImage` (default is set to 'voltdb/voltdb-community:8.3.3')
  * You can pick wanted version on [dockerhub](https://hub.docker.com/r/voltdb/voltdb-community/tags/)
##### Produces
* `embedded.voltdb.host`
* `embedded.voltdb.port`

VoltDB container has no security enabled, you can use any credentials.

### embedded-minio
##### Maven dependency
```xml
<dependency>
    <groupId>com.playtika.testcontainers</groupId>
    <artifactId>embedded-minio</artifactId>
    <scope>test</scope>
</dependency>
```
##### Consumes (via bootstrap.properties)
* `embedded.minio.enabled` (true|false, default is 'true')
* `embedded.minio.dockerImage` (default is set to 'minio/minio')
  * You can pick wanted version on [dockerhub](https://hub.docker.com/r/minio/minio/tags)
* `embedded.minio.accessKey` (default is set to 'AKIAIOSFODNN7EXAMPLE")
* `embedded.minio.secretKey` (default is set to 'wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY')
* `embedded.minio.userName` (default is set to 'minio')
* `embedded.minio.groupName` (default is set to 'minio') 
* `embedded.minio.region` (default is set to '')
* `embedded.minio.worm` (on|off, default is set to 'off') 
* `embedded.minio.browser` (on|off, default is set to 'on') 
* `embedded.minio.directory` (default is set to '/data') 

##### Produces
* `embedded.minio.host`
* `embedded.minio.port`
* `embedded.minio.accessKey`
* `embedded.minio.secretKey`
* `embedded.minio.region`
  
### embedded-mongodb

##### Maven dependency
```xml
<dependency>
    <groupId>com.playtika.testcontainers</groupId>
    <artifactId>embedded-mongodb</artifactId>
    <scope>test</scope>
</dependency>
```

##### Consumes (via bootstrap.properties)
* `embedded.mongodb.enabled` (true|false, default is 'true')
* `embedded.mongodb.dockerImage` (default is set to 'mongo:4.2.0-bionic')
  * You can pick wanted version on [dockerhub](https://hub.docker.com/_/mongo?tab=tags)
* `embedded.mongodb.host` (default is localhost)
* `embedded.mongodb.port` (default is 27017)
* `embedded.mongodb.username`
* `embedded.mongodb.password`
* `embedded.mongodb.database` (default is test)

##### Produces
* `embedded.mongodb.host`
* `embedded.mongodb.port` (mapped port)
* `embedded.mongodb.username`
* `embedded.mongodb.password`
* `embedded.mongodb.database`

##### Example
To auto-configure spring-data-mongodb use these properties in your test `application.properties`:
```
spring.data.mongodb.uri=mongodb://${embedded.mongodb.host}:${embedded.mongodb.port}/${embedded.mongodb.database}
```

### embedded-google-pubsub

##### Maven dependency
```xml
<dependency>
    <groupId>com.playtika.testcontainers</groupId>
    <artifactId>embedded-google-pubsub</artifactId>
    <scope>test</scope>
</dependency>
```

##### Consumes (via bootstrap.properties)
* `embedded.google.pubsub.enabled` (true|false, default is 'true')
* `embedded.google.pubsub.dockerImage` (default is set to 'google/cloud-sdk:257.0.0')
  * You can pick wanted version on [dockerhub](https://hub.docker.com/r/google/cloud-sdk/)
* `embedded.google.pubsub.host` (default is 0.0.0.0)
* `embedded.google.pubsub.port` (default is 8089)
* `embedded.google.pubsub.project-id` (default is my-project-id)
* Topics and Subscriptions:
  * `embedded.google.pubsub.topics-and-subscriptions[0].topic=topic0_name`
  * `embedded.google.pubsub.topics-and-subscriptions[0].subscription=subscription0_name`
  * `embedded.google.pubsub.topics-and-subscriptions[1].topic=topic0_name`
  * `embedded.google.pubsub.topics-and-subscriptions[1].subscription=subscription0_name`

##### Produces
* `embedded.google.pubsub.host`
* `embedded.google.pubsub.port` (mapped port)
* `embedded.google.pubsub.project-id`

##### Example
To auto-configure spring-cloud-gcp-starter-pubsub use these properties in your test `application.properties`:
```
spring.cloud.gcp.project-id=${embedded.google.pubsub.project-id}
spring.cloud.gcp.pubsub.emulatorHost=${embedded.google.pubsub.host}:${embedded.google.pubsub.port}
```

### embedded-keycloak

##### Maven dependency
```xml
<dependency>
    <groupId>com.playtika.testcontainers</groupId>
    <artifactId>embedded-keycloak</artifactId>
    <scope>test</scope>
</dependency>
```

##### Consumes (via bootstrap.properties)
* `embedded.keycloak.enabled` (boolean, true|false, default is 'true')
* `embedded.keycloak.dockerImage` (String, default is set to 'jboss/keycloak:8.0.1')
  * You can pick wanted version on [dockerhub](https://hub.docker.com/r/jboss/keycloak)
* `embedded.keycloak.command` (String[], default is '["-c", "standalone.xml", "-Dkeycloak.profile.feature.upload_scripts=enabled"]')
* `embedded.keycloak.admin-user` (String, default is 'admin')
* `embedded.keycloak.admin-password` (String, default is 'letmein')
* `embedded.keycloak.import-file` (String, default is '')

##### Produces
* `embedded.keycloak.host`
* `embedded.keycloak.http-port` (mapped HTTP port)
* `embedded.keycloak.auth-server-url`

##### Example
To configure for example the `keycloak-spring-boot-starter` use these properties in your test `application.yml`:
```yaml
keycloak:
  auth-server-url: ${embedded.keycloak.auth-server-url}
```

### embedded-influxdb

#### Maven dependency
```xml
<dependency>
    <groupId>com.playtika.testcontainers</groupId>
    <artifactId>embedded-influxdb</artifactId>
    <scope>test</scope>
</dependency>
```

##### Consumes (via bootstrap.properties)
* `embedded.influxdb.enabled` (boolean, true|false, default is 'true')
* `embedded.influxdb.dockerImage` (String, default is set to 'influxdb:1.5-alpine')
  * You can pick wanted version on [dockerhub](https://hub.docker.com/_/influxdb)
* `embedded.influxdb.admin-user` (String, default is 'admin')
* `embedded.influxdb.admin-password` (String, default is 'admin')
* `embedded.influxdb.user` (String, default is 'user')
* `embedded.influxdb.password` (String, default is 'password')
* `embedded.influxdb.enable-http-auth` (boolean true|false, default is 'true')
* `embedded.influxdb.host` (String, default is 'localhost')
* `embedded.influxdb.port` (int, default is '8086')
* `embedded.influxdb.database` (String, default is 'db')

##### Produces
* `embedded.influxdb.database`
* `embedded.influxdb.host`
* `embedded.influxdb.password`
* `embedded.influxdb.port` (mapped HTTP port)
* `embedded.influxdb.user`

##### Example
There is currently no starter library for using InfluxDB server version 1.x because it uses basic HTTP protocol to communicate.
You can anyway create your own properties using those values for example in your test `application.yml`:
```yaml
influxdb:
  url: http://${embedded.influxdb.host}:${embedded.influxdb.port}
  user: ${embedded.influxdb.user}
  password: ${embedded.influxdb.password}
  database: ${embedded.influxdb.database}
```

### embedded-vault

##### Maven dependency
```xml
<dependency>
    <groupId>com.playtika.testcontainers</groupId>
    <artifactId>embedded-vault</artifactId>
    <scope>test</scope>
</dependency>
```

##### Consumes (via bootstrap.properties)
* `embedded.vault.enabled` (boolean, true|false, default is 'true')
* `embedded.vault.dockerImage` (String, default is set to 'vault:1.4.0')
  * You can pick wanted version on [dockerhub](https://hub.docker.com/_/vault)
* `embedded.vault.host` (String, default is 'localhost')
* `embedded.vault.port` (int, default is 8200)
* `embedded.vault.token` (String, default is '00000000-0000-0000-0000-000000000000')
* `embedded.vault.path` (String, default is 'secret/application')
* `embedded.vault.secrets` (Map, default is empty)

##### Produces
* `embedded.vault.host`
* `embedded.vault.port` (mapped HTTP port)
* `embedded.vault.token`

##### Example
To auto-configure spring-cloud-vault-config use these properties in your test `bootstrap.properties`:
```yaml
spring.cloud.vault:
  scheme: http
  host: ${embedded.vault.host}
  port: ${embedded.vault.port}
  token: ${embedded.vault.token}
  kv:
   enabled: true
```

### embedded-oracle-xe

##### Maven dependency
```xml
<dependency>
    <groupId>com.playtika.testcontainers</groupId>
    <artifactId>embedded-oracle-xe</artifactId>
    <scope>test</scope>
</dependency>
```

##### Consumes (via bootstrap.properties)
* `embedded.oracle.enabled` (boolean, true|false, default is 'true')
* `embedded.oracle.waitTimeoutInSeconds` (default is 60 seconds)
* `embedded.oracle.dockerImage` (String, default is set to 'oracleinanutshell/oracle-xe-11g')
  * You can pick wanted image on [dockerhub](https://hub.docker.com/search?q=oracle-xe&type=image)
  * You can [build image by yourself](https://github.com/oracle/docker-images/tree/master/OracleDatabase/SingleInstance)
* `embedded.oracle.user` (String, default is 'system')
* `embedded.oracle.password` (String, default is 'oracle')
* `embedded.oracle.initScriptPath` (default is null)

##### Produces
* `embedded.oracle.host`
* `embedded.oracle.port` (mapped TCP port)
* `embedded.oracle.db` (set to 'xe')


### embedded-mysql
##### Maven dependency
```xml
<dependency>
    <groupId>com.playtika.testcontainers</groupId>
    <artifactId>embedded-mysql</artifactId>
    <scope>test</scope>
</dependency>
```
##### Consumes (via bootstrap.properties)
* `embedded.mysql.enabled` (true|false, default is 'true')
* `embedded.mysql.encoding` (default is 'utf8mb4')
* `embedded.mysql.collation` (default is 'utf8mb4_unicode_ci')
* `embedded.mysql.dockerImage` (default is set to 'mysql:5.7.22')
  * You can pick wanted version on [dockerhub](https://hub.docker.com/r/library/mysql/tags/)
* `embedded.mysql.waitTimeoutInSeconds` (default is 60 seconds)
* `embedded.mysql.initScriptPath` (default is null)
##### Produces
* `embedded.mysql.port`
* `embedded.mysql.host`
* `embedded.mysql.schema`
* `embedded.mysql.user`
* `embedded.mysql.password`

### embedded-localstack
##### Maven dependency
```xml
<dependency>
    <groupId>com.playtika.testcontainers</groupId>
    <artifactId>embedded-localstack</artifactId>
    <scope>test</scope>
</dependency>
```
##### Consumes (via bootstrap.properties)
* `embedded.localstack.enabled` (true|false, default is 'true')
* `embedded.localstack.services` (comma separated list of AWS services (S3, SQS, DYNAMODB, etc. Should be non empty)
    * You can choose available services on [localstack](https://github.com/localstack/localstack)
* `embedded.localstack.defaultRegion` (default is 'us-east-1')
* `embedded.localstack.dockerImage` (default is set to 'localstack/localstack:0.10.8')
    * You can pick a desired version on [dockerhub](https://hub.docker.com/r/localstack/localstack/tags)
* `embedded.localstack.useSsl` (default is 'false')
* `embedded.localstack.hostname` (default is 'localhost')
* `embedded.localstack.hostnameExternal` (default is 'localhost')
* `embedded.localstack.edgePort` (default is '4566')
##### Produces
* `embedded.localstack.host`
* `embedded.localstack.accessKey`
* `embedded.localstack.secretKey`
* `embedded.localstack.region`
* `embedded.localstack.{service}` (service endpoint for connection)
* `embedded.localstack.{service}.port` (mapped port for connection)

## How to contribute
### Flow
* There is 2 branches in project: master and develop
* You need to fork project and create branch from develop
* You do not need to update project version in pom.xml files, this will be done by release job
* Once finished - create pull request to develop from your fork, pass review and wait for merge
* On release, ci job will merge develop into master and remove snapshot + publish artifacts into public maven repo
### Release
* Release build is done on using [gitflow-maven-plugin](https://github.com/aleksandr-m/gitflow-maven-plugin)
* Release is done per each major change, critical bug
* Release can be done by contributor request
* Contacts to start release: 
   * [obevzenko@playtika.com](mailto:obevzenko@playtika.com)
   * [ivasylyev@playtika.com](mailto:ivasylyev@playtika.com)
