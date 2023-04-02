package com.vitalorg.function;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Handler;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.RoutingContext;

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
    }
}
