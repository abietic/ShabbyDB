public interface BPlusTreeBasicMethods {


    int GREATER_THAN = 1;
    int LESSER_THAN = -1;
    int EQUALS = 0;
    long NULL_POINTER = 0L;
    long NULL_KEY = 0L;

    /**
     * 这些方法返回指向树节点的指针
     * 我们对树节点的读取都需要通过指针
     */
    Object getNewBPlusTreeNodePointer();

    Object getRootPointer();

    /**
     * 判断一个指针是否指向叶节点
     */
    boolean isLeaf(Object bPlusTreeNodePointer);


    /**
     * 将一个指针设置成指向叶节点的指针
     */
    Object setAsPointerToLeaf(Object bPlusTreeNodePointer);


    /**
     * 通过给定的树节点指针读取出树节点
     * 再通过树节点类访问和修改其中的内容
     */
    Object getNode(Object bPlusTreeNodePointer);

    /**
     * 给出一个临时的树节点对象用于方便之后树节点分裂
     */
    Object getTempBPlusTreeNode();

    /**
     * 通过比较键值进行目标的搜索
     */
    Object getKeyValue(Object key);

    int compareKeyValue(Object keyValue1, Object keyValue2);

    /**
     * 保存你所改变的树节点
     * 这里需要你要保存的位置和你要保存的内容
     */
    void saveTreeNodeChanges(Object bPlusTreeNodePointer, Object bPlusTreeNode);


    /**
     * 在树结构发生变动后当根节点发生改变时要改变根节点的指针
     */
    void setTreeRoot(Object bPlusTreeNodePointer);


    /**
     * 得到该树的索引名
     */
    String getAttributeName();
}
