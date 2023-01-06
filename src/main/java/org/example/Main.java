package org.example;

import io.micrometer.core.instrument.*;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Main {
    public static void main(String[] args) {
        PrometheusMeterRegistry registry = getPrometheusMeterRegistry();
        var vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
                        new MicrometerMetricsOptions()
                                .setMicrometerRegistry(registry)
                                .setPrometheusOptions(
                                        new VertxPrometheusOptions().setEnabled(true)
                                                .setStartEmbeddedServer(true)
                                                .setEmbeddedServerOptions(new HttpServerOptions().setPort(8080))
                                                .setEmbeddedServerEndpoint("/metrics"))
                                .setEnabled(true)
                )
        );

        var orgs = List.of("a", "b", "c", "d", "e", "f");
        var i = new AtomicInteger(0);

        vertx.setPeriodic(1000, t -> {
            var index = i.get();
            var org = orgs.get(index);
            var tags = Tags.of(
                    Tag.of("org", org),
                    Tag.of("tag", "function")
            );
            i.set((index + 1) % orgs.size());
            int value = new Random().nextInt(1000);

            registry.remove(new Meter.Id("admin", tags, null, null, Meter.Type.GAUGE));
//            registry.gauge("admin", tags, new AtomicLong(value));
            Gauge.builder("admin", () -> value)
                    .tags(tags)
                    .strongReference(true)
                    .register(registry);

            System.out.println(org + "=" + value);
//            System.gc();
        });

    }


    private static PrometheusMeterRegistry getPrometheusMeterRegistry() {
        var prometheusClientRegistry = CollectorRegistry.defaultRegistry;
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT, prometheusClientRegistry, Clock.SYSTEM);
        return registry;
    }

}
