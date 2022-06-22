package arshin;

import arshin.dto.ItemReg;
import arshin.dto.ItemVerify;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

final class Parser {

    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private static String toString(Object value) {
        if (value instanceof Boolean) {
            Boolean b = (Boolean) value;
            return b.booleanValue() ? "Да" : "Нет";
        } else if (value instanceof Number) {
            Number n = (Number) value;
            return n.toString();
        } else if (value instanceof String) {
            return (String) value;
        } else if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            return array.toList().stream().map(Parser::toString).collect(Collectors.joining(", "));
        } else if (value == null || JSONObject.NULL.equals(value)) {
            return "";
        } else {
            return value.toString();
        }
    }

    static final class RegPage {

        final int totalCount;
        final int portion;

        RegPage(int totalCount, int portion) {
            this.totalCount = totalCount;
            this.portion = portion;
        }
    }

    static RegPage parseReg(JSONObject root, String num, List<ItemReg> list) throws IOException {
        int status = root.getInt("status");
        if (status != 200) {
            throw new IOException("Error" + status);
        }
        JSONObject result = root.getJSONObject("result");
        int totalCount = result.getInt("totalCount");
        JSONArray items = result.getJSONArray("items");
        for (Object anyItem : items) {
            JSONObject item = (JSONObject) anyItem;
            JSONArray properties = item.getJSONArray("properties");
            String name = "";
            String type = "";
            String manufacturer = "";
            String regNum = null;
            String id = toString(item.get("id"));
            for (Object anyProperty : properties) {
                JSONObject property = (JSONObject) anyProperty;
                String propName = property.getString("name");
                Object propValue = property.opt("value");
                if ("foei:NameSI".equals(propName)) {
                    name = toString(propValue);
                } else if ("foei:DesignationSI".equals(propName)) {
                    type = toString(propValue);
                } else if ("foei:ManufacturerTotalSI".equals(propName)) {
                    manufacturer = toString(propValue);
                } else if ("foei:NumberSI".equals(propName)) {
                    regNum = toString(propValue);
                }
            }
            if (!Objects.equals(regNum, num))
                continue;
            list.add(new ItemReg(
                name, type, manufacturer,
                "https://fgis.gost.ru/fundmetrology/registry/4/items/" + id
            ));
        }
        return new RegPage(totalCount, items.length());
    }

    static final class VerifyPage {

        final int start;
        final int numFound;
        final int portion;

        VerifyPage(int start, int numFound, int portion) {
            this.start = start;
            this.numFound = numFound;
            this.portion = portion;
        }
    }

    private static String formatDate(String dateStr) {
        if (dateStr == null)
            return "";
        try {
            LocalDateTime dt = LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return dt.format(DATE_FORMAT);
        } catch (Exception ex) {
            return dateStr;
        }
    }

    private static String unnull(JSONObject obj, String field) {
        String str = obj.optString(field);
        return str == null ? "" : str;
    }

    static VerifyPage parseVerify(JSONObject root, List<ItemVerify> list) throws IOException {
        JSONObject header = root.getJSONObject("responseHeader");
        int status = header.getInt("status");
        if (status != 0) {
            String message = null;
            Object error = root.opt("error");
            if (error instanceof JSONObject) {
                message = ((JSONObject) error).optString("msg");
            }
            throw new IOException(message == null ? "Error " + status : message);
        }
        JSONObject response = root.getJSONObject("response");
        int numFound = response.getInt("numFound");
        int start = response.getInt("start");
        JSONArray docs = response.getJSONArray("docs");
        for (Object anyItem : docs) {
            JSONObject item = (JSONObject) anyItem;
            String organization = unnull(item, "org_title");
            String typeName = unnull(item, "mi.mititle");
            String type = unnull(item, "mi.mitype");
            String modification = unnull(item, "mi.modification");
            String factoryNum = unnull(item, "mi.number");
            String verifyDate = formatDate(item.optString("verification_date"));
            String validTo = formatDate(item.optString("valid_date"));
            String docNum = unnull(item, "result_docnum");
            String acceptable = item.getBoolean("applicability") ? "ГОДЕН" : "НЕ ГОДЕН";
            String vriId = unnull(item, "vri_id");
            list.add(new ItemVerify(
                organization, typeName, type, modification, factoryNum, verifyDate, validTo, docNum, acceptable,
                "https://fgis.gost.ru/fundmetrology/cm/results/" + vriId
            ));
        }
        return new VerifyPage(start, numFound, docs.length());
    }
}
