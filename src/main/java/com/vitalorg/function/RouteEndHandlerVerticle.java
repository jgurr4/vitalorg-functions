package com.vitalorg.function;

import io.reactivex.rxjava3.annotations.Nullable;
import io.vertx.core.Handler;
import io.vertx.rxjava3.core.http.HttpServerRequest;
import io.vertx.rxjava3.core.http.HttpServerResponse;
import io.vertx.rxjava3.ext.web.RoutingContext;

public class RouteEndHandlerVerticle {
    public static Handler<RoutingContext> handle(RoutingContext context) {
        final HttpServerRequest request = context.request();
        final HttpServerResponse response = context.response();
        @Nullable final String path = request.path();
        if (!response.ended()) {
            response.setStatusCode(404);
            response.end();
        }
        int code = response.getStatusCode();
        return null;
    }
}
