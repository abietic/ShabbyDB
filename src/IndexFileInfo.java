/**
 * 索引文件所需的全部信息
 */
public class IndexFileInfo {

    //索引基本属性
    public IndexInfo indexInfo;

    //树节点的N值用于给出树节点实例时使用
    public int N;

    //根节点指针
    public long rootPointer;

    //下一个可用于存储的节点指针
    public long nextPointer;

    //构造方法
    public IndexFileInfo(IndexInfo indexInfo, int n, long rootPointer, long nextPointer) {
        this.indexInfo = indexInfo;
        this.N = n;
        this.rootPointer = rootPointer;
        this.nextPointer = nextPointer;
    }

    //无参不赋值构造函数
    public IndexFileInfo() {
        indexInfo = new IndexInfo();
    }

    public int getKeyType() {
        return this.indexInfo.typeAndLength.get(TableFile.TYPE);
    }

    public int getKeyLength() {
        return this.indexInfo.typeAndLength.get(TableFile.LENGTH);
    }
}
