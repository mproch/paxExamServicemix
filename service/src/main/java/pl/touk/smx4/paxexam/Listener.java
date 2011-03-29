package pl.touk.smx4.paxexam;

import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.api.Status;
import org.apache.servicemix.nmr.api.event.ExchangeListener;

import java.util.HashMap;
import java.util.Map;

public class Listener implements ExchangeListener {

    public final Map<String, Object> exchanges = new HashMap<String, Object>();

    @Override
    public void exchangeSent(Exchange exchange) {
    }

    @Override
    public void exchangeDelivered(Exchange exchange) {
        if (exchange.getStatus() == Status.Done) {
            exchanges.put(exchange.getId(), exchange.getIn().getBody());
        }
    }

    @Override
    public void exchangeFailed(Exchange exchange) {
    }
}
