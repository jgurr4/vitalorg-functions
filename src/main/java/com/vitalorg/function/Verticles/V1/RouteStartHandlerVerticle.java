package com.vitalorg.function.Verticles.V1;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Handler;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.ext.web.RoutingContext;

public class RouteStartHandlerVerticle extends AbstractVerticle {
    public static Handler<RoutingContext> handle(RoutingContext context) {
        final HttpServerRequest request = context.request();
        @Nullable final String path = request.path();
        context.next();
    }
}
