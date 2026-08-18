package com.aalsaeed.fleetops.testinfra;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.testcontainers.junit.jupiter.Container;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Closes cached Spring test contexts that depend on class-managed Testcontainers.
 *
 * <p>JUnit owns the lifecycle of static {@link Container} fields. Without explicit
 * context disposal, Spring may keep a cached DataSource alive after Testcontainers
 * stops PostgreSQL, leaving Hikari housekeeper threads validating dead connections
 * while later tests are already running.</p>
 */
public final class ContainerBackedContextCleanupTestExecutionListener
        extends AbstractTestExecutionListener {

    private static final int ORDER = 2999;

    @Override
    public void afterTestClass(TestContext testContext) {
        if (!testContext.hasApplicationContext()) {
            return;
        }
        if (!usesClassManagedContainers(testContext.getTestClass())) {
            return;
        }

        testContext.markApplicationContextDirty(DirtiesContext.HierarchyMode.EXHAUSTIVE);
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    static boolean usesClassManagedContainers(Class<?> testClass) {
        Class<?> current = testClass;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) && field.isAnnotationPresent(Container.class)) {
                    return true;
                }
            }
            current = current.getSuperclass();
        }
        return false;
    }
}
