package com.codeaffine.tiny.star.extrinsic;

import org.eclipse.rap.rwt.application.Application;
import org.eclipse.rap.rwt.application.ApplicationConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.slf4j.Logger;

import java.net.URI;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.codeaffine.tiny.star.extrinsic.Log4j2Configurator.CONFIGURATION_FILE_NAME_SUFFIX;
import static com.codeaffine.tiny.star.extrinsic.Log4j2ConfiguratorTest.FakeConfigurator.initializeFakeConfigurator;
import static com.codeaffine.tiny.star.extrinsic.Log4j2ConfiguratorTest.FakeConfigurator.reconfigureCalled;
import static com.codeaffine.tiny.star.extrinsic.Texts.*;
import static java.lang.Thread.currentThread;
import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class Log4j2ConfiguratorTest {

    private static final String GLOBAL = "application-expecting-log4j2-configuration-only-available-to-system-classloader";
    private static final String UNKNOWN_CONFIGURATOR_CLASS = "unknownConfiguratorClass";
    private static final String CONFIGURATOR_CLASS = FakeConfigurator.class.getName();
    private static final String APPLICATION_NAME = TestConfiguration.class.getName();
    private static final String RECONFIGURE_METHOD = "reconfigure";
    private static final String ERROR_MESSAGE = "bad";

    private TestConfiguration configuration;
    private Logger logger;

    static class FakeConfigurator {

        static final AtomicReference<Exception> problemHolder = new AtomicReference<>();
        static final AtomicBoolean reconfigureCalled = new AtomicBoolean(false);

        static void initializeFakeConfigurator() {
            reconfigureCalled.set(false);
            problemHolder.set(null);
        }

        @SuppressWarnings("unused")
        public static void reconfigure(@SuppressWarnings("unused") URI uri) throws Exception {
            if(nonNull(problemHolder.get())) {
                throw problemHolder.get();
            }
            reconfigureCalled.set(true);
        }
    }

    static class TestConfiguration implements ApplicationConfiguration {
        @Override
        public void configure(Application application) {}
    }

    @BeforeEach
    void setUp() {
        initializeFakeConfigurator();
        configuration = new TestConfiguration();
        logger = mock(Logger.class);
    }

    @Test
    void run() {
        Log4j2Configurator configurator = new Log4j2Configurator(CONFIGURATOR_CLASS, RECONFIGURE_METHOD, r -> null, logger);

        configurator.run(getAppLoader(), APPLICATION_NAME);

        assertThat(reconfigureCalled).isTrue();
        InOrder order = inOrder(logger);
        order.verify(logger).debug(LOG_LOG4J2_DETECTED, expectedLog4j2ConfigurationFileName());
        order.verify(logger).debug(LOG_TRY_LOADING_CONFIGURATION_LOADING_FROM_CONTEXT_CLASS_LOADER, expectedLog4j2ConfigurationFileName());
        order.verify(logger).debug(LOG_LOG4J2_RECONFIGURATION_SUCCESSFUL, expectedLog4j2ConfigurationFileName());
        order.verifyNoMoreInteractions();
    }

    @Test
    void runIfContextClassLoaderIsNotSet() {
        Log4j2Configurator configurator = new Log4j2Configurator(CONFIGURATOR_CLASS, RECONFIGURE_METHOD,r -> null, logger);
        currentThread().setContextClassLoader(null);

        configurator.run(getAppLoader(), APPLICATION_NAME);

        assertThat(reconfigureCalled).isTrue();
        InOrder order = inOrder(logger);
        order.verify(logger).debug(LOG_LOG4J2_DETECTED, expectedLog4j2ConfigurationFileName());
        order.verify(logger).debug(LOG_TRY_LOADING_CONFIGURATION_FROM_APPLICATION_CLASS_LOADER, expectedLog4j2ConfigurationFileName());
        order.verify(logger).debug(LOG_LOG4J2_RECONFIGURATION_SUCCESSFUL, expectedLog4j2ConfigurationFileName());
        order.verifyNoMoreInteractions();
    }

    @Test
    void runIfContextClassLoaderCannotFindResource() {
        Log4j2Configurator configurator = new Log4j2Configurator(CONFIGURATOR_CLASS, RECONFIGURE_METHOD, r -> null, logger);
        currentThread().setContextClassLoader(mock(ClassLoader.class));

        configurator.run(getAppLoader(), APPLICATION_NAME);

        assertThat(reconfigureCalled).isTrue();
        InOrder order = inOrder(logger);
        order.verify(logger).debug(LOG_LOG4J2_DETECTED, expectedLog4j2ConfigurationFileName());
        order.verify(logger).debug(LOG_TRY_LOADING_CONFIGURATION_LOADING_FROM_CONTEXT_CLASS_LOADER, expectedLog4j2ConfigurationFileName());
        order.verify(logger).debug(LOG_TRY_LOADING_CONFIGURATION_FROM_APPLICATION_CLASS_LOADER, expectedLog4j2ConfigurationFileName());
        order.verify(logger).debug(LOG_LOG4J2_RECONFIGURATION_SUCCESSFUL, expectedLog4j2ConfigurationFileName());
        order.verifyNoMoreInteractions();
    }

    @Test
    void runIfApplicationClassLoaderCannotFindResource()  {
        URL resource = getClass().getClassLoader().getResource(expectedLog4j2ConfigurationFileName());
        Log4j2Configurator configurator = new Log4j2Configurator(CONFIGURATOR_CLASS, RECONFIGURE_METHOD, any -> resource, logger);

        configurator.run(getAppLoader(), GLOBAL);

        assertThat(reconfigureCalled).isTrue();
        InOrder order = inOrder(logger);
        order.verify(logger).debug(LOG_LOG4J2_DETECTED, expectedLog4j2ConfigurationFileName(GLOBAL));
        order.verify(logger).debug(LOG_TRY_LOADING_CONFIGURATION_LOADING_FROM_CONTEXT_CLASS_LOADER, expectedLog4j2ConfigurationFileName(GLOBAL));
        order.verify(logger).debug(LOG_TRY_LOADING_CONFIGURATION_FROM_APPLICATION_CLASS_LOADER, expectedLog4j2ConfigurationFileName(GLOBAL));
        order.verify(logger).debug(LOG_TRY_LOADING_CONFIGURATION_LOADING_FROM_SYSTEM_CLASS_LOADER, expectedLog4j2ConfigurationFileName(GLOBAL));
        order.verify(logger).debug(LOG_LOG4J2_RECONFIGURATION_SUCCESSFUL, expectedLog4j2ConfigurationFileName(GLOBAL));
        order.verifyNoMoreInteractions();
    }

    @Test
    void runIfNoClassLoaderCanFindResource()  {
        Log4j2Configurator configurator = new Log4j2Configurator(CONFIGURATOR_CLASS, RECONFIGURE_METHOD, any -> null, logger);

        configurator.run(getAppLoader(), GLOBAL);

        assertThat(reconfigureCalled).isFalse();
        InOrder order = inOrder(logger);
        order.verify(logger).debug(LOG_LOG4J2_DETECTED, expectedLog4j2ConfigurationFileName(GLOBAL));
        order.verify(logger).debug(LOG_TRY_LOADING_CONFIGURATION_LOADING_FROM_CONTEXT_CLASS_LOADER, expectedLog4j2ConfigurationFileName(GLOBAL));
        order.verify(logger).debug(LOG_TRY_LOADING_CONFIGURATION_FROM_APPLICATION_CLASS_LOADER, expectedLog4j2ConfigurationFileName(GLOBAL));
        order.verify(logger).debug(LOG_TRY_LOADING_CONFIGURATION_LOADING_FROM_SYSTEM_CLASS_LOADER, expectedLog4j2ConfigurationFileName(GLOBAL));
        order.verify(logger).debug(LOG_LOG4J2_CONFIGURATION_NOT_FOUND, expectedLog4j2ConfigurationFileName(GLOBAL));
        order.verifyNoMoreInteractions();
    }

    @Test
    void runIfReconfigurationCallThrowException() {
        Log4j2Configurator configurator = new Log4j2Configurator(CONFIGURATOR_CLASS, RECONFIGURE_METHOD, r -> null, logger);
        RuntimeException expected = new RuntimeException(ERROR_MESSAGE);
        FakeConfigurator.problemHolder.set(expected);

        Exception actual = catchException(() -> configurator.run(getAppLoader(), APPLICATION_NAME));

        assertThat(reconfigureCalled).isFalse();
        assertThat(actual).isSameAs(expected);
    }

    @Test
    void runIfLog4J2CannotBeDetected() {
        Log4j2Configurator configurator = new Log4j2Configurator(UNKNOWN_CONFIGURATOR_CLASS, RECONFIGURE_METHOD, r -> null, logger);

        configurator.run(getAppLoader(), GLOBAL);

        assertThat(reconfigureCalled).isFalse();
        verify(logger, never()).debug(anyString(), eq(expectedLog4j2ConfigurationFileName(GLOBAL)));
    }

    @Test
    void runWithNullAsConfigurationArgument() {
        Log4j2Configurator configurator = new Log4j2Configurator();

        assertThatThrownBy(() -> configurator.run(null, GLOBAL))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void runWithNullAsApplicationNameArgument() {
        ClassLoader appLoader = getAppLoader();
        Log4j2Configurator configurator = new Log4j2Configurator();

        assertThatThrownBy(() -> configurator.run(appLoader, null))
            .isInstanceOf(NullPointerException.class);
    }

    private static String expectedLog4j2ConfigurationFileName() {
        return expectedLog4j2ConfigurationFileName(APPLICATION_NAME);
    }

    private static String expectedLog4j2ConfigurationFileName(String applicationName) {
        return applicationName + CONFIGURATION_FILE_NAME_SUFFIX;
    }

    private ClassLoader getAppLoader() {
        return configuration.getClass().getClassLoader();
    }
}
