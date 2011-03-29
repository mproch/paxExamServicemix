package pl.touk.smx4.paxexam;

import org.apache.camel.builder.RouteBuilder;

public class TestRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        from("jbi:service:http://touk.pl/smx4/paxexam/service1")
            .to("mock:service1").setBody().constant("<response xmlns='http://touk.pl/smx4/paxexam'>response</response>");


    }
}
