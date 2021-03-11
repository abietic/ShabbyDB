public interface BPlusTreeNodeMethods {


    /**
     * 一个树节点的最大长度
     */
    int nodeSize = 256;

    /**
     * 一个指针的长度
     */
    int pointerSize = 8;

    /**
     * 最大的N值
     */
    int maxN = 16;


    /**
     * 未赋值与越界情况下返回空值
     */
    Object EMPTY = null;


    /**初始规定每个树节点规定有N个指针，随数据长度的变化可能会减少*/


    /**
     * 取出第index个键值，
     * 与修改第index个键值
     */
    Object getKey(int index);

    void setKey(int index, Object key);


    /**
     * 得到第index个指针
     * 与设置第index个指针
     */
    Object getPointer(int index);

    void setPointer(int index, Object pointer);


    /**
     * 得到当前树节点所存储的指针数
     */
    int getSize();

    /**
     * 设置当前树节点所存储的指针数,用于在对节点进行更新时使用
     */
    void setSize(int newSize);

    /**
     * 得到树节点的N值
     */
    int getN();
}
