package com.sample;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;

import java.util.Arrays;
import java.util.List;

public class LoadBalanceRouteBuilder extends RouteBuilder {
    private final String baseUrl = "http://localhost:8080";
    List<String> servers = Arrays.asList(
            "/error01",
            "/error02",
            "/ok03");

    @Override
    public void configure() throws Exception {
        onException(Exception.class)
                .maximumRedeliveries(2)
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .continued(true)
                .setHeader("Exchange.HTTP_RESPONSE_CODE", constant(403))
                .setBody(constant("Error message set by onException"));

        from("timer:foo?repeatCount=1")
                .to("direct:start")
                .log("Finish: ${body}");

        from("direct:start")
                .loadBalance().failover(5, false, true, true)
                    .to(baseUrl + servers.get(0))
                    .to(baseUrl + servers.get(1))
                    .to(baseUrl + servers.get(2))
                .end();

        rest(servers.get(0))
                .get()
                .to("direct:error");
        rest(servers.get(1))
                .get()
                .to("direct:error");
        rest(servers.get(2))
                .get()
                .to("direct:ok");

        from("direct:error")
                .process(new ExceptionProcessor());

        from("direct:ok")
                .setBody(constant("OK"));
    }
}