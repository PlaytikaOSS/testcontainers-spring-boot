# Embeddable data services library using docker and testcontainers

If you are writing services using spring boot (and maybe spring cloud) and you do [medium sized](https://testing.googleblog.com/2010/12/test-sizes.html) tests during build process, then this set of spring boot auto-configurations might be handy.
By adding module into classpath, you will get stateful service, like couchbase or kafka, auto-started and available for connection from your application service w/o wiring any additional code.

## How to use
#### Make sure you have spring boot and spring cloud in classpath of your tests
```xml
<project>
...
      <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter</artifactId>
            <exclusions>
                <exclusion>
                    <artifactId>spring-boot-starter-logging</artifactId>
                    <groupId>org.springframework.boot</groupId>
                </exclusion>
            </exclusions>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-log4j2</artifactId>
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
##### Consumes
* embedded.mariadb.enabled `(true|false, default is 'true')`
* embedded.mariadb.dockerImage `(default is set to 'mariadb:10.3.2')`
  * You can pick wanted version on [dockerhub](https://hub.docker.com/r/library/mariadb/tags/)
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
##### Consumes
* embedded.couchbase.enabled `(true|false, default is 'true')`
* embedded.couchbase.services `(comma separated list, default is 'kv,index,n1ql,fts')`
* embedded.couchbase.clusterRamMb `(default is set to '256')`
* embedded.couchbase.bucketRamMb `(default is set to '100')`
* embedded.couchbase.dockerImage `(default is set to 'couchbase:community-4.5.1')`
  * You can pick wanted version on [dockerhub](https://hub.docker.com/r/library/couchbase/tags/)
  * NOTE: Versions of couchbase 2.x/3.x are not functional via docker, consider use of [CouchbaseMock](https://github.com/couchbase/CouchbaseMock)
* embedded.couchbase.bucketType `(default is set to 'couchbase')`
  * Used for test [bucket creation](https://developer.couchbase.com/documentation/server/3.x/admin/REST/rest-bucket-create.html)
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
##### Consumes
* embedded.zookeeper.enabled `(true|false, default is 'true')`
* embedded.kafka.enabled `(true|false, default is 'true')`
* embedded.kafka.topicsToCreate `(comma separated list of topic names, default is empty)`
* embedded.kafka.dockerImage `(default is set to kafka 0.11.x)`
  * To use kafka 0.10.x place "confluentinc/cp-kafka:3.2.2" or recent [from 3.2.x branch](https://hub.docker.com/r/confluentinc/cp-kafka/tags/)
  * To use kafka 0.11.x place "confluentinc/cp-kafka:3.3.0" or recent [from 3.3.x branch](https://hub.docker.com/r/confluentinc/cp-kafka/tags/)
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
##### Consumes
* aerospike [client library](https://mvnrepository.com/artifact/com.aerospike/aerospike-client) 
* embedded.aerospike.enabled `(true|false, default is 'true')`
* embedded.aerospike.dockerImage `(default is set to 'aerospike:3.15.0.1')`
  * You can pick wanted version on [dockerhub](https://hub.docker.com/r/library/aerospike/tags/)
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
##### Consumes
* embedded.memsql.enabled `(true|false, default is 'true')`
* embedded.memsql.dockerImage `(default is set to 'memsql/quickstart:minimal-6.0.8')`
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
##### Consumes
* embedded.redis.enabled `(true|false, default is 'true')`
* embedded.redis.dockerImage `(default is set to 'redis:4.0.2')`
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
##### Consumes
* embedded.neo4j.enabled `(true|false, default is 'true')`
* embedded.neo4j.dockerImage `(default is set to 'neo4j:3.2.7')`
##### Produces
* embedded.neo4j.user
* embedded.neo4j.password
* embedded.neo4j.httpsPort
* embedded.neo4j.httpPort
* embedded.neo4j.boltPort
### embedded-zookeeper
##### Consumes
* 
##### Produces
* 



