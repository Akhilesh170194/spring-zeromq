package com.aoneconsultancy.zeromq.annotation;

import com.aoneconsultancy.zeromq.config.ZmqListenerConfigUtils;
import com.aoneconsultancy.zeromq.config.ZmqListenerConfigurer;
import com.aoneconsultancy.zeromq.core.DefaultSocketEventListener;
import com.aoneconsultancy.zeromq.core.ZmqSocketMonitor;
import com.aoneconsultancy.zeromq.core.converter.MessageConverter;
import com.aoneconsultancy.zeromq.listener.ZmqListenerContainerFactory;
import com.aoneconsultancy.zeromq.listener.endpoint.MethodZmqListenerEndpoint;
import com.aoneconsultancy.zeromq.listener.endpoint.MultiMethodZmqListenerEndpoint;
import com.aoneconsultancy.zeromq.listener.endpoint.ZmqListenerEndpointRegistrar;
import com.aoneconsultancy.zeromq.listener.endpoint.ZmqListenerEndpointRegistry;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Registers methods annotated with {@link ZmqListener} to receive messages.
 * Similar to Spring AMQP's RabbitListenerAnnotationBeanPostProcessor,
 * this processes methods annotated with @ZmqListener and creates
 * listener containers for them.
 * 
 * Also supports class-level @ZmqListener annotations with methods
 * annotated with @ZmqHandler, similar to Spring AMQP's class-level
 * @RabbitListener with @RabbitHandler methods.
 */
@Slf4j
public class ZmqListenerAnnotationBeanPostProcessor implements BeanPostProcessor, Ordered, BeanFactoryAware,
        SmartInitializingSingleton {

    public static final String DEFAULT_ZMQ_LISTENER_CONTAINER_FACTORY_BEAN_NAME = "zmqListenerContainerFactory";

    private final AtomicInteger counter = new AtomicInteger();

    @Setter
    private String defaultContainerFactoryBeanName = DEFAULT_ZMQ_LISTENER_CONTAINER_FACTORY_BEAN_NAME;

    private BeanFactory beanFactory;

    private final ZmqListenerEndpointRegistrar registrar = new ZmqListenerEndpointRegistrar();

    @Setter
    private ZmqListenerEndpointRegistry endpointRegistry;

    private BeanExpressionResolver resolver = new StandardBeanExpressionResolver();

    private BeanExpressionContext expressionContext;

    private ZmqSocketMonitor.SocketEventListener defaultSocketEventListener;

    private final ConcurrentMap<Class<?>, TypeMetadata> typeCache = new ConcurrentHashMap<>();

    public ZmqListenerAnnotationBeanPostProcessor() {
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        if (beanFactory instanceof ConfigurableListableBeanFactory clbf) {
            this.resolver = clbf.getBeanExpressionResolver();
            this.expressionContext = new BeanExpressionContext(clbf, null);
        }

        // Create default socket event listener
        try {
            ApplicationEventPublisher publisher = beanFactory.getBean(ApplicationEventPublisher.class);
            this.defaultSocketEventListener = new DefaultSocketEventListener(publisher);
            log.debug("Created default socket event listener");
        } catch (BeansException e) {
            log.warn("Could not create default socket event listener: {}", e.getMessage());
        }
    }

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        final TypeMetadata metadata = this.typeCache.computeIfAbsent(targetClass, this::buildMetadata);

        // Process method-level @ZmqListener annotations
        for (ListenerMethod lm : metadata.listenerMethods) {
            for (ZmqListener zmqListener : lm.annotations()) {
                processListenerMethod(bean, lm.method(), beanName, zmqListener);
            }
        }

        // Process class-level @ZmqListener annotations with @ZmqHandler methods
        if (metadata.handlerMethods.length > 0) {
            processMultiMethodListeners(metadata.classAnnotations, metadata.handlerMethods, bean, beanName);
        }

        return bean;
    }

    private void processListenerMethod(Object bean, Method method, String beanName, ZmqListener listenerAnnotation) {
        method.setAccessible(true);

        Method methodToUse = checkProxy(method, bean);
        // Create a MethodEndpoint for the annotated method
        MethodZmqListenerEndpoint endpoint = new MethodZmqListenerEndpoint(bean, methodToUse);

        // Set a unique ID for the endpoint
        endpoint.setId(getEndpointId(listenerAnnotation));
        // Configure the endpoint with the annotation metadata
        List<String> addresses = List.of(listenerAnnotation.addresses());
        if (!addresses.isEmpty()) {
            endpoint.setAddresses(addresses);
        }
        endpoint.setSocketType(listenerAnnotation.socketType());
        endpoint.setConcurrency(listenerAnnotation.concurrency());
        endpoint.setMessageConverter(resolveMessageConverter(listenerAnnotation, bean, beanName));
        endpoint.setConsumerBatchEnabled(listenerAnnotation.batch());

        // Set default socket event listener if available
        if (this.defaultSocketEventListener != null) {
            endpoint.setSocketEventListener(this.defaultSocketEventListener);
        }

        ZmqListenerContainerFactory<?> factory = resolveContainerFactory(listenerAnnotation, methodToUse, beanName);

        // Register the endpoint with the registrar
        this.registrar.registerEndpoint(endpoint, factory);
    }

    private void processListenerMethod(Object bean, Method method, String beanName) {
        ZmqListener listenerAnnotation = method.getAnnotation(ZmqListener.class);
        processListenerMethod(bean, method, beanName, listenerAnnotation);
    }

    private void processMultiMethodListeners(ZmqListener[] classLevelListeners, Method[] multiMethods,
                                            Object bean, String beanName) {
        List<Method> checkedMethods = new ArrayList<>();
        Method defaultMethod = null;

        for (Method method : multiMethods) {
            Method checked = checkProxy(method, bean);
            if (AnnotationUtils.findAnnotation(method, ZmqHandler.class).isDefault()) {
                final Method toAssert = defaultMethod;
                Assert.state(toAssert == null, () -> "Only one @ZmqHandler can be marked 'isDefault', found: "
                        + toAssert.toString() + " and " + method.toString());
                defaultMethod = checked;
            }
            checkedMethods.add(checked);
        }

        for (ZmqListener classLevelListener : classLevelListeners) {
            MultiMethodZmqListenerEndpoint endpoint =
                    new MultiMethodZmqListenerEndpoint(checkedMethods, defaultMethod, bean);

            // Set a unique ID for the endpoint
            endpoint.setId(getEndpointId(classLevelListener));
            // Configure the endpoint with the annotation metadata
            List<String> addresses = List.of(classLevelListener.addresses());
            if (!addresses.isEmpty()) {
                endpoint.setAddresses(addresses);
            }
            endpoint.setSocketType(classLevelListener.socketType());
            endpoint.setConcurrency(classLevelListener.concurrency());
            endpoint.setMessageConverter(resolveMessageConverter(classLevelListener, bean, beanName));

            // Set default socket event listener if available
            if (this.defaultSocketEventListener != null) {
                endpoint.setSocketEventListener(this.defaultSocketEventListener);
            }

            ZmqListenerContainerFactory<?> factory = resolveContainerFactory(classLevelListener, bean, beanName);

            // Register the endpoint with the registrar
            this.registrar.registerEndpoint(endpoint, factory);
        }
    }

    private Method checkProxy(Method methodArg, Object bean) {
        Method method = methodArg;
        if (AopUtils.isJdkDynamicProxy(bean)) {
            try {
                // Found a @RabbitListener method on the target class for this JDK proxy ->
                // is it also present on the proxy itself?
                method = bean.getClass().getMethod(method.getName(), method.getParameterTypes());
                Class<?>[] proxiedInterfaces = ((Advised) bean).getProxiedInterfaces();
                for (Class<?> iface : proxiedInterfaces) {
                    try {
                        method = iface.getMethod(method.getName(), method.getParameterTypes());
                        break;
                    } catch (@SuppressWarnings("unused") NoSuchMethodException noMethod) {
                    }
                }
            } catch (SecurityException ex) {
                ReflectionUtils.handleReflectionException(ex);
            } catch (NoSuchMethodException ex) {
                throw new IllegalStateException(String.format(
                        "@RabbitListener method '%s' found on bean target class '%s', " +
                                "but not found in any interface(s) for a bean JDK proxy. Either " +
                                "pull the method up to an interface or switch to subclass (CGLIB) " +
                                "proxies by setting proxy-target-class/proxyTargetClass " +
                                "attribute to 'true'", method.getName(), method.getDeclaringClass().getSimpleName()), ex);
            }
        }
        return method;
    }

    @Nullable
    private ZmqListenerContainerFactory<?> resolveContainerFactory(ZmqListener rabbitListener,
                                                                   Object factoryTarget, String beanName) {
        ZmqListenerContainerFactory<?> factory = null;
        String containerFactoryBeanName = rabbitListener.containerFactory() == null ? "containerFactory" :
                rabbitListener.containerFactory();
        if (StringUtils.hasText(containerFactoryBeanName)) {
            Assert.state(this.beanFactory != null, "BeanFactory must be set to obtain container factory by bean name");
            try {
                factory = this.beanFactory.getBean(containerFactoryBeanName, ZmqListenerContainerFactory.class);
            } catch (NoSuchBeanDefinitionException ex) {
                throw new BeanInitializationException(
                        noBeanFoundMessage(factoryTarget, beanName, containerFactoryBeanName,
                                ZmqListenerContainerFactory.class), ex);
            }
        }
        return factory;
    }

    private MessageConverter resolveMessageConverter(ZmqListener zmqListener,
                                                     Object target, String beanName) {

        Object resolved = resolveExpression(zmqListener.messageConverter());
        if (resolved instanceof MessageConverter converter) {
            return converter;
        } else {
            String mcBeanName = resolveExpressionAsString(zmqListener.messageConverter(), "messageConverter");
            if (StringUtils.hasText(mcBeanName)) {
                Assert.state(this.beanFactory != null, "BeanFactory must be set to obtain container factory by bean name");
                try {
                    return this.beanFactory.getBean(mcBeanName, MessageConverter.class);
                } catch (NoSuchBeanDefinitionException ex) {
                    throw new BeanInitializationException(
                            noBeanFoundMessage(target, beanName, mcBeanName, MessageConverter.class), ex);
                }
            }
        }
        return null;
    }

    protected String resolveExpressionAsString(String value, String attribute) {
        Object resolved = resolveExpression(value);
        if (resolved instanceof String str) {
            return str;
        } else {
            throw new IllegalStateException("The [" + attribute + "] must resolve to a String. "
                    + "Resolved to [" + resolved.getClass() + "] for [" + value + "]");
        }
    }

    private Object resolveExpression(String value) {
        String resolvedValue = resolve(value);

        return this.resolver.evaluate(resolvedValue, this.expressionContext);
    }

    /**
     * Resolve the specified value if possible.
     *
     * @param value the value to resolve.
     * @return the resolved value.
     * @see ConfigurableBeanFactory#resolveEmbeddedValue
     */
    private String resolve(String value) {
        if (this.beanFactory != null && this.beanFactory instanceof ConfigurableBeanFactory cbf) {
            return cbf.resolveEmbeddedValue(value);
        }
        return value;
    }

    protected String noBeanFoundMessage(Object target, String listenerBeanName, String requestedBeanName,
                                        Class<?> expectedClass) {
        return "Could not register rabbit listener endpoint on ["
                + target + "] for bean " + listenerBeanName + ", no '" + expectedClass.getSimpleName() + "' with id '"
                + requestedBeanName + "' was found in the application context";
    }

    @Override
    public void afterSingletonsInstantiated() {

        this.registrar.setBeanFactory(this.beanFactory);

        if (this.beanFactory instanceof ListableBeanFactory lbf) {
            Map<String, ZmqListenerConfigurer> instances =
                    lbf.getBeansOfType(ZmqListenerConfigurer.class);
            for (ZmqListenerConfigurer configurer : instances.values()) {
                configurer.configureZmqListeners(this.registrar);
            }
        }

        if (this.registrar.getEndpointRegistry() == null) {
            if (this.endpointRegistry == null) {
                Assert.state(this.beanFactory != null,
                        "BeanFactory must be set to find endpoint registry by bean name");
                this.endpointRegistry = this.beanFactory.getBean(
                        ZmqListenerConfigUtils.ZMQ_LISTENER_ENDPOINT_REGISTRY_BEAN_NAME,
                        ZmqListenerEndpointRegistry.class);
            }
            this.registrar.setEndpointRegistry(this.endpointRegistry);
        }

        if (this.defaultContainerFactoryBeanName != null) {
            this.registrar.setContainerFactoryBeanName(this.defaultContainerFactoryBeanName);
        }

        // Actually register all listeners
        this.registrar.afterPropertiesSet();

        // clear the cache - prototype beans will be re-cached.
        this.typeCache.clear();
    }

    private TypeMetadata buildMetadata(Class<?> targetClass) {
        List<ZmqListener> classLevelListeners = findListenerAnnotations(targetClass);
        final boolean hasClassLevelListeners = classLevelListeners.size() > 0;
        final List<ListenerMethod> methods = new ArrayList<>();
        final List<Method> multiMethods = new ArrayList<>();

        ReflectionUtils.doWithMethods(targetClass, method -> {
            List<ZmqListener> listenerAnnotations = findListenerAnnotations(method);
            if (listenerAnnotations.size() > 0) {
                methods.add(new ListenerMethod(method,
                        listenerAnnotations.toArray(new ZmqListener[0])));
            }
            if (hasClassLevelListeners) {
                ZmqHandler zmqHandler = AnnotationUtils.findAnnotation(method, ZmqHandler.class);
                if (zmqHandler != null) {
                    multiMethods.add(method);
                }
            }
        }, ReflectionUtils.USER_DECLARED_METHODS);

        if (methods.isEmpty() && multiMethods.isEmpty()) {
            return TypeMetadata.EMPTY;
        }

        return new TypeMetadata(
                methods.toArray(new ListenerMethod[0]),
                multiMethods.toArray(new Method[0]),
                classLevelListeners.toArray(new ZmqListener[0]));
    }

    private List<ZmqListener> findListenerAnnotations(AnnotatedElement element) {
        ZmqListener ann = AnnotationUtils.findAnnotation(element, ZmqListener.class);
        if (ann == null) {
            return new ArrayList<>();
        }

        // Check for @ZmqListeners
        ZmqListeners anns = AnnotationUtils.findAnnotation(element, ZmqListeners.class);
        if (anns != null) {
            return Arrays.asList(anns.value());
        }

        return Arrays.asList(ann);
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }

    private String getEndpointId(ZmqListener zmqListener) {
        if (StringUtils.hasText(zmqListener.id())) {
            return resolveExpressionAsString(zmqListener.id(), "id");
        } else {
            return ZmqListenerConfigUtils.ZMQ_LISTENER_ANNOTATION_ENDPOINT_CONTAINER_BEAN_NAME + "#" + this.counter.getAndIncrement();
        }
    }

    // The ZmqHandlerMethodFactoryAdapter inner class has been removed
    // as we now use direct method invocation instead of Spring Messaging

    /**
     * The metadata holder of the class with {@link ZmqListener}
     * and {@link ZmqHandler} annotations.
     */
    private static class TypeMetadata {

        /**
         * Methods annotated with {@link ZmqListener}.
         */
        final ListenerMethod[] listenerMethods; // NOSONAR

        /**
         * Methods annotated with {@link ZmqHandler}.
         */
        final Method[] handlerMethods; // NOSONAR

        /**
         * Class level {@link ZmqListener} annotations.
         */
        final ZmqListener[] classAnnotations; // NOSONAR

        static final TypeMetadata EMPTY = new TypeMetadata();

        private TypeMetadata() {
            this.listenerMethods = new ListenerMethod[0];
            this.handlerMethods = new Method[0];
            this.classAnnotations = new ZmqListener[0];
        }

        TypeMetadata(ListenerMethod[] methods, Method[] multiMethods, ZmqListener[] classLevelListeners) { // NOSONAR
            this.listenerMethods = methods; // NOSONAR
            this.handlerMethods = multiMethods; // NOSONAR
            this.classAnnotations = classLevelListeners; // NOSONAR
        }
    }

    /**
     * A method annotated with {@link ZmqListener}, together with the annotations.
     *
     * @param method      the method with annotations
     * @param annotations on the method
     */
    private record ListenerMethod(Method method, ZmqListener[] annotations) {
    }
}
