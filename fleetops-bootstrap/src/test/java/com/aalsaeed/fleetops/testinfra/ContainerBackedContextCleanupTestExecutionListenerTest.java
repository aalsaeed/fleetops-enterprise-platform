package com.aalsaeed.fleetops.testinfra;

import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;

import static org.assertj.core.api.Assertions.assertThat;
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
        when(testContext.getTestClass()).thenReturn(ContainerBackedTest.class);

        listener.afterTestClass(testContext);

        verify(testContext).markApplicationContextDirty(DirtiesContext.HierarchyMode.EXHAUSTIVE);
    }

    @Test
    void leavesContextCachedWhenTestDoesNotOwnStaticContainer() {
        TestContext testContext = mock(TestContext.class);
        when(testContext.hasApplicationContext()).thenReturn(true);
        when(testContext.getTestClass()).thenReturn(PlainSpringTest.class);

        listener.afterTestClass(testContext);

        verify(testContext, never()).markApplicationContextDirty(DirtiesContext.HierarchyMode.EXHAUSTIVE);
    }

    @Test
    void detectsInheritedClassManagedContainer() {
        assertThat(ContainerBackedContextCleanupTestExecutionListener
                .usesClassManagedContainers(InheritedContainerTest.class))
                .isTrue();
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
