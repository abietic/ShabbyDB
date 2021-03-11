import java.util.ArrayList;

/**
 * 用于记录索引基本属性
 */
public class IndexInfo {

    //索引所属的关系名
    public String tableName;

    //索引名
    public String attributeName;

    //索引的属性信息
    //第一个值为类型，第二个值为偏移量(但是在此处是不能使用的)，第三个值为数据长度
    public ArrayList<Integer> typeAndLength;

    //构造函数
    public IndexInfo(String tableName, String attributeName, ArrayList<Integer> typeAndLength) {
        this.tableName = tableName;
        this.attributeName = attributeName;
        this.typeAndLength = typeAndLength;
    }

    //无参不赋值构造函数
    public IndexInfo() {
        typeAndLength = new ArrayList<>();
    }
}
