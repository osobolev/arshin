package arshin.dto;

import java.util.List;

public final class NumInfo {

    private final List<ItemReg> regItems;
    private final List<ItemVerify> verifyItems;

    public NumInfo(List<ItemReg> regItems, List<ItemVerify> verifyItems) {
        this.regItems = regItems;
        this.verifyItems = verifyItems;
    }

    public List<ItemReg> getRegItems() {
        return regItems;
    }

    public List<ItemVerify> getVerifyItems() {
        return verifyItems;
    }
}
