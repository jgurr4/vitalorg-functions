package com.vitalorg.function.Verticles.V1;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Handler;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.Session;

public class StaticHandlerVerticle {
    public static Handler<RoutingContext> handle(RoutingContext context) {
        final HttpServerResponse response = context.response();
        final HttpServerRequest request = context.request();
        @Nullable String path = request.path();
        try {
            response.setChunked(true);
            Session session = context.session();
            String username = session.get(SessionKey.username.name());
            if (username != null && path.equals("/static/login.html") || username != null && path.equals("/static/jscrawl.html")) {
                WebUtils.redirect(response, "/static/jscrawl.html");
                return;
            }
            writeStaticHtml(response, path);
        } catch (Throwable e) {
            response.setStatusCode(502);
            response.end();
        }
        context.next();

    }
}
