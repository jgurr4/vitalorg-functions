import com.vitalorg.function.HttpServerVerticle;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.net.SelfSignedCertificate;
import io.vertx.rxjava3.ext.web.Router;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.net.ssl.KeyManagerFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
public class HttpServerTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerTests.class);

    private Vertx vertx;

    @BeforeEach
    public void setUp(TestInfo testInfo) {
        vertx = Vertx.vertx(new VertxOptions().setBlockedThreadCheckInterval(1000));
    }

    @Test
    void testDeployHttpVerticle(Vertx vertx, VertxTestContext context) {
        final Router router = Router.router(vertx);
        HttpServerOptions serverOptions = new HttpServerOptions();

        Single<String> deployment = vertx.rxDeployVerticle(new HttpServerVerticle(router, serverOptions, 8080), new DeploymentOptions()
                .setWorker(true)
                .setMaxWorkerExecuteTime(1000)
                .setMaxWorkerExecuteTimeUnit(TimeUnit.MILLISECONDS)
                );

        deployment.subscribe(id -> {
            assertNotNull(id, "Verticle deployment ID must not be null");
            context.completeNow();
        }, context::failNow);
        assertTrue(deployment.blockingGet() != null, "Verticle deployment ID must not be null");

    }

    // NOTE: This is currently broken, even when explicitely adding a BouncyCastleProvider
/*
    @Test
    void testDeployHttpsVerticle(Vertx vertx, VertxTestContext context) {
        final Router router = Router.router(vertx);
        Security.addProvider(new BouncyCastleProvider());
        SelfSignedCertificate cert = SelfSignedCertificate.create();

        HttpServerOptions serverOptions = new HttpServerOptions()
                .setSsl(true)
                .setKeyCertOptions(cert.keyCertOptions());
//                .setKeyStoreOptions(new JksOptions()
//                .setPath("/path/to/store.jks")
//                .setPassword("storePass"));

        Single<String> deployment = vertx.rxDeployVerticle(new HttpServerVerticle(router, serverOptions, 8080), new DeploymentOptions()
                .setWorker(true)
                .setMaxWorkerExecuteTime(1000)
                .setMaxWorkerExecuteTimeUnit(TimeUnit.MILLISECONDS)
        );

        deployment.subscribe(id -> {
            assertNotNull(id, "Verticle deployment ID must not be null");
            context.completeNow();
        }, context::failNow);
        assertTrue(deployment.blockingGet() != null, "Verticle deployment ID must not be null");

    }
*/

    @AfterEach
    public void tearDown(TestInfo testInfo) {
        vertx.close();
    }

}
