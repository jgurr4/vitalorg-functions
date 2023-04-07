package com.vitalorg.function;


import io.reactivex.rxjava3.annotations.Nullable;
import io.vertx.core.Handler;
import io.vertx.rxjava3.core.http.HttpServerRequest;
import io.vertx.rxjava3.ext.web.RoutingContext;

public class RouteStartHandlerVerticle {
    public static Handler<RoutingContext> handle(RoutingContext context) {
        final HttpServerRequest request = context.request();
        @Nullable final String path = request.path();
        RouteStartHandlerVerticle.handle(context);
        return null;
    }
}
