package arshin;

final class VerifyFilter {

    final String regNum;
    final Integer year;
    final Integer month;
    final String serial;

    VerifyFilter(String regNum, Integer year, Integer month, String serial) {
        this.regNum = regNum;
        this.year = year;
        this.month = month;
        this.serial = serial;
    }
}
