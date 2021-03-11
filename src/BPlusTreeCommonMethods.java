public interface BPlusTreeCommonMethods {


    /**
     * B树的基本方法
     */
    void insert(Object key, Object dataPointer);

    void delete(Object key, Object dataPointer);

    /**
     * 返回的是目标数据的指针数组
     */
    Object[] find(Object fromKey, Object toKey);
    //显示表中所有数据
    Object[] showAll();

    /**
     * 获得该树的索引名
     */
    String getAttributeName();
}
