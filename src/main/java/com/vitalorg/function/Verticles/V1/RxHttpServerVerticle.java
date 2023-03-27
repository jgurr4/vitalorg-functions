package com.vitalorg.function.Verticles.V1;

import com.vitalorg.function.BusEvent;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.eventbus.EventBus;
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

        EventBus eb = vertx.eventBus();
        router.route().handler(ctx -> RouteStartHandlerVerticle.handle(ctx));
        router.get("/status").handler(ctx -> StatusHandlerVerticle.handle(ctx));
        router.route().handler(mySesh);
        router.get("/static/*").handler(ctx -> staticHandle(ctx));
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
