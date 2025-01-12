package arshin.dto;

import java.util.List;

public final class VerifyInfo {

    private final List<ItemVerify> verifyItems;
    private final boolean extraVerifyItems;

    public VerifyInfo(List<ItemVerify> verifyItems, boolean extraVerifyItems) {
        this.verifyItems = verifyItems;
        this.extraVerifyItems = extraVerifyItems;
    }

    public List<ItemVerify> getVerifyItems() {
        return verifyItems;
    }

    public boolean isExtraVerifyItems() {
        return extraVerifyItems;
    }

    public String getVerifyCount() {
        String size = String.valueOf(verifyItems.size());
        return extraVerifyItems ? size + "+" : size;
    }
}
