
[![CircleCI](https://circleci.com/gh/Playtika/testcontainers-spring-boot/tree/master.svg?style=shield&circle-token=d229579db6903be702f2416a357d1a01fb5c5fc0)](https://circleci.com/gh/Playtika/testcontainers-spring-boot/tree/master)
[![codecov](https://codecov.io/gh/Playtika/testcontainers-spring-boot/branch/master/graph/badge.svg)](https://codecov.io/gh/Playtika/testcontainers-spring-boot)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/9bf7bea79a2b4048bf3ed8fa247092d1)](https://www.codacy.com/app/alekseibevzenko/testcontainers-spring-boot?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=Playtika/testcontainers-spring-boot&amp;utm_campaign=Badge_Grade)
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
   4. [embedded-aerospike](#embedded-aerospike)
   5. [embedded-memsql](#embedded-memsql)
   6. [embedded-redis](#embedded-redis)
   7. [embedded-neo4j](#embedded-neo4j)
   8. [embedded-zookeeper](#embedded-zookeeper)
   9. [embedded-postgresql](#embedded-postgresql)
   10. [embedded-elasticsearch](#embedded-elasticsearch)
3. [How to contribute](#how-to-contribute)

## How to use
#### Make sure you have [spring boot and spring cloud](http://projects.spring.io/spring-cloud/#quick-start) in classpath of your tests. In case if you need to [pick version](https://mvnrepository.com/artifact/org.springframework.cloud/spring-cloud-starter).
```xml
<project>
...
      <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter</artifactId>
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
            ...
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter</artifactId>
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
/src/test/resources/application.properties

```properties
spring.kafka.zookeeperHost=${embedded.zookeeper.zookeeperConnect}
spring.kafka.brokerList=${embedded.kafka.brokerList}
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
* embedded.couchbase.enabled `(true|false, default is 'true')`
* embedded.couchbase.services `(comma separated list, default is 'kv,index,n1ql,fts')`
* embedded.couchbase.clusterRamMb `(default is set to '256')`
* embedded.couchbase.bucketRamMb `(default is set to '100')`
* embedded.couchbase.dockerImage `(default is set to 'couchbase:community-4.5.1')`
  * You can pick wanted version on [dockerhub](https://hub.docker.com/r/library/couchbase/tags/)
  * NOTE: Versions of couchbase 2.x/3.x are not functional via docker, consider use of [CouchbaseMock](https://github.com/couchbase/CouchbaseMock)
* embedded.couchbase.bucketType `(default is set to 'couchbase')`
  * Used for test [bucket creation](https://developer.couchbase.com/documentation/server/3.x/admin/REST/rest-bucket-create.html)
* embedded.couchbase.waitTimeoutInSeconds `(default is 60 seconds)`
##### Produces
* embedded.couchbase.bootstrapHttpDirectPort
  * Please note that this also produced as System property for [couchbase java client](https://github.com/couchbase/couchbase-jvm-core/blob/master/src/main/java/com/couchbase/client/core/env/DefaultCoreEnvironment.java)
* embedded.couchbase.bootstrapCarrierDirectPort
  * Please note that this also produced as System property for [couchbase java client](https://github.com/couchbase/couchbase-jvm-core/blob/master/src/main/java/com/couchbase/client/core/env/DefaultCoreEnvironment.java)
* embedded.couchbase.host
* embedded.couchbase.bucket
* embedded.couchbase.user
* embedded.couchbase.password
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
* embedded.zookeeper.enabled `(true|false, default is 'true')`
* embedded.zookeeper.waitTimeoutInSeconds `(default is 60 seconds)`
* embedded.kafka.enabled `(true|false, default is 'true')`
* embedded.kafka.topicsToCreate `(comma separated list of topic names, default is empty)`
* embedded.kafka.dockerImage `(default is set to kafka 0.11.x)`
  * To use kafka 0.10.x place "confluentinc/cp-kafka:3.2.2" or recent [from 3.2.x branch](https://hub.docker.com/r/confluentinc/cp-kafka/tags/)
  * To use kafka 0.11.x place "confluentinc/cp-kafka:3.3.0" or recent [from 3.3.x branch](https://hub.docker.com/r/confluentinc/cp-kafka/tags/)
* embedded.kafka.waitTimeoutInSeconds `(default is 60 seconds)`
##### Produces
* embedded.zookeeper.zookeeperConnect
* embedded.kafka.brokerList
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
* embedded.aerospike.enabled `(true|false, default is 'true')`
* embedded.aerospike.dockerImage `(default is set to 'aerospike:3.15.0.1')`
  * You can pick wanted version on [dockerhub](https://hub.docker.com/r/library/aerospike/tags/)
* embedded.aerospike.waitTimeoutInSeconds `(default is 60 seconds)`
##### Produces
* embedded.aerospike.host
* embedded.aerospike.port
* embedded.aerospike.namespace
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
* embedded.memsql.enabled `(true|false, default is 'true')`
* embedded.memsql.dockerImage `(default is set to 'memsql/quickstart:minimal-6.0.8')`
  * You can pick wanted version on [dockerhub](https://hub.docker.com/r/memsql/quickstart/tags/)
* embedded.memsql.waitTimeoutInSeconds `(default is 60 seconds)`
##### Produces
* embedded.memsql.port
* embedded.memsql.host
* embedded.memsql.schema
* embedded.memsql.user
* embedded.memsql.password
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
* embedded.redis.enabled `(true|false, default is 'true')`
* embedded.redis.dockerImage `(default is set to 'redis:4.0.2')`
  * You can pick wanted version on [dockerhub](https://hub.docker.com/r/library/redis/tags/)
* embedded.redis.waitTimeoutInSeconds `(default is 60 seconds)`
##### Produces
* embedded.redis.host
* embedded.redis.port
* embedded.redis.user
* embedded.redis.password 
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
* embedded.neo4j.enabled `(true|false, default is 'true')`
* embedded.neo4j.dockerImage `(default is set to 'neo4j:3.2.7')`
  * You can pick wanted version on [dockerhub](https://hub.docker.com/r/library/neo4j/tags/)
* embedded.neo4j.waitTimeoutInSeconds `(default is 60 seconds)`
##### Produces
* embedded.neo4j.user
* embedded.neo4j.password
* embedded.neo4j.httpsPort
* embedded.neo4j.httpPort
* embedded.neo4j.boltPort
### embedded-zookeeper
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
* embedded.postgresql.enabled `(true|false, default is 'true')`
* embedded.postgresql.dockerImage `(default is set to 'postgres:9.6.8')`
  * You can pick wanted version on [dockerhub](https://hub.docker.com/r/library/postgres/tags/)
* embedded.postgresql.waitTimeoutInSeconds `(default is 60 seconds)`
##### Produces
* embedded.postgresql.port
* embedded.postgresql.host
* embedded.postgresql.schema
* embedded.postgresql.user
* embedded.postgresql.password
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
* embedded.elasticsearch.enabled `(true|false, default is 'true')`
* embedded.elasticsearch.dockerImage `(default is set to 'docker.elastic.co/elasticsearch/elasticsearch:6.2.4')`
* embedded.elasticsearch.indices `(indices to create, no indices are created by default)`
  * You can pick wanted version on [docker.elastic.co](https://www.docker.elastic.co)
* embedded.elasticsearch.waitTimeoutInSeconds `(default is 60 seconds)`
##### Produces
* embedded.elasticsearch.clusterName
* embedded.elasticsearch.host
* embedded.elasticsearch.httpPort
* embedded.elasticsearch.transportPort
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
