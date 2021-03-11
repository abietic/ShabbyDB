import java.util.ArrayList;
import java.util.LinkedHashMap;

public class TableInfo {
    //表的名字
    public String tableName;

    //单条数据的长度
    public int dataSize;

    //以属性名为键值，第一个值为类型，第二个值为偏移量，第三个值为数据长度,
    // 使用LinkedHashMap来保证迭代遍历的有序性。
    // 为后面进行数据转换提供方便。
    public LinkedHashMap<String, ArrayList<Integer>> attributes;

    public TableInfo(String tableName, int dataSize, LinkedHashMap<String, ArrayList<Integer>> attributes) {
        this.tableName = tableName;
        this.dataSize = dataSize;
        this.attributes = attributes;
    }
}
