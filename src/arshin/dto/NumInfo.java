package arshin.dto;

import java.util.List;

public final class NumInfo {

    private final List<ItemReg> regItems;
    private final List<ItemVerify> verifyItems;
    private final boolean extraVerifyItems;

    public NumInfo(List<ItemReg> regItems, List<ItemVerify> verifyItems, boolean extraVerifyItems) {
        this.regItems = regItems;
        this.verifyItems = verifyItems;
        this.extraVerifyItems = extraVerifyItems;
    }

    public List<ItemReg> getRegItems() {
        return regItems;
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
