package com.playtika.testcontainer.nativekafka;

import com.playtika.testcontainer.nativekafka.properties.NativeKafkaConfigurationProperties;
import com.playtika.testcontainer.nativekafka.properties.NativeKafkaConfigurationProperties.TopicConfiguration;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.errors.TopicExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.kafka.KafkaContainer;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NativeKafkaTopicsConfigurer Tests")
class NativeKafkaTopicsConfigurerTest {

    @Mock
    private KafkaContainer nativeKafka;

    @Mock
    private NativeKafkaConfigurationProperties properties;

    @Mock
    private AdminClient adminClient;

    @Mock
    private CreateTopicsResult createTopicsResult;

    @Mock
    private KafkaFuture<Void> future;

    @Captor
    private ArgumentCaptor<Collection<NewTopic>> newTopicsCaptor;

    private NativeKafkaTopicsConfigurer configurer;

    @BeforeEach
    void setUp() {
        configurer = new NativeKafkaTopicsConfigurer(nativeKafka, properties);
    }

    @Test
    @DisplayName("afterPropertiesSet should call createTopics with configuration from properties")
    void shouldCallCreateTopicsOnAfterPropertiesSet() {
        Collection<String> topicsToCreate = Collections.emptyList();
        Collection<TopicConfiguration> topicsConfiguration = Collections.emptyList();

        when(properties.getTopicsToCreate()).thenReturn(topicsToCreate);
        when(properties.getTopicsConfiguration()).thenReturn(topicsConfiguration);

        configurer.afterPropertiesSet();

        verify(properties).getTopicsToCreate();
        verify(properties).getTopicsConfiguration();
    }

    @Test
    @DisplayName("should create topics with default partitions when no custom configuration provided")
    void shouldCreateTopicsWithDefaultPartitions() {
        when(nativeKafka.getBootstrapServers()).thenReturn("localhost:9092");

        try (MockedStatic<AdminClient> adminClientMock = mockStatic(AdminClient.class)) {
            adminClientMock.when(() -> AdminClient.create(any(Properties.class))).thenReturn(adminClient);
            when(adminClient.createTopics(anyCollection())).thenReturn(createTopicsResult);
            when(createTopicsResult.all()).thenReturn(future);

            Collection<String> topicsToCreate = Arrays.asList("topic1", "topic2");
            Collection<TopicConfiguration> topicsConfiguration = Collections.emptyList();

            configurer.createTopics(topicsToCreate, topicsConfiguration);

            verify(adminClient).createTopics(newTopicsCaptor.capture());
            Collection<NewTopic> createdTopics = newTopicsCaptor.getValue();

            assertThat(createdTopics).hasSize(2);
            assertThat(createdTopics).extracting(NewTopic::name).containsExactlyInAnyOrder("topic1", "topic2");
            assertThat(createdTopics).extracting(NewTopic::numPartitions).allMatch(partitions -> partitions == 1);
        }
    }

    @Test
    @DisplayName("should create topics with custom partitions when configuration provided")
    void shouldCreateTopicsWithCustomPartitions() {
        when(nativeKafka.getBootstrapServers()).thenReturn("localhost:9092");

        try (MockedStatic<AdminClient> adminClientMock = mockStatic(AdminClient.class)) {
            adminClientMock.when(() -> AdminClient.create(any(Properties.class))).thenReturn(adminClient);
            when(adminClient.createTopics(anyCollection())).thenReturn(createTopicsResult);
            when(createTopicsResult.all()).thenReturn(future);

            Collection<String> topicsToCreate = Arrays.asList("topic1");
            Collection<TopicConfiguration> topicsConfiguration = Arrays.asList(
                    new TopicConfiguration("topic1", 5),
                    new TopicConfiguration("topic2", 3)
            );

            configurer.createTopics(topicsToCreate, topicsConfiguration);

            verify(adminClient).createTopics(newTopicsCaptor.capture());
            Collection<NewTopic> createdTopics = newTopicsCaptor.getValue();

            assertThat(createdTopics).hasSize(2);

            NewTopic topic1 = createdTopics.stream()
                    .filter(topic -> "topic1".equals(topic.name()))
                    .findFirst()
                    .orElseThrow();
            assertThat(topic1.numPartitions()).isEqualTo(5);

            NewTopic topic2 = createdTopics.stream()
                    .filter(topic -> "topic2".equals(topic.name()))
                    .findFirst()
                    .orElseThrow();
            assertThat(topic2.numPartitions()).isEqualTo(3);
        }
    }

    @Test
    @DisplayName("should merge topics to create with custom configuration, prioritizing custom config")
    void shouldMergeTopicsWithCustomConfigurationTakingPrecedence() {
        when(nativeKafka.getBootstrapServers()).thenReturn("localhost:9092");

        try (MockedStatic<AdminClient> adminClientMock = mockStatic(AdminClient.class)) {
            adminClientMock.when(() -> AdminClient.create(any(Properties.class))).thenReturn(adminClient);
            when(adminClient.createTopics(anyCollection())).thenReturn(createTopicsResult);
            when(createTopicsResult.all()).thenReturn(future);

            Collection<String> topicsToCreate = Arrays.asList("topic1", "topic2");
            Collection<TopicConfiguration> topicsConfiguration = Arrays.asList(
                    new TopicConfiguration("topic1", 10)
            );

            configurer.createTopics(topicsToCreate, topicsConfiguration);

            verify(adminClient).createTopics(newTopicsCaptor.capture());
            Collection<NewTopic> createdTopics = newTopicsCaptor.getValue();

            assertThat(createdTopics).hasSize(2);

            NewTopic topic1 = createdTopics.stream()
                    .filter(topic -> "topic1".equals(topic.name()))
                    .findFirst()
                    .orElseThrow();
            assertThat(topic1.numPartitions()).isEqualTo(10);

            NewTopic topic2 = createdTopics.stream()
                    .filter(topic -> "topic2".equals(topic.name()))
                    .findFirst()
                    .orElseThrow();
            assertThat(topic2.numPartitions()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("should not create topics when both collections are empty")
    void shouldNotCreateTopicsWhenEmpty() {
        try (MockedStatic<AdminClient> adminClientMock = mockStatic(AdminClient.class)) {
            configurer.createTopics(Collections.emptyList(), Collections.emptyList());

            adminClientMock.verify(() -> AdminClient.create(any(Properties.class)), never());
            verify(adminClient, never()).createTopics(anyCollection());
        }
    }

    @Test
    @DisplayName("should handle execution exception during topic creation")
    void shouldHandleExecutionExceptionDuringTopicCreation() throws Exception {
        when(nativeKafka.getBootstrapServers()).thenReturn("localhost:9092");

        try (MockedStatic<AdminClient> adminClientMock = mockStatic(AdminClient.class)) {
            adminClientMock.when(() -> AdminClient.create(any(Properties.class))).thenReturn(adminClient);
            when(adminClient.createTopics(anyCollection())).thenReturn(createTopicsResult);
            when(createTopicsResult.all()).thenReturn(future);
            when(future.get()).thenThrow(new ExecutionException(new TopicExistsException("Topic exists")));

            Collection<String> topicsToCreate = Arrays.asList("topic1");
            Collection<TopicConfiguration> topicsConfiguration = Collections.emptyList();

            assertThatThrownBy(() -> configurer.createTopics(topicsToCreate, topicsConfiguration))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Failed to create topics")
                    .hasCauseInstanceOf(ExecutionException.class);
        }
    }

    @Test
    @DisplayName("should handle interrupted exception during topic creation")
    void shouldHandleInterruptedExceptionDuringTopicCreation() throws Exception {
        when(nativeKafka.getBootstrapServers()).thenReturn("localhost:9092");

        try (MockedStatic<AdminClient> adminClientMock = mockStatic(AdminClient.class)) {
            adminClientMock.when(() -> AdminClient.create(any(Properties.class))).thenReturn(adminClient);
            when(adminClient.createTopics(anyCollection())).thenReturn(createTopicsResult);
            when(createTopicsResult.all()).thenReturn(future);
            when(future.get()).thenThrow(new InterruptedException("Interrupted"));

            Collection<String> topicsToCreate = Arrays.asList("topic1");
            Collection<TopicConfiguration> topicsConfiguration = Collections.emptyList();

            assertThatThrownBy(() -> configurer.createTopics(topicsToCreate, topicsConfiguration))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Failed to create topics")
                    .hasCauseInstanceOf(InterruptedException.class);
        }
    }

    @Test
    @DisplayName("should create topics with replication factor of 1")
    void shouldCreateTopicsWithReplicationFactorOne() {
        when(nativeKafka.getBootstrapServers()).thenReturn("localhost:9092");

        try (MockedStatic<AdminClient> adminClientMock = mockStatic(AdminClient.class)) {
            adminClientMock.when(() -> AdminClient.create(any(Properties.class))).thenReturn(adminClient);
            when(adminClient.createTopics(anyCollection())).thenReturn(createTopicsResult);
            when(createTopicsResult.all()).thenReturn(future);

            Collection<String> topicsToCreate = Arrays.asList("topic1");
            Collection<TopicConfiguration> topicsConfiguration = Collections.emptyList();

            configurer.createTopics(topicsToCreate, topicsConfiguration);

            verify(adminClient).createTopics(newTopicsCaptor.capture());
            Collection<NewTopic> createdTopics = newTopicsCaptor.getValue();

            assertThat(createdTopics).hasSize(1);
            NewTopic topic = createdTopics.iterator().next();
            assertThat(topic.replicationFactor()).isEqualTo((short) 1);
        }
    }

    @Test
    @DisplayName("should only create topics from configuration when no topics to create specified")
    void shouldCreateOnlyConfiguredTopicsWhenNoTopicsToCreateSpecified() {
        when(nativeKafka.getBootstrapServers()).thenReturn("localhost:9092");

        try (MockedStatic<AdminClient> adminClientMock = mockStatic(AdminClient.class)) {
            adminClientMock.when(() -> AdminClient.create(any(Properties.class))).thenReturn(adminClient);
            when(adminClient.createTopics(anyCollection())).thenReturn(createTopicsResult);
            when(createTopicsResult.all()).thenReturn(future);

            Collection<String> topicsToCreate = Collections.emptyList();
            Collection<TopicConfiguration> topicsConfiguration = Arrays.asList(
                    new TopicConfiguration("configuredTopic", 2)
            );

            configurer.createTopics(topicsToCreate, topicsConfiguration);

            verify(adminClient).createTopics(newTopicsCaptor.capture());
            Collection<NewTopic> createdTopics = newTopicsCaptor.getValue();

            assertThat(createdTopics).hasSize(1);
            NewTopic topic = createdTopics.iterator().next();
            assertThat(topic.name()).isEqualTo("configuredTopic");
            assertThat(topic.numPartitions()).isEqualTo(2);
        }
    }
}