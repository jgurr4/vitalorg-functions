package com.vitalorg.function;

import io.vertx.core.Handler;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.RoutingContext;

public class StatusHandlerVerticle {
    public static Handler<RoutingContext> handle(RoutingContext context) {
        final HttpServerResponse response = context.response();
        response.putHeader("Content-Type", "text/html");
        response.setStatusCode(200);
        response.end("<html><body>All is well</body></html>");
        // we are not doing context.next() here because we don't want to create a session for every health check
        RouteEndHandlerVerticle.handle(context);
    }
}
