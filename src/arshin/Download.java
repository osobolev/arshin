package arshin;

import arshin.dto.ItemReg;
import arshin.dto.ItemVerify;
import arshin.dto.NumInfo;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.net.URIBuilder;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.DoubleConsumer;

final class Download {

    private static Charset getEncoding(HttpEntity entity, Charset def) {
        String contentType = entity.getContentType();
        if (contentType == null)
            return def;
        try {
            ContentType parsed = ContentType.parse(contentType);
            Charset charset = parsed.getCharset();
            return charset == null ? def : charset;
        } catch (Exception ex) {
            return def;
        }
    }

    private static JSONObject parse(CloseableHttpResponse response) throws IOException {
        HttpEntity entity = response.getEntity();
        Charset encoding = getEncoding(entity, StandardCharsets.UTF_8);
        JSONTokener parser = new JSONTokener(new InputStreamReader(entity.getContent(), encoding));
        return (JSONObject) parser.nextValue();
    }

    static List<ItemReg> listRegItems(CloseableHttpClient client, String num, DoubleConsumer progress) throws Exception {
        if (client == null) {
            ItemReg testItem = new ItemReg(
                "Test name", "Test type", "Test manufacturer",
                "http://localhost:8080/test"
            );
            return Collections.singletonList(testItem);
        }
        List<ItemReg> list = new ArrayList<>();
        int pageNumber = 1;
        int pageSize = 20;
        while (true) {
            URIBuilder buf = new URIBuilder("https://fgis.gost.ru/fundmetrology/api/registry/4/data");
            buf.addParameter("pageNumber", String.valueOf(pageNumber));
            buf.addParameter("pageSize", String.valueOf(pageSize));
            buf.addParameter("orgID", "CURRENT_ORG");
            buf.addParameter("filterBy", "foei:NumberSI");
            buf.addParameter("filterValues", num);
//                buf.addParameter("filterBy", "foei:NameSI");
//                buf.addParameter("filterValues", "Энергосбыт");
            HttpGet get = new HttpGet(buf.build());
            try (CloseableHttpResponse response = client.execute(get)) {
                JSONObject value = parse(response);
                Parser.RegPage page = Parser.parseReg(value, num, list);
                if (page.portion <= 0 || list.size() >= page.totalCount) {
                    progress.accept(1.0);
                    break;
                }
                progress.accept((double) list.size() / page.totalCount);
                pageNumber++;
            }
        }
        return list;
    }

    static List<ItemVerify> listVerifyItems(CloseableHttpClient client, String num, DoubleConsumer progress) throws Exception {
        if (client == null) {
            ItemVerify testItem = new ItemVerify(
                "Test org", "Test type name", "Test type", "Test modification", "Test num", "Test verify date", "Test valid to", "Test doc num", "Test acceptable",
                "http://localhost:8080/test"
            );
            return Collections.nCopies(2, testItem);
        }
        List<ItemVerify> list = new ArrayList<>();
        int start = 0;
        int rows = 20;
        while (true) {
            URIBuilder buf = new URIBuilder("https://fgis.gost.ru/fundmetrology/cm/xcdb/vri/select");
            buf.addParameter("fq", "mi.mitnumber:" + num);
            buf.addParameter("q", "*");
            buf.addParameter("fl", "vri_id,org_title,mi.mitnumber,mi.mititle,mi.mitype,mi.modification,mi.number,verification_date,valid_date,applicability,result_docnum,sticker_num");
            buf.addParameter("sort", "verification_date desc,org_title asc");
            buf.addParameter("start", String.valueOf(start));
            buf.addParameter("rows", String.valueOf(rows));
            HttpGet get = new HttpGet(buf.build());
            try (CloseableHttpResponse response = client.execute(get)) {
                JSONObject value = parse(response);
                Parser.VerifyPage page = Parser.parseVerify(value, list);
                int downloaded = page.start + page.portion;
                if (page.portion <= 0 || downloaded >= page.numFound) {
                    progress.accept(1.0);
                    break;
                }
                start = downloaded;
                progress.accept((double) downloaded / page.numFound);
            }
        }
        return list;
    }

    static NumInfo getNumInfo(CloseableHttpClient client, String num, DoubleConsumer progress) throws Exception {
        List<ItemReg> regItems = listRegItems(client, num, prc -> progress.accept(prc * 0.5));
        List<ItemVerify> verifyItems = listVerifyItems(client, num, prc -> progress.accept(0.5 + prc * 0.5));
        return new NumInfo(regItems, verifyItems);
    }
}
