package com.playtika.testcontainer.kafka.camel.samples;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.ProcessDefinition;
import org.apache.camel.model.RouteDefinition;

import static com.playtika.testcontainer.kafka.camel.samples.SampleProductionRouteContext.PRODUCTION_ROUTE;

public class SampleTestRouteMonitor {

    private final MockEndpoint resultEndpoint;

    public SampleTestRouteMonitor(CamelContext camelContext) throws Exception {
        ModelCamelContext modelCamelContext = camelContext.adapt(ModelCamelContext.class);
        RouteDefinition route = modelCamelContext.getRouteDefinition(PRODUCTION_ROUTE);
        AdviceWith.adviceWith(route, modelCamelContext, new AdviceWithRouteBuilder() {
            @Override
            public void configure() {
                weaveByType(ProcessDefinition.class)
                        .before()
                        .to("mock:end");
            }
        });
        resultEndpoint = camelContext.getEndpoint("mock:end", MockEndpoint.class);
    }

    public MockEndpoint getResultEndpoint() {
        return resultEndpoint;
    }
}
