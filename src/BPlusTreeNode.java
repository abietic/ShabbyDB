import java.nio.ByteBuffer;

public class BPlusTreeNode implements DataTypes, BPlusTreeNodeMethods {


    private byte[] bytes;
    private int N;
    private int keyType;
    private int keyLength;
    //现有的Pointer数
    private int size;

    /**
     * 构造方法
     * 从给出的byte数组中
     * 读出树节点头
     * 获取树节点信息
     */
    public BPlusTreeNode(byte[] nodeInfo) {


        //读入树节点头
        long nodeHeader = ByteBuffer.wrap(nodeInfo).getLong();

        //得到size
        this.size = (int) (Long.parseUnsignedLong("000000000000001F", 16) & nodeHeader);

        //得到keyType
        nodeHeader >>>= 5;
        this.keyType = (int) (Long.parseUnsignedLong("0000000000000003", 16) & nodeHeader);

        //得到keyLength
        nodeHeader >>>= 2;
        this.keyLength = (int) (Long.parseUnsignedLong("00000000000000FF", 16) & nodeHeader);

        //得到N
        nodeHeader >>>= 8;
        this.N = (int) (Long.parseUnsignedLong("000000000000001F", 16) & nodeHeader);

        //得到byte数组
        this.bytes = nodeInfo;
    }

    @Override
    public Object getKey(int index) {

        //如果越界返回空指针，值得注意的是下标是从1到(size - 1)的
        if (index >= this.size || index <= 0) {
            System.err.println(index);
            new Exception("node key visit out of range").printStackTrace();
            return null;
        }


        //计算出值在节点中的偏移量
        int offset = (this.N + 1) * pointerSize + (index - 1) * this.keyLength;

        //根据其类型返回相应对象
        switch (this.keyType) {
            case INT:
                return new Long(ByteBuffer.wrap(this.bytes).getLong(offset));
            case FLOAT:
                return new Double(ByteBuffer.wrap(this.bytes).getDouble(offset));
            case CHAR:
                byte[] bytesForString = new byte[this.keyLength];
                ByteBuffer.wrap(this.bytes , offset , this.keyLength).get(bytesForString);
                //System.out.println("In getKey method");
                String ress = new String(bytesForString);
                //System.out.println(ress);
                String[] res = ress.split("\0");
                if (res.length == 0) {
                    System.out.println("In getKey method");
                    System.out.println(this.size);
                    System.out.println(this.N);
                    System.out.println(index);
                    System.out.println("Out getKey method");
                }
                //System.out.println(res.length);
                //System.out.println("Out getKey method");
                return res[0];
            default:
                break;
        }

        return null;

    }

    @Override
    public void setKey(int index, Object key) {

        //检查是否越界
        if (index >= this.N || index <= 0) {
            System.err.println(index);
            new Exception("node key modify out of range").printStackTrace();
            return;
        }

        //检查类型是否匹配
        switch (this.keyType) {
            case INT:
                if (!(key instanceof Long)) {
                    new Exception("Key type not match").printStackTrace();
                    return;
                }
                break;
            case FLOAT:
                if (!(key instanceof Double)) {
                    new Exception("Key type not match").printStackTrace();
                    return;
                }
                break;
            case CHAR:
                if (!(key instanceof String)) {
                    System.err.println(key);
                    new Exception("Key type not match").printStackTrace();
                    return;
                }
                break;
            default:
                new Exception("Key type not match in basic types").printStackTrace();
                return;
        }

        //计算其在节点中的偏移量
        int offset = (this.N + 1) * pointerSize + (index - 1) * this.keyLength;

        //进行key的写入
        switch (this.keyType) {
            case INT:
                ByteBuffer.wrap(this.bytes).putLong(offset, (long) key);
                break;
            case FLOAT:
                ByteBuffer.wrap(this.bytes).putDouble(offset, (double) key);
                break;
            case CHAR:
                ByteBuffer.wrap(this.bytes, offset, this.keyLength).put(ByteBuffer.allocate(this.keyLength).put(((String) key).getBytes()).array());
                break;
            default:
                break;
        }

    }

    @Override
    public Object getPointer(int index) {

        //检查是否越界
        if (index <= 0 || index > this.size) {
            System.err.println(index);
            new Exception("node pointer visit out of range").printStackTrace();
            return null;
        }

        //计算偏移量
        int offset = pointerSize + (index - 1) * pointerSize;


        return new Long(ByteBuffer.wrap(this.bytes).getLong(offset));
    }

    @Override
    public void setPointer(int index, Object pointer) {

        //检查是否越界
        if (index <= 0 || index > this.N) {
            System.err.println(index);
            new Exception("node pointer modify out of range").printStackTrace();
            return;
        }

        //检查类型
        if (!(pointer instanceof Long)) {
            System.err.println(pointer);
            new Exception("it's not a pointer").printStackTrace();
            return;
        }

        //计算偏移量
        int offset = pointerSize + (index - 1) * pointerSize;

        //放入byte数组中
        ByteBuffer.wrap(this.bytes).putLong(offset, (long) pointer);

    }

    /**
     * 得到当前树节点所存储的指针数
     */
    @Override
    public int getSize() {
        return this.size;
    }

    public void setSize(int size) {

        //将更改后的大小放入成员变量并更改byte数组中的header
        this.size = size;

        ByteBuffer byteBuffer = ByteBuffer.wrap(this.bytes);

        //取出数组中的header
        long header = byteBuffer.getLong();

        //清空header中原来的size
        //header &= Long.parseUnsignedLong("FFFFFFFFFFFFFFE0" , 16);
        header >>>= 5;
        header <<= 5;

        //放入新的size
        header |= (long) this.size;

        //将该改写后的头存回数组
        byteBuffer.putLong(0 , header);

    }

    /**
     * 得到树节点的N值
     */
    @Override
    public int getN() {
        return this.N;
    }

    /**
     * 得到树节点在真正存储时的字节数组
     * */
    public byte[] getBytes() {
        return this.bytes;
    }

/*    public static void main(String[] args) {


        IndexFile indexFile = new IndexFile();

        indexFile.setIndexFile("student_info" , "student_id");

        long pointer = (long) indexFile.setAsPointerToLeaf(indexFile.getNewBPlusTreeNodePointer());

        BPlusTreeNode treeNode = (BPlusTreeNode) indexFile.getTempBPlusTreeNode();

        treeNode.setPointer(1,1L);

        treeNode.setKey(1,"13");

        treeNode.setPointer(2,2L);

        treeNode.setKey(2,"19");

        treeNode.setPointer(3,3L);

        treeNode.setSize(3);

        indexFile.saveTreeNodeChanges(pointer , treeNode);

        indexFile.setTreeRoot(pointer);

        long temp = (long) indexFile.getRootPointer();

        System.out.println(indexFile.isLeaf(temp));

        System.out.println(temp == pointer);

        BPlusTreeNode tempNode = (BPlusTreeNode) indexFile.getNode(temp);

        for (int i = 1 ; i < tempNode.getSize() ; i ++) {
            System.out.println(tempNode.getKey(i));

            System.out.println(tempNode.getPointer(i));

        }

        System.out.println(tempNode.getPointer(3));

        System.out.println(indexFile.compareKeyValue(tempNode.getKey(1),tempNode.getKey(2)));


    }*/
}
