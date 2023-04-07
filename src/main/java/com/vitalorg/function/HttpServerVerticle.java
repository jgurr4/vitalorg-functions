package com.vitalorg.function;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.core.eventbus.EventBus;
import io.vertx.rxjava3.core.http.HttpServer;
import io.vertx.rxjava3.core.http.HttpServerRequest;
import io.vertx.rxjava3.core.http.HttpServerResponse;
import io.vertx.rxjava3.ext.web.Router;
import io.vertx.rxjava3.ext.web.RoutingContext;
import io.vertx.rxjava3.ext.web.handler.BodyHandler;
import io.vertx.rxjava3.ext.web.handler.SessionHandler;
import io.vertx.rxjava3.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.rxjava3.ext.web.sstore.LocalSessionStore;
import io.vertx.rxjava3.ext.web.sstore.SessionStore;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

// Usage:
/*
        final Router router = Router.router(vertx);
        // app specific router options here

        HttpServerOptions serverOptions = new HttpServerOptions()
                .setSsl(true)
                .setKeyStoreOptions(new JksOptions()
                .setPath("/path/to/store.jks)
                .setPassword(storePass));

 		vertx.deployVerticle(new HttpServerVerticle(router, serverOptions, 8080));
*/

public class HttpServerVerticle extends AbstractVerticle {
    private final Router router;
    private final HttpServerOptions serverOptions;
    private final int port;

    public HttpServerVerticle(Router router, HttpServerOptions serverOptions, int port) {
        this.router = router;
        this.serverOptions = serverOptions;
        this.port = port;
    }

    @Override
    public Completable rxStart() {

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
        router.get("/static/*").handler(ctx -> staticHandle(ctx));
        router.route("/debug").handler(ctx -> DebugHandlerVerticle.handle(ctx));
        router.route().handler(BodyHandler.create());
        router.mountSubRouter("/bus", sockJSHandler.bridge(options));
        router.errorHandler(500, ctx -> ErrorHandlerVerticle.handle(ctx));
//        router.route().handler(ctx -> RouteEndHandlerVerticle.handle(ctx));

        final HttpServer server = vertx.createHttpServer(serverOptions);

        final Single<HttpServer> rxListen = server
                .requestHandler(router)
                .rxListen(port);

        return rxListen.ignoreElement();
    }

    public void writeStaticHtml(Single<HttpServerResponse> response, String path) {
        path = path.substring(1);
        final InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        if (stream != null) {
            final String text = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
            if (path.endsWith(".html")) {
                response.map(r -> r.putHeader("Content-Type", "text/html"));
            } else if (path.endsWith(".css")) {
                response.map(r -> r.putHeader("Content-Type", "text/css"));
            } else if (path.endsWith(".js")) {
                response.map(r -> r.putHeader("Content-Type", "text/javascript"));
            } else {
            }
            response.map(r -> {
                r.setStatusCode(200);
                r.end(text);
                return r;
            });
        } else {
            response.map(r -> {
                r.setStatusCode(404);
                r.end();
                return r;
            });
        }
    }

    public void staticHandle(RoutingContext context) {
        EventBus eb = vertx.eventBus();
        final Single<HttpServerResponse> response = Single.just(context.response().setChunked(true));
        final Single<HttpServerRequest> request = Single.just(context.request());
        final String[] path = new String[1];
        response.map(r -> eb.rxRequest(context.get("application"), context)
                .map(e -> {
                    JsonObject json = JsonObject.mapFrom(e.body());
                    path[0] = json.getString("path");
                    return json.getValue("response");
                })
                .onErrorResumeNext(error -> {
                    System.out.println("Error occurred: " + error.getMessage());
                    r.setStatusCode(502);
                    r.end();
                    return Single.error(error);
                }).cast(HttpServerResponse.class));
        writeStaticHtml(response, path[0]);
        context.next();
    }

}
