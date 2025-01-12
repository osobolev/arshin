package arshin.dto;

import java.util.List;

public final class RegInfo {

    private final List<ItemReg> regItems;

    public RegInfo(List<ItemReg> regItems) {
        this.regItems = regItems;
    }

    public List<ItemReg> getRegItems() {
        return regItems;
    }
}
