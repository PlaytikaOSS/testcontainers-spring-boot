# Embeddable data services library using docker and testcontainers

If you are writing services using spring boot (and maybe spring cloud) and you do integration testing during build process then this set of autoconfigurations is for you.
By adding module into classpath you will get stateful service like mariadb or kafka auto-started and available for connection from your application service w/o wiring any additional code.

## How to use
### Make sure you have spring boot and spring cloud in classpath
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
### If you do not use spring cloud - make it work for tests
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
### Add data service library
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
### Use produced properties in your configuration
### Example:
/src/test/resources/application.properties

```properties
spring.kafka.zookeperHost=${embedded.zookeeper.zookeeperConnect}
spring.kafka.brokerList=${embedded.kafka.brokerList}
```
 /src/test/resources/bootstrap.properties
```properties
embedded.kafka.topicsToCreate=some_topic
```

## List of properties per data service
### embedded-kafka
#### Consumes
* embedded.zookeeper.enabled `(true|false, default is true)`
* embedded.kafka.enabled `(true|false, default is true)`
* embedded.kafka.topicsToCreate `(comma separated list of topic names)`
* embedded.kafka.dockerImage `(default is set to kafka 0.11.x)`
  * To use kafka 0.10.x place "confluentinc/cp-kafka:3.2.2" or most recent [from 3.2.x branch](https://hub.docker.com/r/confluentinc/cp-kafka/tags/)
  * To use kafka 0.11.x place "confluentinc/cp-kafka:3.3.0" or most recent [from 3.3.x branch](https://hub.docker.com/r/confluentinc/cp-kafka/tags/)
#### Produces
* embedded.zookeeper.zookeeperConnect
* embedded.kafka.brokerList
### embedded-aerospike
#### Consumes
* aerospike [client library](https://mvnrepository.com/artifact/com.aerospike/aerospike-client) 
* embedded.aerospike.enabled `(true|false, default is true)`
#### Produces
* embedded.aerospike.host
* embedded.aerospike.port
* embedded.aerospike.namespace
* AerospikeTimeTravelService
  * timeTravelTo
  * nextDay
  * addDays
  * addHours
  * rollbackTime

