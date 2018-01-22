/*
 * Copyright (c) 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.vertx.junit5;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * @author <a href="https://julien.ponge.org/">Julien Ponge</a>
 */
class VertxExtensionTest {

  @Nested
  @ExtendWith(VertxExtension.class)
  class Injection {

    @Test
    void gimme_vertx(Vertx vertx) {
      assertNotNull(vertx);
    }

    @Test
    void gimme_vertx_test_context(VertxTestContext context) {
      assertNotNull(context);
      context.completeNow();
    }

    @Test
    void gimme_everything(Vertx vertx, VertxTestContext context) {
      assertNotNull(vertx);
      assertNotNull(context);
      context.completeNow();
    }
  }

  @Nested
  @ExtendWith(VertxExtension.class)
  @Timeout(4500)
  class SpecifyTimeout {

    @Test
    @Timeout(value = 5, timeUnit = TimeUnit.SECONDS)
    void a(VertxTestContext context) throws InterruptedException {
      Thread.sleep(50);
      context.completeNow();
    }

    @Test
    void b(VertxTestContext context) throws InterruptedException {
      Thread.sleep(50);
      context.completeNow();
    }
  }

  @Nested
  class EmbeddedWithARunner {

    @Test
    void checkFailureTest() {
      LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
        .selectors(selectClass(FailureTest.class))
        .build();
      Launcher launcher = LauncherFactory.create();
      SummaryGeneratingListener listener = new SummaryGeneratingListener();
      launcher.registerTestExecutionListeners(listener);
      launcher.execute(request);
      TestExecutionSummary summary = listener.getSummary();
      assertThat(summary.getTestsStartedCount()).isEqualTo(1);
      assertThat(summary.getTestsFailedCount()).isEqualTo(1);
      assertThat(summary.getFailures().get(0).getException()).isInstanceOf(AssertionError.class);
    }

    @Nested
    @ExtendWith(VertxExtension.class)
    class FailureTest {

      @Test
      @Tag("programmatic")
      void thisMustFail(Vertx vertx, VertxTestContext testContext) {
        testContext.verify(() -> {
          assertTrue(false);
        });
      }
    }

    @Test
    void checkDirectFailure() {
      LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
        .selectors(selectClass(DirectFailureTest.class))
        .build();
      Launcher launcher = LauncherFactory.create();
      SummaryGeneratingListener listener = new SummaryGeneratingListener();
      launcher.registerTestExecutionListeners(listener);
      launcher.execute(request);
      TestExecutionSummary summary = listener.getSummary();
      assertThat(summary.getTestsStartedCount()).isEqualTo(1);
      assertThat(summary.getTestsFailedCount()).isEqualTo(1);
      assertThat(summary.getFailures().get(0).getException()).isInstanceOf(RuntimeException.class);
    }

    @Nested
    @ExtendWith(VertxExtension.class)
    class DirectFailureTest {

      @Test
      @Tag("programmatic")
      @Timeout(value = 1, timeUnit = TimeUnit.SECONDS)
      void thisMustFail(VertxTestContext testContext) {
        throw new RuntimeException("YOLO");
      }
    }
  }

  static class UselessVerticle extends AbstractVerticle {
  }

  @Nested
  @ExtendWith(VertxExtension.class)
  class VertxInjectionTest {

    @BeforeEach
    void prepare(Vertx vertx, VertxTestContext testContext) {
      vertx.deployVerticle(new UselessVerticle(), testContext.succeeding());
    }

    @AfterEach
    void cleanup(Vertx vertx, VertxTestContext testContext) {
      assertThat(vertx.deploymentIDs()).isNotEmpty().hasSize(1);
      vertx.close(testContext.succeeding());
    }

    @RepeatedTest(10)
    void checkDeployments(Vertx vertx, VertxTestContext testContext) {
      assertThat(vertx.deploymentIDs()).isNotEmpty().hasSize(1);
      testContext.completeNow();
    }

    @Nested
    class NestedTest {
      @RepeatedTest(10)
      void checkDeployments(Vertx vertx, VertxTestContext testContext) {
        assertThat(vertx.deploymentIDs()).isNotEmpty().hasSize(1);
        testContext.completeNow();
      }
    }
  }
}
