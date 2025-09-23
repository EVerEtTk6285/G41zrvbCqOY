// 代码生成时间: 2025-09-23 08:21:34
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeEventType;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.webauthn.WebAuthnAuth;
import io.vertx.ext.web.handler.AuthenticationHandler;
import io.vertx.ext.web.handler.usermgmt.SimpleUserRegistry;
import io.vertx.ext.web.handler.usermgmt.UserManager;
import io.vertx.ext.auth.webauthn.WebAuthnUserAuth;
import io.vertx.ext.web.handler.AuthHandler;
import io.vertx.ext.web.handler.AuthHandlerRegistry;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.sockjs.BridgeEventHandler;
import io.vertx.ext.web.handler.sockjs.BridgeEventType;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * 网页内容抓取工具，使用Vertx框架实现
 */
public class WebContentFetcher extends AbstractVerticle {

    // 创建Web客户端
    private WebClient client;

    // 创建HttpClient
    private HttpClient httpClient;

    @Override
    public void start() {
        // 初始化Web客户端
        client = WebClient.create(vertx);

        // 初始化HttpClient
        httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // 创建Router
        Router router = Router.router(vertx);

        // 创建SessionHandler
        SessionHandler sessionHandler = SessionHandler.create(LocalSessionStore.create(vertx));

        // 创建SockJSHandler
        SockJSHandlerOptions sockJSHandlerOptions = new SockJSHandlerOptions().setHeartbeatInterval(5000);
        SockJSHandler sockJSHandler = SockJSHandler.create(vertx, sockJSHandlerOptions);

        // 创建BridgeOptions
        BridgeOptions bridgeOptions = new BridgeOptions();
        sockJSHandler.bridge(bridgeOptions, new BridgeEventHandler() {
            @Override
            public void onBridgeEvent(BridgeEventType type, JsonObject message, MultiMap headers, Handler<AsyncResult<Buffer>> resultHandler) {
                // 处理桥接事件
            }
        });

        // 创建路由
        router.post("/fetch").handler(this::fetchWebContent);

        // 设置静态文件处理器
        router.route("/static/*").handler(StaticHandler.create("webroot"));

        // 设置SessionHandler
        router.route().handler(sessionHandler);

        // 设置SockJSHandler
        router.route("/eventbus/*").handler(sockJSHandler);

        // 创建和启动HTTP服务器
        vertx.createHttpServer()
                .requestHandler(router)
                .listen(config().getInteger("http.port", 8080));
    }

    /**
     * 抓取网页内容
     *
     * @param context RoutingContext对象
     */
    private void fetchWebContent(RoutingContext context) {
        try {
            // 获取URL参数
            String url = context.request().getParam("url");

            // 检查URL是否为空
            if (url == null) {
                context.response().setStatusCode(400).end("URL is required");
                return;
            }

            // 发送HTTP请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            // 获取响应
            CompletableFuture<HttpResponse<String>> responseFuture = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            responseFuture.thenAccept(response -> {
                // 检查响应状态码
                if (response.statusCode() == 200) {
                    // 返回响应内容
                    context.response()
                            .putHeader("content-type", "text/html")
                            .end(response.body());
                } else {
                    // 返回错误信息
                    context.response().setStatusCode(response.statusCode()).end("Failed to fetch web content");
                }
            }).exceptionally(e -> {
                // 处理异常
                context.response().setStatusCode(500).end("Error fetching web content");
                return null;
            });
        } catch (Exception e) {
            // 处理异常
            context.response().setStatusCode(500).end("Error fetching web content");
        }
    }
}
