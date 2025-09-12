package arshin;

import arshin.dto.ItemReg;
import arshin.dto.ItemVerify;
import arshin.dto.RegInfo;
import arshin.dto.VerifyInfo;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.core5.http.*;
import smalljson.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.function.DoubleConsumer;

import static smalljson.JSONFactory.JSON;

final class Download {

    static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/139.0.0.0 Safari/537.36";

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

    private static final class Success<T> {

        final T result;

        Success(T result) {
            this.result = result;
        }
    }

    private static Success<JSONObject> parse(ClassicHttpResponse response) throws IOException {
        int code = response.getCode();
        if (code == 403 || code >= 500)
            return null;
        HttpEntity entity = response.getEntity();
        Charset encoding = getEncoding(entity, StandardCharsets.UTF_8);
        JSONObject value = JSON.parseObject(new InputStreamReader(entity.getContent(), encoding));
        return new Success<>(value);
    }

    private static JSONObject execute(HttpClient client, ClassicHttpRequest request, String referer) throws IOException {
        request.setHeader(HttpHeaders.ACCEPT, "*/*");
        request.setHeader(HttpHeaders.ACCEPT_LANGUAGE, "accept-language: en-US,en;q=0.9,ru");
        request.setHeader(HttpHeaders.REFERER, referer);
        for (int i = 0; i < 3; i++) {
            Success<JSONObject> success = client.execute(request, Download::parse);
            if (success != null)
                return success.result;
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new IOException("Request is forbidden: " + request);
    }

    static RegInfo listRegItems(HttpClient client, String num, DoubleConsumer progress) throws Exception {
        if (client == null) {
            ItemReg testItem = new ItemReg(
                "Test name", "Test type", "Test manufacturer",
                "http://localhost:8080/test"
            );
            return new RegInfo(Collections.singletonList(testItem));
        }
        List<ItemReg> list = new ArrayList<>();
        int pageNumber = 1;
        int pageSize = 20;
        while (true) {
            MyRequestBuilder buf = new MyRequestBuilder("https://fgis.gost.ru/fundmetrology/api/registry/4/data");
            buf.addParameter("pageNumber", String.valueOf(pageNumber));
            buf.addParameter("pageSize", String.valueOf(pageSize));
            buf.addParameter("orgID", "CURRENT_ORG");
            buf.addParameter("filterBy", "foei:NumberSI");
            buf.addParameter("filterValues", num);
//                buf.addParameter("filterBy", "foei:NameSI");
//                buf.addParameter("filterValues", "Энергосбыт");
            ClassicHttpRequest get = buf.build();
            JSONObject value = execute(client, get, "https://fgis.gost.ru/fundmetrology/registry/4");
            Parser.RegPage page = Parser.parseReg(value, num, list);
            if (page.portion <= 0 || list.size() >= page.totalCount)
                break;
            progress.accept((double) list.size() / page.totalCount);
            pageNumber++;
        }
        progress.accept(1.0);
        return new RegInfo(list);
    }

    static VerifyInfo listVerifyItems(HttpClient client, VerifyFilter filter, int limit, DoubleConsumer progress) throws Exception {
        if (client == null) {
            ItemVerify testItem = new ItemVerify(
                "Test org", "Test type name", "Test type", "Test modification", "Test num", "Test verify date", "Test valid to", "Test doc num", "Test acceptable",
                "http://localhost:8080/test"
            );
            return new VerifyInfo(Collections.nCopies(2, testItem), false);
        }
        List<ItemVerify> list = new ArrayList<>();
        int start = 0;
        int rows = 100;
        boolean extraItems = false;
        while (true) {
            MyRequestBuilder buf = new MyRequestBuilder("https://fgis.gost.ru/fundmetrology/cm/xcdb/vri/select");
            if (filter.regNum != null) {
                buf.addParameter("fq", "mi.mitnumber:" + filter.regNum);
            }
            if (filter.year != null) {
                buf.addParameter("fq", "verification_year:" + filter.year);
                if (filter.month != null) {
                    YearMonth ym = YearMonth.of(filter.year.intValue(), filter.month.intValue());
                    buf.addParameter("fq", String.format(
                        "verification_date:[%sT00:00:00Z TO %sT23:59:59Z]",
                        ym.atDay(1), ym.atEndOfMonth()
                    ));
                }
            }
            if (filter.serial != null) {
                buf.addParameter("fq", "mi.mititle:*Счетчики* *Счётчики*");
                StringTokenizer tok = new StringTokenizer(filter.serial, " ");
                while (tok.hasMoreTokens()) {
                    String t = tok.nextToken().trim();
                    buf.addParameter("fq", "mi.number:*" + t + "*");
                }
            }
            buf.addParameter("q", "*");
            buf.addParameter("fl", "vri_id,org_title,mi.mitnumber,mi.mititle,mi.mitype,mi.modification,mi.number,verification_date,valid_date,applicability,result_docnum,sticker_num");
            buf.addParameter("sort", "verification_date desc,org_title asc");
            buf.addParameter("start", String.valueOf(start));
            buf.addParameter("rows", String.valueOf(rows));
            ClassicHttpRequest get = buf.build();
            JSONObject value = execute(client, get, "https://fgis.gost.ru/fundmetrology/cm/results/");
            Parser.VerifyPage page = Parser.parseVerify(value, list, limit);
            if (page == null) {
                extraItems = true;
                break;
            }
            int downloaded = page.start + page.portion;
            if (page.portion <= 0 || downloaded >= page.numFound)
                break;
            start = downloaded;
            int progressMax = limit > 0 ? Math.min(page.numFound, limit) : page.numFound;
            progress.accept((double) downloaded / progressMax);
        }
        progress.accept(1.0);
        return new VerifyInfo(list, extraItems);
    }

    static final class NumInfo {

        final RegInfo regInfo;
        final VerifyInfo verifyInfo;

        NumInfo(RegInfo regInfo, VerifyInfo verifyInfo) {
            this.regInfo = regInfo;
            this.verifyInfo = verifyInfo;
        }
    }

    static NumInfo getNumInfo(HttpClient client, String num, DoubleConsumer progress) throws Exception {
        RegInfo regInfo = listRegItems(client, num, prc -> progress.accept(prc * 0.5));
        VerifyInfo verifyInfo = listVerifyItems(client, new VerifyFilter(num, null, null, null), 100, prc -> progress.accept(0.5 + prc * 0.5));
        return new NumInfo(regInfo, verifyInfo);
    }
}
