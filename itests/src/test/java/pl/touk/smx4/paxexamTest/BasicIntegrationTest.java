/*
 * Copyright by Touk (c) 2011
 */

package pl.touk.smx4.paxexamTest;

import org.apache.camel.CamelContext;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.nmr.api.*;
import org.apache.servicemix.nmr.core.ExchangeImpl;
import org.apache.servicemix.nmr.core.util.StringSource;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import javax.xml.transform.Source;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.*;

@RunWith(JUnit4TestRunner.class)
public abstract class BasicIntegrationTest {

    @Inject
    protected BundleContext bundleContext;

    NMR nmr;

    Map<String, CamelContext> ctx = new HashMap<String, CamelContext>();

    protected SourceTransformer sourceTransformer = new SourceTransformer();

    @Configuration
    public static Option[] configuration() throws Exception {

        String resources = BasicIntegrationTest.class.getResource("/").getFile();

        System.setProperty("org.ops4j.pax.runner.platform.ee", "file://" + resources + "etc/jre.properties");
        String karafVersion = "2.1.3";

        return options(
                workingDirectory(resources + "paxExam"),

                systemProperty("karaf.startLocalConsole").value("false"),
                systemProperty("karaf.home").value(resources),

                systemProperty("karaf.startRemoteShell").value("false"),
                systemProperty("felix.fileinstall.dir").value(resources + "etc"),
                systemProperty("felix.fileinstall.filter").value(".*\\.cfg"),
                systemProperty("felix.fileinstall.poll").value("1000"),
                systemProperty("felix.fileinstall.noInitialDelay").value("true"),

                systemProperty("javax.xml.validation.SchemaFactory:http://www.w3.org/2001/XMLSchema")
                        .value("com.sun.org.apache.xerces.internal.jaxp.validation.xs.SchemaFactoryImpl"),

                felix(),
                logProfile(),

                profile("felix.fileinstall"),
                profile("spring.dm", "1.2.0"),
                profile("spring"),
                mavenBundle("org.apache.felix", "org.apache.felix.configadmin").startLevel(2),
                mavenBundle("org.apache.felix", "org.apache.felix.fileinstall").startLevel(2),

                mavenBundle("org.apache.karaf.shell", "org.apache.karaf.shell.console", karafVersion).startLevel(3),
                mavenBundle("org.apache.karaf", "org.apache.karaf.management", karafVersion).startLevel(3),
                mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint", "0.2-incubating").startLevel(2),

                mavenBundle("org.apache.xbean", "xbean-blueprint", "3.7").startLevel(4),
                mavenBundle("org.apache.commons", "commons-jexl", "2.0.1").startLevel(4),
                mavenBundle("org.apache.servicemix.specs", "org.apache.servicemix.specs.scripting-api-1.0", "1.7.0").startLevel(4),

                scanFeatures(
                        maven().groupId("org.apache.servicemix")
                                .artifactId("apache-servicemix")
                                .type("xml").classifier("features")
                                .version("4.3.0"),
                        "servicemix-camel", "servicemix-cxf-bc", "camel-nmr"
                ).startLevel(4),

                mavenBundle("pl.touk.smx4", "paxExamSample-service")
        );
    }


    protected <T> T getBean(Class<T> klazz, String beanName) throws Exception {
        ServiceTracker tracker = new ServiceTracker(bundleContext, bundleContext.createFilter(
                "(&(objectClass=" + klazz.getName() + ")(org.springframework.osgi.bean.name=" + beanName + "))"), null);
        tracker.open();
        T bean = (T) tracker.waitForService(10000);
        tracker.close();
        assertNotNull(bean);
        return bean;
    }

    protected NMR getNMR() throws Exception {
        if (nmr == null) {
            nmr = getBean(NMR.class);
            Thread.sleep(3000);
        }
        return nmr;
    }


    protected CamelContext getCamelContext(String id) throws Exception {
        if (!ctx.containsKey(id)) {
            ctx.put(id, getBean(CamelContext.class, id));
        }
        return ctx.get(id);
    }

    protected <T> T getBean(Class<T> klazz) throws Exception {
        ServiceTracker tracker = new ServiceTracker(bundleContext, klazz.getName(), null);
        tracker.open();
        T ret = (T) tracker.waitForService(5000);
        tracker.close();
        return ret;

    }


    protected Source sendMessage(String service, String message) throws Exception {
        NMR nmr = getNMR();
        assertNotNull(nmr);
        ExchangeImpl ei = new ExchangeImpl(Pattern.InOut);

        ei.getIn().setBody(new StringSource(message));

        Reference ref = nmr.getEndpointRegistry().lookup(Collections.singletonMap("SERVICE_NAME", service));

        assertNotNull(ref);
        ei.setTarget(ref);
        Channel channel = nmr.createChannel();
        channel.sendSync(ei);


        Source out = ei.getOut().getBody(Source.class);

        ei.setStatus(Status.Done);
        channel.send(ei);
        channel.close();
        return out;
    }


}
