package com.vitalorg.function;

import io.reactivex.rxjava3.annotations.Nullable;
import io.vertx.core.Handler;
import io.vertx.rxjava3.core.http.HttpServerRequest;
import io.vertx.rxjava3.ext.web.RoutingContext;

public class ErrorHandlerVerticle {
    public static Handler<RoutingContext> handle(RoutingContext context) {
        final HttpServerRequest request = context.request();
        @Nullable final String path = request.path();
        RouteEndHandlerVerticle.handle(context);
        return null;
    }
}
