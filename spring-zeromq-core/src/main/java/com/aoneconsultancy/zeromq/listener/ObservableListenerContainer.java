package com.aoneconsultancy.zeromq.listener;

import io.micrometer.observation.ObservationRegistry;
import java.util.HashMap;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

@Slf4j
public abstract class ObservableListenerContainer
        implements MessageListenerContainer, ApplicationContextAware, BeanNameAware, DisposableBean {

    private static final boolean MICROMETER_PRESENT = ClassUtils.isPresent(
            "io.micrometer.core.instrument.MeterRegistry", AbstractMessageListenerContainer.class.getClassLoader());

    private ApplicationContext applicationContext;

    private final Map<String, String> micrometerTags = new HashMap<>();

    private MicrometerHolder micrometerHolder;

    @Setter
    private boolean micrometerEnabled = true;

    @Setter
    private boolean observationEnabled = false;

    private String beanName = "not.a.Spring.bean";

    private String listenerId;

    private volatile boolean contextClosed;

    private ObservationRegistry observationRegistry = ObservationRegistry.NOOP;

    @Nullable
    protected final ApplicationContext getApplicationContext() {
        return this.applicationContext;
    }

    @Override
    public final void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        if (applicationContext instanceof ConfigurableApplicationContext configurableApplicationContext) {
            configurableApplicationContext.addApplicationListener(new ApplicationClosedListener());
        }
    }

    protected MicrometerHolder getMicrometerHolder() {
        return this.micrometerHolder;
    }

    /**
     * Set additional tags for the Micrometer listener timers.
     *
     * @param tags the tags.
     * @since 2.2
     */
    public void setMicrometerTags(Map<String, String> tags) {
        if (tags != null) {
            this.micrometerTags.putAll(tags);
        }
    }

    protected void checkMicrometer() {
        try {
            if (this.micrometerHolder == null && MICROMETER_PRESENT && this.micrometerEnabled
                    && !this.observationEnabled && this.applicationContext != null) {

                this.micrometerHolder = new MicrometerHolder(this.applicationContext, getListenerId(),
                        this.micrometerTags);
            }
        } catch (IllegalStateException e) {
            log.debug("Could not enable micrometer timers", e);
        }
    }

    protected void checkObservation() {
        if (this.observationEnabled) {
            obtainObservationRegistry(this.applicationContext);
        }
    }

    protected boolean isApplicationContextClosed() {
        return this.contextClosed;
    }

    protected void obtainObservationRegistry(@Nullable ApplicationContext appContext) {
        if (appContext != null) {
            ObjectProvider<ObservationRegistry> registry =
                    appContext.getBeanProvider(ObservationRegistry.class);
            this.observationRegistry = registry.getIfUnique(() -> this.observationRegistry);
        }
    }

    @Override
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    /**
     * @return The bean name that this listener container has been assigned in its containing bean factory, if any.
     */
    @Nullable
    protected final String getBeanName() {
        return this.beanName;
    }

    /**
     * The 'id' attribute of the listener.
     *
     * @return the id (or the container bean name if no id set).
     */
    public String getListenerId() {
        return this.listenerId != null ? this.listenerId : this.beanName;
    }

    @Override
    public void setListenerId(String listenerId) {
        this.listenerId = listenerId;
    }

    @Override
    public void destroy() {
        if (this.micrometerHolder != null) {
            this.micrometerHolder.destroy();
        }
    }

    private final class ApplicationClosedListener implements ApplicationListener<ContextClosedEvent> {

        @Override
        public void onApplicationEvent(ContextClosedEvent event) {
            if (event.getApplicationContext().equals(ObservableListenerContainer.this.applicationContext)) {
                ObservableListenerContainer.this.contextClosed = true;
            }
        }

    }

}
