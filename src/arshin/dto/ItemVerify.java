package arshin.dto;

public final class ItemVerify {

    private final String organization;
    private final String typeName;
    private final String type;
    private final String modification;
    private final String factoryNum;
    private final String verifyDate;
    private final String validTo;
    private final String docNum;
    private final String acceptable;
    private final String link;

    public ItemVerify(String organization, String typeName, String type, String modification, String factoryNum, String verifyDate, String validTo, String docNum, String acceptable, String link) {
        this.organization = organization;
        this.typeName = typeName;
        this.type = type;
        this.modification = modification;
        this.factoryNum = factoryNum;
        this.verifyDate = verifyDate;
        this.validTo = validTo;
        this.docNum = docNum;
        this.acceptable = acceptable;
        this.link = link;
    }

    public String getOrganization() {
        return organization;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getType() {
        return type;
    }

    public String getModification() {
        return modification;
    }

    public String getFactoryNum() {
        return factoryNum;
    }

    public String getVerifyDate() {
        return verifyDate;
    }

    public String getValidTo() {
        return validTo;
    }

    public String getDocNum() {
        return docNum;
    }

    public String getAcceptable() {
        return acceptable;
    }

    public String getLink() {
        return link;
    }
}
