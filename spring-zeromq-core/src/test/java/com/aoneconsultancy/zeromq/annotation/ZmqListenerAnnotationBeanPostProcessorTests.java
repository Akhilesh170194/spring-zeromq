package com.aoneconsultancy.zeromq.annotation;

import static org.assertj.core.api.Assertions.assertThat;

import com.aoneconsultancy.zeromq.config.MessageListenerTestContainer;
import com.aoneconsultancy.zeromq.config.ZmqListenerContainerTestFactory;
import com.aoneconsultancy.zeromq.listener.PullZmqMessageListenerContainer;
import com.aoneconsultancy.zeromq.listener.endpoint.AbstractZmqListenerEndpoint;
import com.aoneconsultancy.zeromq.listener.endpoint.MethodZmqListenerEndpoint;
import com.aoneconsultancy.zeromq.listener.endpoint.ZmqListenerEndpoint;
import com.aoneconsultancy.zeromq.listener.endpoint.ZmqListenerEndpointRegistry;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Iterator;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;
import org.zeromq.SocketType;
import org.zeromq.ZContext;

/**
 * Tests for {@link ZmqListenerAnnotationBeanPostProcessor}.
 * <p>
 * This class is similar to Spring AMQP's RabbitListenerAnnotationBeanPostProcessorTests.
 */
public class ZmqListenerAnnotationBeanPostProcessorTests {

    protected Class<?> getConfigClass() {
        return Config.class;
    }

    @Test
    public void simpleMessageListener() {
        ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
                getConfigClass(), SimpleMessageListenerTestBean.class);

        ZmqListenerContainerTestFactory factory = context.getBean(ZmqListenerContainerTestFactory.class);
        assertThat(factory.getListenerContainers().size()).as("One container should have been registered").isEqualTo(1);
        MessageListenerTestContainer container = factory.getListenerContainers().get(0);

        ZmqListenerEndpoint endpoint = container.getEndpoint();
        assertThat(endpoint.getClass()).as("Wrong endpoint type").isEqualTo(MethodZmqListenerEndpoint.class);
        MethodZmqListenerEndpoint methodEndpoint = (MethodZmqListenerEndpoint) endpoint;
        assertThat(methodEndpoint.getBean()).isNotNull();
        assertThat(methodEndpoint.getMethod()).isNotNull();

        PullZmqMessageListenerContainer listenerContainer = new PullZmqMessageListenerContainer(new ZContext());
        listenerContainer.setReceiveTimeout(10);
        methodEndpoint.setupListenerContainer(listenerContainer);
        assertThat(listenerContainer.getMessageListener()).isNotNull();

        assertThat(container.isStarted()).as("Should have been started " + container).isTrue();
        context.close(); // Close and stop the listeners
        assertThat(container.isStopped()).as("Should have been stopped " + container).isTrue();
    }

    @Test
    public void simpleMessageListenerWithMixedAnnotations() {
        ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
                getConfigClass(), SimpleMessageListenerWithMixedAnnotationsTestBean.class);

        ZmqListenerContainerTestFactory factory = context.getBean(ZmqListenerContainerTestFactory.class);
        assertThat(factory.getListenerContainers().size()).as("One container should have been registered").isEqualTo(1);
        MessageListenerTestContainer container = factory.getListenerContainers().get(0);

        ZmqListenerEndpoint endpoint = container.getEndpoint();
        assertThat(endpoint.getClass()).as("Wrong endpoint type").isEqualTo(MethodZmqListenerEndpoint.class);
        MethodZmqListenerEndpoint methodEndpoint = (MethodZmqListenerEndpoint) endpoint;
        assertThat(methodEndpoint.getBean()).isNotNull();
        assertThat(methodEndpoint.getMethod()).isNotNull();

        Iterator<String> iterator = ((MethodZmqListenerEndpoint) endpoint).getEndpoints().iterator();
        assertThat(iterator.next()).isEqualTo("tcp://localhost:5555");
        assertThat(iterator.next()).isEqualTo("tcp://localhost:5556");

        PullZmqMessageListenerContainer listenerContainer = new PullZmqMessageListenerContainer(new ZContext());
        listenerContainer.setReceiveTimeout(10);
        methodEndpoint.setupListenerContainer(listenerContainer);
        assertThat(listenerContainer.getMessageListener()).isNotNull();

        assertThat(container.isStarted()).as("Should have been started " + container).isTrue();
        context.close(); // Close and stop the listeners
        assertThat(container.isStopped()).as("Should have been stopped " + container).isTrue();
    }

    @Test
    public void metaAnnotationIsDiscovered() {
        ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
                getConfigClass(), MetaAnnotationTestBean.class);

        ZmqListenerContainerTestFactory factory = context.getBean(ZmqListenerContainerTestFactory.class);
        assertThat(factory.getListenerContainers().size()).as("one container should have been registered").isEqualTo(2);
        ZmqListenerEndpoint endpoint = factory.getListenerContainers().get(0).getEndpoint();
        assertThat(Objects.requireNonNull(endpoint.getEndpoints())
                .iterator()
                .next())
                .isEqualTo("tcp://localhost:5557");
        endpoint = factory.getListenerContainers().get(1).getEndpoint();
        assertThat(Objects.requireNonNull(endpoint.getEndpoints())
                .iterator()
                .next())
                .isEqualTo("tcp://localhost:5558");

        context.close();
    }

    @Test
    public void metaAnnotationIsDiscoveredClassLevel() {
        ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
                Config.class, MetaAnnotationTestBean2.class);

        ZmqListenerContainerTestFactory factory = context.getBean(ZmqListenerContainerTestFactory.class);
        assertThat(factory.getListenerContainers().size()).as("one container should have been registered").isEqualTo(2);
        ZmqListenerEndpoint endpoint = factory.getListenerContainers().get(0).getEndpoint();
        assertThat(Objects.requireNonNull(endpoint.getEndpoints())
                .iterator()
                .next())
                .isEqualTo("tcp://localhost:5559");
        endpoint = factory.getListenerContainers().get(1).getEndpoint();
        assertThat(Objects.requireNonNull(endpoint.getEndpoints())
                .iterator()
                .next())
                .isEqualTo("tcp://localhost:5560");

        context.close();
    }

    @Test
    public void multipleEndpointsTestBean() {
        ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
                getConfigClass(), MultipleAddressesTestBean.class);

        ZmqListenerContainerTestFactory factory = context.getBean(ZmqListenerContainerTestFactory.class);
        assertThat(factory.getListenerContainers().size()).as("one container should have been registered").isEqualTo(1);
        ZmqListenerEndpoint endpoint = factory.getListenerContainers().get(0).getEndpoint();
        final Iterator<String> iterator = ((AbstractZmqListenerEndpoint) endpoint).getEndpoints().iterator();
        assertThat(iterator.next()).isEqualTo("tcp://localhost:5557");
        assertThat(iterator.next()).isEqualTo("tcp://localhost:5555");

        context.close();
    }

    @Test
    public void propertyResolvingToExpressionTestBean() {
        ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
                getConfigClass(), PropertyResolvingToExpressionTestBean.class);

        ZmqListenerContainerTestFactory factory = context.getBean(ZmqListenerContainerTestFactory.class);
        assertThat(factory.getListenerContainers().size()).as("one container should have been registered").isEqualTo(1);
        ZmqListenerEndpoint endpoint = factory.getListenerContainers().get(0).getEndpoint();
        final Iterator<String> iterator = ((AbstractZmqListenerEndpoint) endpoint).getEndpoints().iterator();
        assertThat(iterator.next()).isEqualTo("tcp://localhost:5555");
        assertThat(iterator.next()).isEqualTo("tcp://localhost:5556");

        context.close();
    }

    @Test
    public void invalidValueInAnnotationTestBean() {
        try {
            new AnnotationConfigApplicationContext(getConfigClass(), InvalidValueInAnnotationTestBean.class).close();
        } catch (BeanCreationException e) {
            assertThat(e.getCause()).isInstanceOf(IllegalStateException.class);
            assertThat(e.getMessage()).contains("The [endpoints] must resolve to a String.");
        }
    }

    @Component
    static class SimpleMessageListenerTestBean {

        @ZmqListener(endpoints = "tcp://localhost:5555")
        public void handleIt(String body) {
        }

    }

    @Component
    static class SimpleMessageListenerWithMixedAnnotationsTestBean {

        @ZmqListener(endpoints = {"tcp://localhost:5555", "#{mySecondAddress}"})
        public void handleIt(String body) {
        }

    }

    @Component
    static class MetaAnnotationTestBean {

        @FooListener("tcp://localhost:5557")
        @FooListener("tcp://localhost:5558")
        public void handleIt(String body) {
        }

    }

    @Component
    @FooListener("tcp://localhost:5559")
    @FooListener("tcp://localhost:5560")
    static class MetaAnnotationTestBean2 {

        @ZmqHandler
        public void handleIt(String body) {
        }

    }

    @ZmqListener(socketType = SocketType.PULL)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Repeatable(FooListeners.class)
    @interface FooListener {

        @AliasFor(annotation = ZmqListener.class, attribute = "endpoints")
        String[] value() default {};

    }

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @interface FooListeners {

        FooListener[] value();

    }

    @Component
    static class MultipleAddressesTestBean {

        @ZmqListener(endpoints = {"tcp://localhost:5557", "#{@myTestAddress}"})
        public void handleIt(String body) {
        }
    }

    @Component
    static class PropertyResolvingToExpressionTestBean {

        @ZmqListener(endpoints = {"${myAddressExpression}", "#{@mySecondAddress}"})
        public void handleIt(String body) {
        }
    }

    @Component
    static class InvalidValueInAnnotationTestBean {

        @ZmqListener(endpoints = "#{@testFactory}")
        public void handleIt(String body) {
        }
    }

    @Configuration
    @PropertySource("classpath:/com/aoneconsultancy/zeromq/annotation/zmq-annotation.properties")
    static class Config {

        @Bean
        public ZmqListenerAnnotationBeanPostProcessor postProcessor() {
            ZmqListenerAnnotationBeanPostProcessor postProcessor = new ZmqListenerAnnotationBeanPostProcessor();
            postProcessor.setEndpointRegistry(zmqListenerEndpointRegistry());
            postProcessor.setDefaultContainerFactoryBeanName("testFactory");
            return postProcessor;
        }

        @Bean
        public ZmqListenerEndpointRegistry zmqListenerEndpointRegistry() {
            return new ZmqListenerEndpointRegistry();
        }

        @Bean
        public ZmqListenerContainerTestFactory testFactory() {
            return new ZmqListenerContainerTestFactory();
        }

        @Bean
        public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
            return new PropertySourcesPlaceholderConfigurer();
        }

        @Bean
        public ApplicationEventPublisher applicationEventPublisher() {
            return event -> { /* no-op or log if desired */ };
        }

        @Bean
        public String myTestAddress() {
            return "tcp://localhost:5555";
        }

        @Bean
        public String mySecondAddress() {
            return "tcp://localhost:5556";
        }

    }

}