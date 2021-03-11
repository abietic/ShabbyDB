public interface DataTypes {
    int INT = 1;
    int FLOAT = 2;
    int CHAR = 3;

    static int judgeType(String type) {
        switch (type) {
            case "int":
                return INT;
            case "float":
                return FLOAT;
            case "char":
                return CHAR;
        }
        return 0;
    }

    static int getTypeLength(int type) {
        int result = 0;
        switch (type) {
            case INT:
            case FLOAT:
                result = 8;
                break;
            case CHAR:
                result = 2;
                break;

            default:
                break;
        }

        return result;
    }
}
