package com.vitalorg.function.Verticles;

import com.vitalorg.function.BusEvent;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import io.vertx.reactivex.ext.web.handler.SessionHandler;
import io.vertx.reactivex.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.reactivex.ext.web.sstore.LocalSessionStore;
import io.vertx.reactivex.ext.web.sstore.SessionStore;


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

        router.route().handler(this::routeStartHandler);
        router.get("/status").handler(this::statusHandler);
        router.route().handler(mySesh);
        router.get("/static/*").handler(this::staticHandler);
        router.route("/debug").handler(this::debugHandler);
        router.route().handler(BodyHandler.create());
        router.mountSubRouter("/bus", sockJSHandler.bridge(options));
        router.errorHandler(500, this::errorHandler);
        router.route().handler(this::routeEndHandler);

        final HttpServer server = vertx.createHttpServer(new HttpServerOptions());
        final int port = 8080;

        final Single<HttpServer> rxListen = server
                .requestHandler(router)
                .rxListen(port);

        return rxListen.ignoreElement();
    }
}
