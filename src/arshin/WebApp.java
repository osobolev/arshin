package arshin;

import arshin.dto.VerifyInfo;
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

    private static String normalize(String param) {
        if (param == null)
            return null;
        String trimmed = param.trim();
        if (trimmed.isEmpty())
            return null;
        return trimmed;
    }

    private static Integer tryParse(String str) {
        if (str == null || str.isEmpty())
            return null;
        return Integer.valueOf(str);
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
                builder.setUserAgent(Download.USER_AGENT);
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
            app.get("/arshin/html", ctx -> {
                String num = normalize(ctx.queryParam("num"));
                LOGGER.info("Request {} for {} from {}", ctx.matchedPath(), num, ip(ctx));
                if (num == null) {
                    throw new BadRequestResponse();
                }
                Download.NumInfo info = Download.getNumInfo(client.get(), num, prc -> {});
                Map<String, Object> params = new HashMap<>();
                params.put("num", num);
                params.put("regInfo", info.regInfo);
                params.put("verifyInfo", info.verifyInfo);
                ctx.render("result.ftl", params);
            });
            app.get("/arshin2/html", ctx -> {
                String serial = normalize(ctx.queryParam("serial"));
                String yearStr = normalize(ctx.queryParam("year"));
                String monthStr = normalize(ctx.queryParam("month"));
                LOGGER.info("Request {} for {}/{}/{} from {}", ctx.matchedPath(), serial, yearStr, monthStr, ip(ctx));
                if (serial == null) {
                    throw new BadRequestResponse();
                }
                Integer year;
                Integer month;
                try {
                    year = tryParse(yearStr);
                    month = tryParse(monthStr);
                } catch (NumberFormatException ex) {
                    throw new BadRequestResponse();
                }
                VerifyFilter filter = new VerifyFilter(null, year, month, serial);
                VerifyInfo verifyInfo = Download.listVerifyItems(client.get(), filter, 200, prc -> {});
                Map<String, Object> params = new HashMap<>();
                params.put("serial", serial);
                params.put("verifyInfo", verifyInfo);
                ctx.render("result2.ftl", params);
            });

            int port = Integer.parseInt(props.getProperty("port", "8080"));
            app.start("localhost", port);
        } catch (Exception ex) {
            LOGGER.error("Startup error", ex);
        }
    }
}
