package com.aalsaeed.fleetops.testinfra;

import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextManager;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContainerBackedContextCleanupTestExecutionListenerTest {

    private final ContainerBackedContextCleanupTestExecutionListener listener =
            new ContainerBackedContextCleanupTestExecutionListener();

    @Test
    void marksLoadedContextDirtyForStaticClassManagedContainer() {
        TestContext testContext = mock(TestContext.class);
        when(testContext.hasApplicationContext()).thenReturn(true);
        doReturn(ContainerBackedTest.class).when(testContext).getTestClass();

        listener.afterTestClass(testContext);

        verify(testContext).markApplicationContextDirty(DirtiesContext.HierarchyMode.EXHAUSTIVE);
    }

    @Test
    void leavesContextCachedWhenTestDoesNotOwnStaticContainer() {
        TestContext testContext = mock(TestContext.class);
        when(testContext.hasApplicationContext()).thenReturn(true);
        doReturn(PlainSpringTest.class).when(testContext).getTestClass();

        listener.afterTestClass(testContext);

        verify(testContext, never()).markApplicationContextDirty(DirtiesContext.HierarchyMode.EXHAUSTIVE);
    }

    @Test
    void detectsInheritedClassManagedContainer() {
        assertThat(ContainerBackedContextCleanupTestExecutionListener
                .usesClassManagedContainers(InheritedContainerTest.class))
                .isTrue();
    }

    @Test
    void isAutomaticallyDiscoveredBySpringTestContext() {
        TestContextManager manager = new TestContextManager(PlainSpringTest.class);

        assertThat(manager.getTestExecutionListeners())
                .anyMatch(ContainerBackedContextCleanupTestExecutionListener.class::isInstance);
    }

    private static class ContainerBackedTest {
        @Container
        static final GenericContainer<?> CONTAINER = new GenericContainer<>("alpine:3.20");
    }

    private static class InheritedContainerTest extends ContainerBackedTest {
    }

    private static class PlainSpringTest {
    }
}
