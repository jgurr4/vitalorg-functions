package com.vitalorg.function.Verticles.V1;

import com.vitalorg.function.BusEvent;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.Session;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import io.vertx.reactivex.ext.web.handler.SessionHandler;
import io.vertx.reactivex.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.reactivex.ext.web.sstore.LocalSessionStore;
import io.vertx.reactivex.ext.web.sstore.SessionStore;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;


public class RxHttpServerVerticle extends AbstractVerticle {

    @Override
    public Completable rxStart() {

        final Router router = Router.router(vertx);
        final SessionStore store = LocalSessionStore.create(vertx);
        final SessionHandler mySesh = SessionHandler.create(store);
        mySesh.setSessionTimeout(86400000); //24 hours in milliseconds.

        final SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
        final SockJSBridgeOptions options = new SockJSBridgeOptions();
        final PermittedOptions inboundPermitted = new PermittedOptions()
                .setAddress(BusEvent.browserInput.name())
                .setAddress(BusEvent.newGame.name());
        options.addInboundPermitted(inboundPermitted);

        router.route().handler(ctx -> RouteStartHandlerVerticle.handle(ctx));
        router.get("/status").handler(ctx -> StatusHandlerVerticle.handle(ctx));
        router.route().handler(mySesh);
        router.get("/static/*").handler(ctx -> StaticHandlerVerticle.handle(ctx));
        router.route("/debug").handler(ctx -> DebugHandlerVerticle.handle(ctx));
        router.route().handler(BodyHandler.create());
        router.mountSubRouter("/bus", sockJSHandler.bridge(options));
        router.errorHandler(500, ctx -> ErrorHandlerVerticle.handle(ctx));
        router.route().handler(ctx -> RouteEndHandlerVerticle.handle(ctx));

        final HttpServer server = vertx.createHttpServer(new HttpServerOptions());
        final int port = 8080;

        final Single<HttpServer> rxListen = server
                .requestHandler(router)
                .rxListen(port);

        return rxListen.ignoreElement();
    }

    public void writeStaticHtml(HttpServerResponse response, String path) {
        path = path.substring(1);
        final InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        if (stream != null) {
            final String text = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
            if (path.endsWith(".html")) {
                response.putHeader("Content-Type", "text/html");
            } else if (path.endsWith(".css")) {
                response.putHeader("Content-Type", "text/css");
            } else if (path.endsWith(".js")) {
                response.putHeader("Content-Type", "text/javascript");
            } else {
            }
            response.setStatusCode(200);
            response.end(text);
        } else {
            response.setStatusCode(404);
            response.end();
        }
    }

}
