package arshin.dto;

public final class ItemReg {

    private final String name;
    private final String type;
    private final String manufacturer;
    private final String link;

    public ItemReg(String name, String type, String manufacturer, String link) {
        this.name = name;
        this.type = type;
        this.manufacturer = manufacturer;
        this.link = link;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getLink() {
        return link;
    }
}
