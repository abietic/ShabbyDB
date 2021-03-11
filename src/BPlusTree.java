import java.util.ArrayList;
import java.util.Deque;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingDeque;

public class BPlusTree implements BPlusTreeCommonMethods {

    //提供了用于B+树检索与维护的最基本操作
    private BPlusTreeBasicMethods bPlusTreeBasicMethods;

    //存储在查找时的游历路径用于接下来的操作
    private Stack travelPath = null;

    private int currentHeight = 0;

    //构造函数
    public void setBPlusTree(IndexFile indexFile) {

        this.bPlusTreeBasicMethods = indexFile;
    }


    /**
     * B树的基本方法
     *
     * @param key
     * @param dataPointer
     */
    @Override
    public void insert(Object key, Object dataPointer) {

        //如果是空树
        if (bPlusTreeBasicMethods.getRootPointer().equals(BPlusTreeBasicMethods.NULL_POINTER)) {

            //向空树中插入节点
            insertInEmptyTree(key, dataPointer);
            return;
        }


        //如果不是空树

        //进行树的检索
        //期间将搜索路径保存
        Object targetLeafPointer = findTargetLeafPointer(key);

        //插入叶节点
        //并判断是否发生了分裂
        Object splitedHalfPointer = insertInLeaf(targetLeafPointer, key, dataPointer);

        //如果未发生分裂直接返回即可
        if (splitedHalfPointer == null) {
            return;
        }

        //如果发生了分裂
        //将会对树的结构进行改变

        //得到新分裂出的叶节点
        BPlusTreeNode splitedHalf = (BPlusTreeNode) bPlusTreeBasicMethods.getNode(splitedHalfPointer);

        //得到新分裂出的节点为父节点提供用于检索的键值
        Object keyForSplitedHalf = splitedHalf.getKey(1);

        //对父节点递归/迭代地进行修改
        insertInParent(targetLeafPointer, keyForSplitedHalf, splitedHalfPointer);

    }

    /**
     * 向父节点中插入新分裂出的节点
     * 的搜索键值与指向该节点的指针
     * <p>
     * 注：
     * <p>
     * 注：这里可以看出向叶节点中插入时
     * 要插入的指针在要插入的键值左侧
     * 而在向非叶节点中插入时要插入的
     * 指针在要插入的键值的左侧
     * 这是两种情况方法无法复用
     * 的原因之一
     * <p>
     * 注：在父节点分裂时，为祖父提供的键值
     * 是从键值中提取，即下层会减少一个键值
     */
    private void insertInParent(Object originalPointer, Object keyForSplitedHalf, Object splitedHalfPointer) {

        //如果游历路径栈中已经为空
        //而又进入了本方法，说明原根节点分裂
        //需要产生新的根节点，完成插入并返回
        if (this.travelPath.empty()) {
            createAndSaveANewRoot(originalPointer, keyForSplitedHalf, splitedHalfPointer);
            return;
        }

        //如果栈未空，取出栈顶的指向父节点的指针，及偏移量
        int offset = (int) this.travelPath.pop();

        Object parentPointer = this.travelPath.pop();

        //得到父节点
        BPlusTreeNode parentNode = (BPlusTreeNode) bPlusTreeBasicMethods.getNode(parentPointer);

        //如果父节点未满直接插入不进行分裂并返回
        if (parentNode.getSize() < parentNode.getN()) {

            //进行父节点的修改
            insertInNodeWithoutSplit(parentNode, offset, keyForSplitedHalf, splitedHalfPointer);

            //保存父节点的修改
            bPlusTreeBasicMethods.saveTreeNodeChanges(parentPointer, parentNode);

            //结束
            return;
        }

        //如果父节点已满则会使在插入时发生分裂
        //这时就需要递归调用本方法

        //得到分裂出来的新节点
        BPlusTreeNode splitedParentNode = (BPlusTreeNode) insertInNodeWithSplit(parentNode, offset, keyForSplitedHalf, splitedHalfPointer);

        //得到新的为祖父节点插入提供的键值
        Object keyForSplitedParent = parentNode.getKey(parentNode.getSize() - 1);

        //将提供给祖父节点的键值从原父节点中删去
        parentNode.setSize(parentNode.getSize() - 1);


        //保存父节点的修改
        bPlusTreeBasicMethods.saveTreeNodeChanges(parentPointer, parentNode);

        //为新的节点申请一个指针
        Object splitedParentPointer = bPlusTreeBasicMethods.getNewBPlusTreeNodePointer();

        //将新生节点保存至文件
        bPlusTreeBasicMethods.saveTreeNodeChanges(splitedParentPointer, splitedParentNode);

        //递归调用
        insertInParent(parentPointer, keyForSplitedParent, splitedParentPointer);

    }


    /**
     * 会产生分裂的非叶节点插入
     * <p>
     * 返回分裂出的节点
     * <p>
     * 原非叶节点的最后一个键值被用于给
     * 其父节点提供键值
     */
    private Object insertInNodeWithSplit(BPlusTreeNode parentNode, int offset, Object keyForSplitedHalf, Object splitedHalfPointer) {
        //计算出插入位置和天棚位置
        int destination = offset, halfRoof = (parentNode.getN() / 2) + (parentNode.getN() % 2);

        //为分裂节点创建临时空间
        BPlusTreeNode splitedParent = (BPlusTreeNode) bPlusTreeBasicMethods.getTempBPlusTreeNode();

        //判断是插在前半段还是后半段
        if ((halfRoof + 1) >= destination) {
            //插入在前半段
            //将不会变化的后半段复制到分裂的新节点
            //由于有一个键值是要上交给下一次递归插入的，所以跳过一个开始复制
            int from = halfRoof + 1, to = 1;
            for (; from < parentNode.getN(); ++from, ++to) {

                splitedParent.setKey(to, parentNode.getKey(from));

                splitedParent.setPointer(to + 1, parentNode.getPointer(from + 1));
            }

            //调整原节点与分裂节点的大小
            parentNode.setSize(halfRoof + 1);

            splitedParent.setSize(to);

            //对原节点进行无分裂插入
            insertInNodeWithoutSplit(parentNode, offset, keyForSplitedHalf, splitedHalfPointer);

            //设置分裂出的节点的第一个指针
            splitedParent.setPointer(1, parentNode.getPointer(parentNode.getSize()));
        } else {
            //插入在后半段

            //前半段不会变化只把后半段放入分裂的节点，并对分裂的节点进行无分裂非叶节点插入
            int from = halfRoof + 2, to = 1;
            for (; from < parentNode.getN(); ++from, ++to) {

                splitedParent.setKey(to, parentNode.getKey(from));

                splitedParent.setPointer(to + 1, parentNode.getPointer(from + 1));
            }

            //调整节点大小
            parentNode.setSize(halfRoof + 2);

            splitedParent.setSize(to);

            //插入后半段时要对偏移位置进行调整
            insertInNodeWithoutSplit(splitedParent, destination - (halfRoof + 1), keyForSplitedHalf, splitedHalfPointer);

            //设置分裂出的节点的第一个指针
            splitedParent.setPointer(1, parentNode.getPointer(parentNode.getSize()));

        }
        //System.out.println("has splited");
        return splitedParent;
    }

    /***
     * 不会产生分裂的非叶节点插入
     */
    private void insertInNodeWithoutSplit(BPlusTreeNode parentNode, int offset, Object keyForSplitedHalf, Object splitedHalfPointer) {

        //由于已经给出了原节点在父节点中的位置，只需将新产生的键值和指针插在原节点之后就可以了

        //为新产生的节点和指针腾出位置
        parentNode.setSize(parentNode.getSize() + 1);

        //挪动键值,从键值尾到原节点键值前
        for (int i = parentNode.getSize() - 1; i > offset; --i) {

            parentNode.setKey(i, parentNode.getKey(i - 1));
        }

        //挪动指针，由于指针是新插入键值的右侧指针，位置量比键值大1
        for (int i = parentNode.getSize(); i > offset + 1; --i) {

            parentNode.setPointer(i, parentNode.getPointer(i - 1));
        }

        //将键值与指针插入腾出的空位中
        parentNode.setPointer(offset + 1, splitedHalfPointer);

        parentNode.setKey(offset, keyForSplitedHalf);
    }

    /**
     * 用于在插入不會引起分裂的非葉節點中
     * 未指明插入位置的插入
     */
    private void insertInNodeWithoutSplit(BPlusTreeNode node, Object key, Object pointer) {

        int i = 1;

        //找到插入位置，即在原节点中第一个大于该键的位置，要是没有大于的就插入节点尾
        for (i = 1; i < node.getSize(); ++i) {
            if (this.bPlusTreeBasicMethods.compareKeyValue(node.getKey(i), key) == BPlusTreeBasicMethods.GREATER_THAN) {
                break;
            }
        }

        //进行插入
        insertInNodeWithoutSplit(node, i, key, pointer);

    }


    /**
     * 将键值与data文件提供的pointer
     * 通过目标叶节点的指针
     * 插入到目标叶节点中
     * <p>
     * 将会分为两种情况：
     * 1.插入之后不会发生分裂
     * 2.插入之后会发生分裂
     * <p>
     * 返回：
     * 如果没发生分裂返回null
     * 如果发生了分裂返回分裂出的
     * 新树节点的指针(已经进行了保存)
     *
     * @return BPlusTreeNodePointer
     */
    private Object insertInLeaf(Object targetLeafPointer, Object key, Object dataPointer) {


        //存储返回结果
        //即可能的分裂后的新叶节点
        Object splitedHalfPointer = null;

        //取出目标叶节点
        BPlusTreeNode targetLeafNode = (BPlusTreeNode) bPlusTreeBasicMethods.getNode(targetLeafPointer);

        //如果pointer数量小于N
        if (targetLeafNode.getSize() < targetLeafNode.getN()) {

            //无需分裂即可完成插入
            insertInLeafWithoutSplit(targetLeafNode, key, dataPointer);

        } else {//如果节点中的pointer数量已经达到N了

            //插入需要进行分裂
            BPlusTreeNode splitedHalf = (BPlusTreeNode) insertInLeafWithSplit(targetLeafNode, key, dataPointer);

            //得到一个新的叶节点指针，用于存放新产生的叶节点
            splitedHalfPointer = bPlusTreeBasicMethods.setAsPointerToLeaf(bPlusTreeBasicMethods.getNewBPlusTreeNodePointer());

            //将新产生的叶节点保存至索引文件
            bPlusTreeBasicMethods.saveTreeNodeChanges(splitedHalfPointer, splitedHalf);

            //要将原叶节点的尾指针更新成新产生的叶节点的指针
            targetLeafNode.setPointer(targetLeafNode.getSize(), splitedHalfPointer);
        }

        //对进行了修改的原叶节点进行保存
        bPlusTreeBasicMethods.saveTreeNodeChanges(targetLeafPointer, targetLeafNode);

        //返回可能存在的分裂出的新叶节点
        return splitedHalfPointer;
    }


    /**
     * 叶节点已满需要进行分裂
     * 并将新产生的叶节点返回
     * <p>
     * <p>
     * 注：由于考虑不周导致
     * 分裂完成后旧叶节点的
     * 尾指针没有被置为新分
     * 裂出的叶节点指针在调
     * 用本函数之后，还需将
     * 旧叶节点的尾指针赋值
     */
    private Object insertInLeafWithSplit(BPlusTreeNode targetLeafNode, Object key, Object dataPointer) {

        //得到分裂的新节点
        BPlusTreeNode splitNode = (BPlusTreeNode) bPlusTreeBasicMethods.getTempBPlusTreeNode();

        //得到键值和指针插入位置
        int offset;

        for (offset = 1; offset < targetLeafNode.getN(); ++offset) {

            //找到了键值插入位置
            if (bPlusTreeBasicMethods.compareKeyValue(targetLeafNode.getKey(offset), key) == BPlusTreeBasicMethods.GREATER_THAN) {
                break;
            }
        }
        //如果没有位置插入了,插在最后

        //判断要插入的位置是前半段还是后半段

        //前半节点的界限
        int halfRoof = (targetLeafNode.getN() / 2) + (targetLeafNode.getN() % 2);

        //插入位置在前半段
        if (halfRoof >= offset) {

            //将顺序不变的后部分复制到分裂节点
            int from = halfRoof, to = 1;
            for (; from < targetLeafNode.getN(); ++from, ++to) {

                splitNode.setKey(to, targetLeafNode.getKey(from));

                splitNode.setPointer(to, targetLeafNode.getPointer(from));
            }

            //将前半部分的叶节点中用于串联叶节点的指针复制到分裂的叶节点
            splitNode.setPointer(to, targetLeafNode.getPointer(from));

            //重新指明分裂后两节点的尺寸
            splitNode.setSize(to);

            //为要插入的值留出空位
            targetLeafNode.setSize(halfRoof);

            //对前半段进行无分裂插入
            insertInLeafWithoutSplit(targetLeafNode, key, dataPointer);

        } else {//插入位置在后半段

            //前面的halfRoof个节点无需改变，仅需将后面的元素复制到分裂出的节点并进行无分裂插入
            int from = halfRoof + 1, to = 1;
            for (; from < targetLeafNode.getN(); ++from, ++to) {

                splitNode.setKey(to, targetLeafNode.getKey(from));

                splitNode.setPointer(to, targetLeafNode.getPointer(from));
            }

            //将前半部分的叶节点中用于串联叶节点的指针复制到分裂的叶节点
            splitNode.setPointer(to, targetLeafNode.getPointer(from));

            //重新指明分裂后两节点的尺寸
            targetLeafNode.setSize(halfRoof + 1);

            splitNode.setSize(to);

            //对后半段进行无分裂插入
            insertInLeafWithoutSplit(splitNode, key, dataPointer);
        }
        return splitNode;
    }

    /**
     * 将键值与指针直接插入到叶节点中
     * 其中如有相等键值
     * 直接插入到相等的键值后就可以了
     */
    private void insertInLeafWithoutSplit(BPlusTreeNode targetLeafNode, Object key, Object dataPointer) {

        //存储键值插入位置
        int offset;

        //搜索键值插入位置
        for (offset = 1; offset < targetLeafNode.getSize(); ++offset) {

            //找到了键值插入位置
            if (bPlusTreeBasicMethods.compareKeyValue(targetLeafNode.getKey(offset), key) == BPlusTreeBasicMethods.GREATER_THAN) {
                break;
            }
        }
        //如果没有位置插入了,插在最后


        //指针的插入位置与键值插入位置相同

        //将叶节点的尺寸变大
        targetLeafNode.setSize(targetLeafNode.getSize() + 1);


        //处理键值和指针部分
        //将原有的位置的键值和指针搬走为插入值空出位置
        for (int i = targetLeafNode.getSize(); i > offset; --i) {
            targetLeafNode.setPointer(i, targetLeafNode.getPointer(i - 1));
        }

        for (int i = targetLeafNode.getSize() - 1; i > offset; --i) {
            targetLeafNode.setKey(i, targetLeafNode.getKey(i - 1));
        }

        //将值插入
        targetLeafNode.setKey(offset, key);
        targetLeafNode.setPointer(offset, dataPointer);
    }

    /**
     * 当仍为空树时
     * 对空树进行初始化
     */
    private void insertInEmptyTree(Object key, Object dataPointer) {

        //创建树根节点
        //由于同时是叶节点尾尾指针要接地
        createAndSaveANewRoot(dataPointer, key, BPlusTreeBasicMethods.NULL_POINTER);

        //现在此树根节点也是一个叶节点
        Object rootPointer = bPlusTreeBasicMethods.getRootPointer();

        rootPointer = bPlusTreeBasicMethods.setAsPointerToLeaf(rootPointer);

        //将创建的树根节点指针设置为树根节点指针
        bPlusTreeBasicMethods.setTreeRoot(rootPointer);
    }


    /**
     * 生成并保存一个新的树根
     */
    private void createAndSaveANewRoot(Object fstPointer, Object key, Object secPointer) {

        //创建树根节点的指针
        Object rootPointer = bPlusTreeBasicMethods.getNewBPlusTreeNodePointer();

        //创建树根节点
        BPlusTreeNode rootNode = (BPlusTreeNode) bPlusTreeBasicMethods.getTempBPlusTreeNode();

        //插入键值和指针
        rootNode.setKey(1, key);

        rootNode.setPointer(1, fstPointer);

        rootNode.setPointer(2, secPointer);

        //这时节点中有一个键值两个指针
        rootNode.setSize(2);

        //将创建的树节点存入索引文件
        bPlusTreeBasicMethods.saveTreeNodeChanges(rootPointer, rootNode);

        //将创建的树根节点指针设置为树根节点指针
        bPlusTreeBasicMethods.setTreeRoot(rootPointer);
    }


    @Override
    public void delete(Object key, Object dataPointer) {

        //找到要删除位置相应节点可能出现的开始位置
        Object tempPointer = findTartgetLeafPointerWhenHaveDuplicate(key);

        //找到指定的键值与指针
        while (!tempPointer.equals(IndexFile.NULL_POINTER)) {

            //取出当前叶节点
            BPlusTreeNode tempNode = (BPlusTreeNode) this.bPlusTreeBasicMethods.getNode(tempPointer);

            //依次取出键值对进行比较
            for (int i = 1; i < tempNode.getSize(); ++i) {

                //取出键值对
                Object tempKey = tempNode.getKey(i);

                Object tempDataPointer = tempNode.getPointer(i);

                //如果键已经大于要寻找的键了没找到退出
                if (this.bPlusTreeBasicMethods.compareKeyValue(tempKey, key) == IndexFile.GREATER_THAN) {
                    new Exception("the data pointer you want to delete dose not found.").printStackTrace();
                    return;
                }

                //如果键相等
                if (this.bPlusTreeBasicMethods.compareKeyValue(tempKey, key) == IndexFile.EQUALS) {

                    //进一步检查指针是否相等,若相等进行删除并返回
                    if (tempDataPointer.equals(dataPointer)) {
                        deleteEntry(tempPointer, key, dataPointer);
                        return;
                    }
                }
            }

            //如果本节点没有，那就遍历下一个节点
            tempPointer = tempNode.getPointer(tempNode.getSize());
        }


    }

    /**
     * 返回的是目标数据的指针数组
     *
     * @param fromKey
     * @param toKey   注：在有重复键值时会有问题
     *                需修改
     *                <p>
     *                <p>
     *                <p>
     *                在拥有重复键值时需要小于与等于时都向左走
     *                <p>
     *                同样的搜索方式也会用到删除当中
     */


    @Override
    public Object[] find(Object fromKey, Object toKey) {

        //找到fromkey第一次出现或大于fromkey中最小的key值出现的叶节点
        Object targetLeafPointer = findTartgetLeafPointerWhenHaveDuplicate(fromKey);

        if (targetLeafPointer.equals(BPlusTreeBasicMethods.NULL_POINTER)) {
            return new Vector().toArray();
        }

        //找到该叶节点内出现相应key值的指向相应数据的pointer的位置
        int offset = 0;

        fromTarget:
        while ((targetLeafPointer != null) && (!targetLeafPointer.equals(BPlusTreeBasicMethods.NULL_POINTER))) {

            //得到该指针指向的叶节点
            BPlusTreeNode targetLeafNode = (BPlusTreeNode) this.bPlusTreeBasicMethods.getNode(targetLeafPointer);

            //遍历叶节点中的所有key值
            for (int i = 1; i < targetLeafNode.getSize(); ++i) {

                //取出相应位置的键值
                Object tempKey = targetLeafNode.getKey(i);

                //在该键值大于或等于fromKey值时说明找到了
                //跳出循环
                switch (bPlusTreeBasicMethods.compareKeyValue(tempKey, fromKey)) {
                    case BPlusTreeBasicMethods.GREATER_THAN:
                    case BPlusTreeBasicMethods.EQUALS:
                        offset = i;
                        break fromTarget;
                    default:
                        break;
                }
            }

            //如果在本叶节点中没有找到相应的pointer那么我们继续搜索该叶节点的后继节点
            targetLeafPointer = targetLeafNode.getPointer(targetLeafNode.getSize());

        }

        //如果遍历了所有可能的叶节点也没找到则不可能存在，方法返回空值
        if (targetLeafPointer == null || targetLeafPointer.equals(BPlusTreeBasicMethods.NULL_POINTER)) {
            return null;
        }

        //如果已经确定了范围检索的结果开始，接下来遍历后面的叶节点直到检索范围结束

        //存储所有符合检索范围的数据指针
        Vector vector = new Vector();

        //用于遍历的临时指针
        Object tempPointer = targetLeafPointer;

        toTarget:
        while ((tempPointer != null) && (!tempPointer.equals(BPlusTreeBasicMethods.NULL_POINTER))) {

            BPlusTreeNode tempNode = (BPlusTreeNode) bPlusTreeBasicMethods.getNode(tempPointer);

            //对叶节点进行遍历用的迭代值
            int i;

            //如果是起始叶节点的遍历从偏移量开始
            //否则，从头开始
            if (tempPointer.equals(targetLeafPointer)) {
                i = offset;
            } else {
                i = 1;
            }

            //对单个叶节点进行遍历
            for (; i < tempNode.getSize(); ++i) {

                //如果key的值已经大于检索范围了，跳出遍历
                if (bPlusTreeBasicMethods.compareKeyValue(tempNode.getKey(i), toKey) == BPlusTreeBasicMethods.GREATER_THAN) {
                    break toTarget;
                }

                vector.add(tempNode.getPointer(i));
            }

            //遍历下一个叶节点
            tempPointer = tempNode.getPointer(tempNode.getSize());
        }

        /*//如果没有符合检索范围的结果返回空
        if (vector.isEmpty()) {
            return null;
        }*/


        return vector.toArray();
    }

    @Override
    public Object[] showAll() {

        //存储结果
        ArrayList results = new ArrayList();

        //取得根节点
        Object tempPointer = this.bPlusTreeBasicMethods.getRootPointer();

        if (tempPointer.equals(BPlusTreeBasicMethods.NULL_POINTER)) {
            return results.toArray();
        }

        //游走到全树的第一个叶节点
        while (!this.bPlusTreeBasicMethods.isLeaf(tempPointer)) {
            tempPointer = ((BPlusTreeNode) this.bPlusTreeBasicMethods.getNode(tempPointer)).getPointer(1);
        }

        //顺着叶节点的链将所有叶节点内的data取出
        while (!tempPointer.equals(this.bPlusTreeBasicMethods.NULL_POINTER)) {
            BPlusTreeNode tempNode = (BPlusTreeNode) this.bPlusTreeBasicMethods.getNode(tempPointer);

            //取出当前叶节点中的全部数据指针
            for (int i = 1; i < tempNode.getSize(); ++i) {
                results.add(tempNode.getPointer(i));
            }

            //指向下一个叶节点
            tempPointer = tempNode.getPointer(tempNode.getSize());

        }
        return results.toArray();
    }

    /**
     * 返回含有key一样值的叶节点指针
     * 后含有比key小的key值中最大值
     * 的键所在的叶节点指针
     * <p>
     * 注：本方法并不能保证相应key值的存在，
     * 只能大概率保证返回叶节点
     * 如未设置根节点，还可能得到空值或异常退出
     * <p>
     * <p>
     * 注：由于根节点也可能是叶节点
     * 所以在根节点为底层时会有一些麻烦
     * <p>
     * 注：对函数进行了修改
     * 现在本函数在进行搜索时会把搜索路径保存
     * 保存规则为：
     * 先将游历到的节点压入栈中，
     * 再将该节点要游历的下一个指针
     * 在节点中的索引压入栈中(int)
     * (不包含叶节点及其中索引)
     */
    private Object findTargetLeafPointer(Object key) {

        //从根节点开始搜索
        Object tempPointer = bPlusTreeBasicMethods.getRootPointer();

        //建立存储游历路径的变量
        this.travelPath = new Stack();


        //开始对B+树进行搜索，直到叶节点停止
        //如果根节点是最底层，便不会检查其可行性
        while (!bPlusTreeBasicMethods.isLeaf(tempPointer)) {

            //将游历了的节点指针进行保存
            this.travelPath.push(tempPointer);

            //读出当前指针指向的树节点
            BPlusTreeNode tempNode = (BPlusTreeNode) bPlusTreeBasicMethods.getNode(tempPointer);

            //用于遍历树节点并指出下一个节点指针的索引
            int i;

            loop:
            for (i = 1; i < tempNode.getSize(); ++i) {

                Object tempKey = tempNode.getKey(i);

                switch (bPlusTreeBasicMethods.compareKeyValue(tempKey, key)) {

                    //如果键值与目标key值相等
                    //取得其相应的pointer
                    //由于pointer与键值的配合是左开右闭的
                    case BPlusTreeBasicMethods.EQUALS:
                        ++i;
                        break loop;

                    //大于走左边
                    case BPlusTreeBasicMethods.GREATER_THAN:
                        break loop;
                }
            }

            //得到下一层树节点的指针
            tempPointer = tempNode.getPointer(i);

            //将该节点中的下一个指针的索引保存
            this.travelPath.push(i);
        }

        //返回指向叶节点的指针
        return tempPointer;
    }

    /**
     * 当有重复时为了不丢失搜索结果
     * 在比较键值为大于或等于时都向左走
     * <p>
     * 注：这里搜索会记录搜索走到了树的第几层
     */
    private Object findTartgetLeafPointerWhenHaveDuplicate(Object key) {

        //从根节点开始搜索
        Object tempPointer = bPlusTreeBasicMethods.getRootPointer();

        if (tempPointer.equals(BPlusTreeBasicMethods.NULL_POINTER)) {
            return BPlusTreeBasicMethods.NULL_POINTER;
        }

        //记录遍历到了第几层
        this.currentHeight = 0;


        //开始对B+树进行搜索，直到叶节点停止
        //如果根节点是最底层，便不会检查其可行性
        while (!bPlusTreeBasicMethods.isLeaf(tempPointer)) {

            //读出当前指针指向的树节点
            BPlusTreeNode tempNode = (BPlusTreeNode) bPlusTreeBasicMethods.getNode(tempPointer);

            //用于遍历树节点并指出下一个节点指针的索引
            int i;

            loop:
            for (i = 1; i < tempNode.getSize(); ++i) {

                Object tempKey = tempNode.getKey(i);

                switch (bPlusTreeBasicMethods.compareKeyValue(tempKey, key)) {

                    //大于与等于都要走左边
                    case BPlusTreeBasicMethods.EQUALS:


                    case BPlusTreeBasicMethods.GREATER_THAN:
                        break loop;
                }
            }

            //得到下一层树节点的指针
            tempPointer = tempNode.getPointer(i);

            //层数加一
            ++this.currentHeight;
        }

        //返回指向叶节点的指针
        return tempPointer;

    }


    /**
     * 获得该树的索引名
     */
    @Override
    public String getAttributeName() {
        return this.bPlusTreeBasicMethods.getAttributeName();
    }

    /**
     * for testing
     */
    public static int randFourNumbers() {
        double r = Math.random();

        for (int i = 1; ; ++i) {
            double p = Math.pow(10, i);
            if (((int) (r * p)) / 1000 != 0) {
                return ((int) (r * p)) % 10000;
            }
        }
    }


    /**
     * 通过树在相应键值的遍历
     * 得到拥有指定键值和指针的
     * 树节点的父亲节点
     */

    private Object getParent(Object targetKey, Object targetPointer, int targetHeight) {
        //当前指针指向节点
        Object currentPointer = this.bPlusTreeBasicMethods.getRootPointer();

        //当前访问层数
        int currentHeight = 0;

        //存储要遍历的节点
        Deque nowLayer = new LinkedBlockingDeque();

        Deque nextLayer = new LinkedBlockingDeque();

        //先将指针怼入队头
        nowLayer.addLast(currentPointer);


        //进行遍历
        while (!nowLayer.isEmpty()) {
            //从队中取出一个节点指针
            currentPointer = nowLayer.pollFirst();

            //取出其指向的节点
            BPlusTreeNode currentNode = (BPlusTreeNode) this.bPlusTreeBasicMethods.getNode(currentPointer);

            //如果已经到了父节点那层,看是否含有儿子，那么返回
            //否则层次遍历到下一层的所有节点指针
            if (currentHeight == (targetHeight - 1)) {
                for (int i = 1; i <= currentNode.getSize(); ++i) {
                    if (targetPointer.equals(currentNode.getPointer(i))) {
                        return currentPointer;
                    }
                }
            } else if (currentHeight < (targetHeight - 1)) {
                loop:
                for (int i = 1; i < currentNode.getSize(); ++i) {
                    switch (this.bPlusTreeBasicMethods.compareKeyValue(currentNode.getKey(i), targetKey)) {
                        //大于和等于时都应向左走，在大于时就可以退出遍历了
                        case BPlusTreeBasicMethods.GREATER_THAN:
                            nextLayer.addLast(currentNode.getPointer(i));
                            break loop;
                        case BPlusTreeBasicMethods.EQUALS:
                            nextLayer.addLast(currentNode.getPointer(i));
                            if (i == currentNode.getSize() - 1) {
                                nextLayer.addLast(currentNode.getPointer(i + 1));
                            }
                            break;
                        //如果比最大的键值都大则有一定可能在最后一个指针，但也有大概率在别的节点，所以要把所有可能节点遍历
                        case BPlusTreeBasicMethods.LESSER_THAN:
                            if (i == currentNode.getSize() - 1) {
                                nextLayer.addLast(currentNode.getPointer(i + 1));
                                break loop;
                            }
                            break;
                        default:
                            break;
                    }
                }
            } else {
                //如果超过目标层数没找到
                new Exception("didn't find targets' parent").printStackTrace();
            }
            //如果当前层已排空交换队列并层数加一
            if (nowLayer.isEmpty()) {
                Deque temp = nowLayer;
                nowLayer = nextLayer;
                nextLayer = temp;
                ++currentHeight;
            }
        }

        new Exception("Cannot find valid parent " + targetKey.toString() + "  " + targetPointer.toString()).printStackTrace();

        return null;
    }

    /**
     * 将指定节点的键值和指针删除并
     * 对相应的节点变化进行操作
     */
    private void deleteEntry(Object pointer, Object keyToBeDeleted, Object pointerToBeDeleted) {



        //取出相应的节点
        BPlusTreeNode treeNode = (BPlusTreeNode) this.bPlusTreeBasicMethods.getNode(pointer);

        //记录其检索键值
        Object keyForSearch = treeNode.getKey(1);

        //将相应的key和pointer删除
        deleteElementInNode(treeNode, keyToBeDeleted, pointerToBeDeleted);

        //如果是根节点
        if (pointer.equals(this.bPlusTreeBasicMethods.getRootPointer())) {

            //如果只有一个儿子了
            if (treeNode.getSize() == 1) {

                //如果还是一个叶节点，说明所有的数据都被删除了将根节点置为空
                if (this.bPlusTreeBasicMethods.isLeaf(pointer)) {
                    this.bPlusTreeBasicMethods.setTreeRoot(IndexFile.NULL_POINTER);
                } else {

                    //如果根节点不是叶节点，使唯一儿子成为根节点
                    this.bPlusTreeBasicMethods.setTreeRoot(treeNode.getPointer(1));
                }

            } else {
                //不是只有一个儿子
                //那么只需保存刚才的修改
                this.bPlusTreeBasicMethods.saveTreeNodeChanges(pointer, treeNode);
            }
            return;
        }


        //计算N/2的天棚
        int roof = (treeNode.getN() / 2) + (treeNode.getN() % 2);

        if (treeNode.getSize() >= roof) {
            this.bPlusTreeBasicMethods.saveTreeNodeChanges(pointer, treeNode);
            return;
        }

        /*if (keyForSearch.equals("16812103855")) {
            System.out.println("start error place");
            Object[] p = find("16812103855" , "16812103855");
            if (p == null || p.length == 0) {
                System.out.println("Cannot Find");
            }
        }*/


        //如果不是根节点寻找指向他父节点的指针
        Object parentPointer = getParent(keyForSearch, pointer, this.currentHeight);

        //取出父节点
        BPlusTreeNode parentNode = (BPlusTreeNode) this.bPlusTreeBasicMethods.getNode(parentPointer);

        /*//调试用，如果父节点只有一个指针，意外情况
        if (parentNode.getSize() == 1) {
            new Exception("only one node in the parent node.").printStackTrace();

            Object[] p = find("80916797689", "80916797689");

            if (p == null || p.length == 0) {
                System.out.println("fuck");
            }

            int b = howManyOne();

            System.out.println("now there is " + b);
        }*/

        //得到一个兄弟节点的指针
        Object brotherPointer = null;

        //得到兄弟指针和当前指针之间的键
        Object keyBetween = null;

        //记录得到的兄弟节点是否是当前节点的前一个节点
        boolean isBrotherPredecessor = false;

        //记录兄弟节点之间键值的位置
        int offset = 0;

        if (pointer.equals(parentNode.getPointer(1))) {
            //如果是第一个儿子取下一个儿子
            brotherPointer = parentNode.getPointer(2);
            keyBetween = parentNode.getKey(1);
            offset = 1;
            isBrotherPredecessor = false;
        } else {
            //如果是最后一个儿子取前一个儿子
            //不是前两种特殊情况,就取前一个好了
            for (int i = 2; i <= parentNode.getSize(); ++i) {
                if (pointer.equals(parentNode.getPointer(i))) {
                    brotherPointer = parentNode.getPointer(i - 1);
                    keyBetween = parentNode.getKey(i - 1);
                    offset = i - 1;
                    isBrotherPredecessor = true;
                    break;
                }
            }
        }

        //调试用，如果没找到兄弟节点出问题了
        if (keyBetween == null || brotherPointer == null) {
            new Exception("didn't found a brother pointer.").printStackTrace();
        }

        //取出兄弟节点
        BPlusTreeNode brotherNode = (BPlusTreeNode) this.bPlusTreeBasicMethods.getNode(brotherPointer);

        //将两个兄弟节点合并会有几个pointer
        int totalPointers = brotherNode.getSize() + treeNode.getSize();

        //当前的节点是否为叶节点
        boolean isLeaf = this.bPlusTreeBasicMethods.isLeaf(pointer);

        //如果这两个节点的大小足够合并
        if (((!isLeaf) && (totalPointers <= treeNode.getN())) || (isLeaf && ((totalPointers - 1) <= treeNode.getN()))) {
            //如果当前节点是其兄弟节点的前驱节点,兄弟互换
            if (!isBrotherPredecessor) {
                Object temp = pointer;
                pointer = brotherPointer;
                brotherPointer = temp;
                temp = treeNode;
                treeNode = brotherNode;
                brotherNode = (BPlusTreeNode) temp;
            }

            //如果当前节点不是叶节点,将两个节点之间父节点的键值插入并合并
            //如果是叶节点就直接将键值对依次移入并将链接缩小
            if (!isLeaf) {

                //将父节点中夹在两个节点之间的键插入
                insertInNodeWithoutSplit(brotherNode, keyBetween, treeNode.getPointer(1));

                //将当前节点追加到其兄弟节点
                for (int i = 1; i < treeNode.getSize(); ++i) {
                    insertInNodeWithoutSplit(brotherNode, treeNode.getKey(i), treeNode.getPointer(i + 1));
                }

            } else {

                //将键值对追加到兄弟
                for (int i = 1; i < treeNode.getSize(); ++i) {
                    insertInLeafWithoutSplit(brotherNode, treeNode.getKey(i), treeNode.getPointer(i));
                }

                //如果当前节点指向兄弟节点则无需改变兄弟尾指针,否则将兄弟节点尾指针改为当前节点尾指针
                if (!brotherPointer.equals(treeNode.getPointer(treeNode.getSize()))) {
                    brotherNode.setPointer(brotherNode.getSize(), treeNode.getPointer(treeNode.getSize()));
                }

            }

            //保存兄弟节点的修改
            this.bPlusTreeBasicMethods.saveTreeNodeChanges(brotherPointer, brotherNode);

            //下一次调用的层数减少
            --this.currentHeight;

//            System.out.println("in 1031 " + totalPointers);

            /*//ceshi
            howManyOne();*/

            //将中间键删除的递归调用
            deleteEntry(parentPointer, keyBetween, pointer);

            //返回
            return;
        }

        //如果两个节点大小不足以合并，则从兄弟节点借一个key和pointer

//        System.out.println(" in 1039 " + totalPointers);

        //如果兄弟是当前节点的前驱
        if (isBrotherPredecessor) {

            //如果不是叶节点
            if (!isLeaf) {

                //新的父节点键
                Object newKeyBetween = brotherNode.getKey(brotherNode.getSize() - 1);

                //新的当前节点头指针
                Object newPointerForTreeNode = brotherNode.getPointer(brotherNode.getSize());

                //缩小兄弟节点
                brotherNode.setSize(brotherNode.getSize() - 1);

                //扩大节点大小
                treeNode.setSize(treeNode.getSize() + 1);

                //挪动当前的最后一个指针
                treeNode.setPointer(treeNode.getSize(), treeNode.getPointer(treeNode.getSize() - 1));

                //插入借用的键值对都是第一个键和第一个指针
                for (int i = treeNode.getSize() - 1; i > 1; --i) {
                    treeNode.setKey(i, treeNode.getKey(i - 1));
                    treeNode.setPointer(i, treeNode.getPointer(i - 1));
                }

                treeNode.setKey(1, keyBetween);
                treeNode.setPointer(1, newPointerForTreeNode);

                //将父节点里兄弟间的键更新为新得到的键
                parentNode.setKey(offset, newKeyBetween);

            } else {
                //如果是叶节点

                //得到要转移的键和指针
                Object dataPointer = brotherNode.getPointer(brotherNode.getSize() - 1);

                Object newKeybetween = brotherNode.getKey(brotherNode.getSize() - 1);

                //缩小兄弟节点
                deleteElementInNode(brotherNode, newKeybetween, dataPointer);

                //将键和指针转移到当前节点
                insertInLeafWithoutSplit(treeNode, newKeybetween, dataPointer);

                //修改父节点夹着的键值
                parentNode.setKey(offset, newKeybetween);

            }
        } else {
            //如果不是前驱，做对称的操作（？）

            //如果不是叶节点
            if (!isLeaf) {


                //新的父节点键
                Object newKeyBetween = brotherNode.getKey(1);

                //新的当前节点尾指针
                Object newPointerForTreeNode = brotherNode.getPointer(1);

                //重整兄弟节点
                deleteElementInNode(brotherNode, newKeyBetween, newPointerForTreeNode);

                //将父节点夹着的键和兄弟节点取出的指针插入当前节点****************************************
                insertInNodeWithoutSplit(treeNode, keyBetween, newPointerForTreeNode);

                //更新父节点夹着的键
                parentNode.setKey(offset, newKeyBetween);

            } else {
                //如果是叶节点

                //新的父节点键
                Object newKeyBetween = brotherNode.getKey(1);

                //转移的数据指针
                Object dataPointer = brotherNode.getPointer(1);

                //插入当前节点
                insertInLeafWithoutSplit(treeNode,newKeyBetween,dataPointer);

                //重整兄弟节点
                deleteElementInNode(brotherNode, newKeyBetween, dataPointer);

                //改父节点值
                parentNode.setKey(offset, brotherNode.getKey(1));
            }


        }

        //保存当前和兄弟和父节点的修改
        this.bPlusTreeBasicMethods.saveTreeNodeChanges(parentPointer, parentNode);
        this.bPlusTreeBasicMethods.saveTreeNodeChanges(brotherPointer, brotherNode);
        this.bPlusTreeBasicMethods.saveTreeNodeChanges(pointer,treeNode);

        /*//ceshi
        howManyOne();*/

    }

    /**
     * 将相应的key和pointer删除
     */
    private void deleteElementInNode(BPlusTreeNode treeNode, Object keyToBeDeleted, Object pointerToBeDeleted) {

        boolean isKeyDeleted = false;
        boolean isPointerDeleted = false;
        //删键
        for (int i = 1; i < treeNode.getSize(); ++i) {

            //如果找到了相应的键将其删除，并跳出，防止相同的键全部被删除
            if (this.bPlusTreeBasicMethods.compareKeyValue(keyToBeDeleted, treeNode.getKey(i)) == IndexFile.EQUALS) {
                for (int j = i + 1; j < treeNode.getSize(); ++j) {
                    treeNode.setKey(j - 1, treeNode.getKey(j));
                }
                isKeyDeleted = true;
                break;
            }
        }

        //删指针
        for (int i = 1; i <= treeNode.getSize(); ++i) {

            //找到了删掉并跳出
            if (treeNode.getPointer(i).equals(pointerToBeDeleted)) {
                for (int j = i + 1; j <= treeNode.getSize(); ++j) {
                    treeNode.setPointer(j - 1, treeNode.getPointer(j));
                }
                isPointerDeleted = true;
                break;
            }

        }

        //将节点缩小
        treeNode.setSize(treeNode.getSize() - 1);

        if (!(isKeyDeleted && isPointerDeleted)) {
            new Exception("Delete in Node failed.").printStackTrace();
        }
    }

    private int howManyOne() {
        int count = 0;
        Stack s = new Stack();
        s.push(this.bPlusTreeBasicMethods.getRootPointer());
        while (!s.empty()) {
            Object currentPointer = s.pop();
            BPlusTreeNode treeNode = (BPlusTreeNode) this.bPlusTreeBasicMethods.getNode(currentPointer);
            if ((!currentPointer.equals(this.bPlusTreeBasicMethods.getRootPointer())) && treeNode.getSize() == 1) {
                ++count;
                new Exception().printStackTrace();
                System.out.println("bad node");
            }
            for (int i = 1; i <= treeNode.getSize(); ++i) {
                Object p = treeNode.getPointer(i);
                if (!this.bPlusTreeBasicMethods.isLeaf(p)) {
                    s.push(p);
                }
            }
        }
        return count;
    }

    /*public static void main(String[] args) throws IOException {
        ShabbyDB db = new ShabbyDB();
        db.doFile("test");
        IndexFile indexFile = new IndexFile();
        indexFile.setIndexFile("student_info", "student_id");
        BPlusTree tree = new BPlusTree();
        tree.setBPlusTree(indexFile);
        final int limit = 10000;
        tree.insert("20160109249", new Long(2236));
        System.out.println(1);
        tree.insert("93350204921", new Long(5638));
        System.out.println(2);
        tree.insert("94350204921", new Long(5556));
        System.out.println(3);
        tree.insert("11510206295", new Long(786));
        System.out.println(4);
        tree.insert("35360206905", new Long(3786));
        System.out.println(5);
        tree.insert("74430208293", new Long(7186));
        System.out.println(6);
        tree.insert("85370206639", new Long(4228));
        System.out.println(7);
        tree.insert("63460201922", new Long(5185));
        System.out.println(8);
        tree.insert("10830208139", new Long(9668));
        System.out.println(9);
        for (int i = 10; i <= limit; ++i) {


            String randPrefix = new Integer(randFourNumbers()).toString();
            String randTail = new Integer(randFourNumbers()).toString();
            String tk = randPrefix + "020" + randTail;
            tree.insert(tk, ((long) (Math.random() * 10000)));

            System.out.println(i);
        }
        long timeCosume =  System.currentTimeMillis();
        Object[] res = tree.find("20160109249", "35360206905");
        System.out.println(res.length);
        System.out.println("results starts now");
        for (int i = 0 ; i < res.length; ++i) {
            System.out.println(res[i]);
        }
        System.out.println(System.currentTimeMillis() - timeCosume);
    }*/

    /*public static void main(String[] args) {
        IndexFile indexFile = new IndexFile();
        indexFile.setIndexFile("student_info", "student_class");
        BPlusTree tree = new BPlusTree();
        tree.setBPlusTree(indexFile);
        *//*final int limit = 10000;
        tree.insert(new Long(2016L), new Long(1L) );
        for (int i = 2; i <= limit; ++i) {

            tree.insert(new Long((long) (Math.random() * 10000)), new Long(0L));

            System.out.println(i);
        }*//*
        long timeCosume =  System.currentTimeMillis();
        Object[] res = tree.find(2016L, 2016L);
        System.out.println(res.length);
        System.out.println("results starts now");
        for (int i = 0 ; i < res.length; ++i) {
            System.out.println(res[i]);
        }
        System.out.println(System.currentTimeMillis() - timeCosume);
    }*/
}
