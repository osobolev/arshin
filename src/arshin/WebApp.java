package arshin;

import arshin.dto.NumInfo;
import freemarker.cache.FileTemplateLoader;
import freemarker.core.HTMLOutputFormat;
import freemarker.template.Configuration;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;
import io.javalin.rendering.template.JavalinFreemarker;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class WebApp {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebApp.class);

    private static String normalize(String num) {
        if (num == null)
            return null;
        String trimmed = num.trim();
        if (trimmed.isEmpty())
            return null;
        return trimmed;
    }

    private static void setupProxy(Properties props, HttpClientBuilder builder) {
        String host = props.getProperty("proxy.host");
        String port = props.getProperty("proxy.port");
        if (host == null || port == null)
            return;

        HttpHost proxy = HttpHost.create(URI.create(host + ":" + port));
        builder.setProxy(proxy);

        String user = props.getProperty("proxy.user");
        String password = props.getProperty("proxy.password");
        if (user != null) {
            BasicCredentialsProvider creds = new BasicCredentialsProvider();
            creds.setCredentials(new AuthScope(proxy), new UsernamePasswordCredentials(user, password == null ? null : password.toCharArray()));
            builder.setDefaultCredentialsProvider(creds);
        }
    }

    private static String ip(Context ctx) {
        String realIP = ctx.header("X-Real-IP");
        if (realIP != null)
            return realIP;
        return ctx.ip();
    }

    public static void main(String[] args) {
        LOGGER.info("================= STARTING ARSHIN SERVER =================");
        try {
            Path propFile = Paths.get("arshin.properties");
            Properties props = new Properties();
            if (Files.exists(propFile)) {
                try (BufferedReader rdr = Files.newBufferedReader(propFile)) {
                    props.load(rdr);
                }
            }

            Supplier<CloseableHttpClient> client = new ExpiredSupplier<>(6, TimeUnit.HOURS, () -> {
                HttpClientBuilder builder = HttpClients.custom();
                builder.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36");
                setupProxy(props, builder);
                PoolingHttpClientConnectionManager cman = new PoolingHttpClientConnectionManager();
                cman.setMaxTotal(50);
                return builder.setConnectionManager(cman).build();
            });

            Configuration ftlConfig = new Configuration(Configuration.VERSION_2_3_33);
            ftlConfig.setOutputFormat(HTMLOutputFormat.INSTANCE);
            ftlConfig.setDefaultEncoding("UTF-8");
            ftlConfig.setTemplateLoader(new FileTemplateLoader(new File("web")));

            Javalin app = Javalin.create(cfg -> {
                cfg.showJavalinBanner = false;
                cfg.staticFiles.add("web/public", Location.EXTERNAL);
                cfg.fileRenderer(new JavalinFreemarker(ftlConfig));
            });
            app.get("/arshin/json", ctx -> {
                String num = normalize(ctx.queryParam("num"));
                LOGGER.info("Request /arshin/json for {} from {}", num, ip(ctx));
                if (num == null) {
                    throw new BadRequestResponse();
                }
                NumInfo info = Download.getNumInfo(client.get(), num, prc -> {});
                ctx.json(info);
            });
            app.get("/arshin/html", ctx -> {
                String num = normalize(ctx.queryParam("num"));
                LOGGER.info("Request /arshin/html for {} from {}", num, ip(ctx));
                if (num == null) {
                    throw new BadRequestResponse();
                }
                Map<String, Object> params = new HashMap<>();
                params.put("num", num);
                NumInfo info = Download.getNumInfo(client.get(), num, prc -> {});
                params.put("info", info);
                ctx.render("result.ftl", params);
            });

            int port = Integer.parseInt(props.getProperty("port", "8080"));
            app.start("localhost", port);
        } catch (Exception ex) {
            LOGGER.error("Startup error", ex);
        }
    }
}
