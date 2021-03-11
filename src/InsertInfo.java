public class InsertInfo {

    //要插入到哪个关系中
    public String tableName;

    //各个属性的值
    public Object[] attributeValues;

    public InsertInfo(String tableName, Object[] attributeValues) {
        this.attributeValues = attributeValues;
        this.tableName = tableName;
    }
}
