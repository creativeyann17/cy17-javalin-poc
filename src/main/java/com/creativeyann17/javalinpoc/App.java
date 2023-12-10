package com.creativeyann17.javalinpoc;

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.typesafe.config.ConfigFactory;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinJackson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@Slf4j
public class App {

  private static final Health HEALTH = new Health("OK");
  private static final QueuedThreadPool queuedThreadPool = new QueuedThreadPool(16, 2, 60_000);
  private static final String STARTED = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z").format(new Date());

  static {
    setRootLevel(Level.INFO);
    log.info("App is starting ... CPUs: {}", Runtime.getRuntime().availableProcessors());
  }

  public final Javalin app;

  public App() {
    var appConfig = ConfigFactory.load();
    var publics = appConfig.getStringList("public");

    /*ClasspathLoader loader = new ClasspathLoader();
    loader.setCharset("UTF-8");
    loader.setSuffix(".peb");
    loader.setPrefix("");
    var engine = new PebbleEngine.Builder().strictVariables(false).loader(loader).build();
*/
    //JavalinPebble.init();
    //JavalinRenderer.register(new JavalinPebble(), ".peb", ".pebble");

    //queuedThreadPool.setVirtualThreadsExecutor(Executors.newVirtualThreadPerTaskExecutor());

    this.app = Javalin.create(config -> {
        config.showJavalinBanner = true;
        config.compression.gzipOnly();
        config.plugins.enableCors(cors -> {
          cors.add(it -> {
            it.anyHost();
            it.maxAge = 3600;
            it.allowCredentials = true;
            it.exposeHeader("*");
          });
          config.staticFiles.add(staticFiles -> {
            staticFiles.directory = "/static";
            staticFiles.location = Location.CLASSPATH;
            staticFiles.hostedPath = "/";
            staticFiles.precompress = true;
            //staticFiles.headers = Map.of("last-modified", STARTED);
          });
        });
      /*  config.jetty.server(() -> {
          var server = new Server(queuedThreadPool);
          server.setDryRun(false);
          return server;
        });*/
        config.accessManager((handler, ctx, routeRoles) -> {
          var auth = ctx.header(HttpHeader.AUTHORIZATION.name());
          if (publics.stream().anyMatch(s -> ctx.req().getRequestURI().startsWith(s))) {
            handler.handle(ctx);
          } else if (StringUtils.isBlank(auth)) {
            ctx.status(HttpStatus.UNAUTHORIZED).result("");
          }
        });
        config.jsonMapper(new JavalinJackson().updateMapper(mapper -> {
          mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
          mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        }));
      })
      .exception(Exception.class, (e, ctx) -> {
        log.error("", e);
        ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).result("");
      })
      .get("/render", (ctx) -> ctx.render("templates/index.peb", Map.of("value", "foo")))
      .get("/bench", App::bench)
      .get("/health", ctx -> ctx.json(HEALTH))
      .get("/secured", (context -> context.result("OK")))
      .get("/session", (ctx -> {
        System.out.println("Session: " + ctx.sessionAttributeMap());
        ctx.sessionAttribute("foo", "bar");
        System.out.println("Cookie: " + ctx.cookieMap());
        ctx.cookie("cook", "me");
        ctx.result("OK");
      }))
      .get("/error", (ctx) -> {
        throw new RuntimeException("Someting went badddd");
      });


    app.events(event -> {
      event.serverStopping(() -> {
        log.info("Server stopping ...");
      });
      event.serverStopped(() -> {
        log.info("Server stopped ...");
      });
    });

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      log.info("ShutdownHook called");
      app.stop();
    }));

  }

  public static void main(String[] args) {
    new App().app.start(8080);
  }

  private static void bench(Context ctx) throws InterruptedException {
    final long start = System.currentTimeMillis();
    var data = "";
    //TimeUnit.MILLISECONDS.sleep(random.nextLong(300));
    // CompletableFuture.runAsync(() -> {
    log.info("Data processed: {} {} {} ({}ms)", ctx.req().getMethod(), ctx.req().getRequestURI(), FileUtils.byteCountToDisplaySize(data.length()), System.currentTimeMillis() - start);

    // }, queuedThreadPool.getVirtualThreadsExecutor());
    ctx.result("DONE");
  }

  public static void setRootLevel(Level level) {
    final ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    root.setLevel(level);
  }

  private record Health(String status) implements Serializable {
  }
}
