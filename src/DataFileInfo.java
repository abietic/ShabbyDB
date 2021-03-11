public class DataFileInfo {

    //关系表名
    public String tableName;

    //单条数据长度
    public int dataSize;

    //下一条数据可存放的虚拟地址
    public long nextPointer;

    public DataFileInfo() {
    }

    public DataFileInfo(String tableName, int dataSize) {
        this.tableName = tableName;
        this.dataSize = dataSize;
        this.nextPointer = 1;
    }
}
