package arshin;

import arshin.dto.NumInfo;
import freemarker.cache.ClassTemplateLoader;
import freemarker.core.HTMLOutputFormat;
import freemarker.template.Configuration;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.staticfiles.Location;
import io.javalin.plugin.rendering.JavalinRenderer;
import io.javalin.plugin.rendering.template.JavalinFreemarker;
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
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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

    private static void setupProxy(Properties props, HttpClientBuilder builder) throws URISyntaxException {
        String host = props.getProperty("proxy.host");
        String port = props.getProperty("proxy.port");
        if (host == null || port == null)
            return;

        HttpHost proxy = HttpHost.create(host + ":" + port);
        builder.setProxy(proxy);

        String user = props.getProperty("proxy.user");
        String password = props.getProperty("proxy.password");
        if (user != null) {
            BasicCredentialsProvider creds = new BasicCredentialsProvider();
            creds.setCredentials(new AuthScope(proxy), new UsernamePasswordCredentials(user, password == null ? null : password.toCharArray()));
            builder.setDefaultCredentialsProvider(creds);
        }
    }

    private static boolean isDebug(Properties props) {
        Properties[] toTry = {System.getProperties(), props};
        for (Properties p : toTry) {
            if (Boolean.parseBoolean(p.getProperty("debug", "false")))
                return true;
        }
        return false;
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

            JavalinRenderer.register(JavalinFreemarker.INSTANCE);
            Configuration ftlConfig = new Configuration(Configuration.VERSION_2_3_31);
            ftlConfig.setOutputFormat(HTMLOutputFormat.INSTANCE);
            ftlConfig.setDefaultEncoding("UTF-8");
            ftlConfig.setTemplateLoader(new ClassTemplateLoader(WebApp.class, "/"));
            JavalinFreemarker.configure(ftlConfig);

            HttpClientBuilder builder = HttpClients.custom();
            setupProxy(props, builder);
            PoolingHttpClientConnectionManager cman = new PoolingHttpClientConnectionManager();
            cman.setMaxTotal(50);
            CloseableHttpClient client = builder.setConnectionManager(cman).build();

            Javalin app = Javalin.create(cfg -> {
                cfg.showJavalinBanner = false;
                boolean debug = isDebug(props);
                if (debug) {
                    cfg.addStaticFiles("resources/public", Location.EXTERNAL);
                } else {
                    cfg.addStaticFiles("/public", Location.CLASSPATH);
                }
            });
            app.get("/arshinJson", ctx -> {
                String num = normalize(ctx.queryParam("num"));
                if (num == null) {
                    throw new BadRequestResponse();
                }
                NumInfo info = Download.getNumInfo(client, num, prc -> {});
                ctx.json(info);
            });
            app.get("/arshinHtml", ctx -> {
                String num = normalize(ctx.queryParam("num"));
                Map<String, Object> params = new HashMap<>();
                if (num != null) {
                    params.put("num", num);
                    try {
//                        Thread.sleep(5000);
                        NumInfo info = Download.getNumInfo(client, num, prc -> {});
                        params.put("info", info);
                    } catch (Exception ex) {
                        LOGGER.error("Backend error", ex);
                        params.put("error", true);
                    }
                }
                ctx.render("result.ftl", params);
            });

            int port = Integer.parseInt(props.getProperty("port", "8080"));
            app.start(port);
        } catch (Exception ex) {
            LOGGER.error("Startup error", ex);
        }
    }
}
