package com.playtika.test.kafka.camel;

import com.playtika.test.kafka.AbstractEmbeddedKafkaTest;
import com.playtika.test.kafka.camel.samples.SampleRouteConfiguration;
import com.playtika.test.kafka.camel.samples.SampleTestRouteMonitor;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.ServiceStatus;
import org.apache.camel.component.kafka.KafkaConstants;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import static com.playtika.test.kafka.properties.KafkaConfigurationProperties.KAFKA_BEAN_NAME;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Order(5)
public class CamelRouteTest extends AbstractEmbeddedKafkaTest {

    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

    @Autowired
    private ProducerTemplate producerTemplate;

    @Autowired
    private SampleRouteConfiguration routeConfiguration;

    private CamelContext camelContext;
    private SampleTestRouteMonitor routeMonitor;

    @Autowired
    public void setCamelContext(CamelContext camelContext) throws Exception {
        this.camelContext = camelContext;
        this.routeMonitor = new SampleTestRouteMonitor(camelContext);
    }

    @Test
    public void should_sendAndReceiveMessage() throws InterruptedException {
        String message = "this is a test!";
        producerTemplate.sendBodyAndHeader(routeConfiguration.camelTopicEndpoint(), message, KafkaConstants.KEY, "12345678");

        routeMonitor.getResultEndpoint().setResultWaitTime(15000);
        routeMonitor.getResultEndpoint().expectedBodiesReceived(message);
        routeMonitor.getResultEndpoint().assertIsSatisfied();
    }

    @Test
    public void shouldSetupDependsOnForCamel() {
        String[] beanNamesForType = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, CamelContext.class);
        assertThat(beanNamesForType)
                .as("CamelContext should be present")
                .hasSize(1);
        asList(beanNamesForType).forEach(this::hasDependsOn);
        assertEquals(ServiceStatus.Started, camelContext.getStatus());
    }

    private void hasDependsOn(String beanName) {
        assertThat(beanFactory.getBeanDefinition(beanName).getDependsOn())
                .isNotNull()
                .isNotEmpty()
                .contains(KAFKA_BEAN_NAME);
    }

}