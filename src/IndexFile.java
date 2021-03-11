import java.io.*;
import java.nio.ByteBuffer;


public class IndexFile implements BPlusTreeBasicMethods, DataTypes {


    private static final int FILE_HEADER_SIZE = 256;
    //两个指针和一个树节点头
    private static final int MAX_DATASIZE_LIMIT = BPlusTreeNodeMethods.nodeSize - 8 * 3;
    private static final long ROOT_POINTER_SAVE_POSITION = 12L;
    private static final long NEXT_POINTER_SAVE_POSITION = 20L;
    private static final long LEAF_TEST_BIT = Long.parseUnsignedLong("8000000000000000" , 16);
    //属性成员变量
    private IndexFileInfo indexFileInfo;
    //用于文件读写的成员对象
    private File file;

    //实验：专用的写文件成员变量使用写同步模式（“rws”）保证读取信息的正确
    private RandomAccessFile accessFile;


    /**
     * 用于生成索引文件的静态方法
     */
    public static String createIndexFile(IndexInfo indexInfo) throws IOException {

        //生成返回结果用的对象
        StringBuffer result = new StringBuffer();

        result.append(indexInfo.attributeName + " ");


        //创建相应的空索引文件
        File file = new File("indexes/" + indexInfo.tableName + "_" + indexInfo.attributeName);


        file.createNewFile();


        //按字节输入文件头的数据
        FileOutputStream fileOutputStream = null;

        try {
            fileOutputStream = new FileOutputStream(file, false);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        //文件头一共256B
        //使用内存数组节点流写入
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(FILE_HEADER_SIZE);
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);


        //写入键值类型
        dataOutputStream.writeInt(indexInfo.typeAndLength.get(TableFile.TYPE));

        //写入键值数据长度
        int keySize = indexInfo.typeAndLength.get(TableFile.LENGTH);
        dataOutputStream.writeInt(keySize);


        //计算单个树节点的N值
        if (keySize > MAX_DATASIZE_LIMIT) {
            //抱歉还无法支持较长的字符串直接杀死程序
            System.err.println("Too long key.");
            System.exit(0);
        }

        //加入了树节点头所以减了pointersize*2
        int N = (BPlusTreeNodeMethods.nodeSize - BPlusTreeNodeMethods.pointerSize * 2) / (keySize + BPlusTreeNodeMethods.pointerSize) + 1;


        //写入N值
        dataOutputStream.writeInt(N);

        //写入初始的根节点位置(这里我们使用long做虚拟指针,用0代表空指针)
        //使用ByteBuffer将long转化为byte[]
        dataOutputStream.writeLong(NULL_POINTER);

        //写入下一个可插入的位置(使用虚拟指针)
        dataOutputStream.writeLong(1L);


        //将其转为byte数组
        byte[] bytes = byteArrayOutputStream.toByteArray();


        //写入索引文件
        fileOutputStream.write(bytes);

        //关闭索引文件
        fileOutputStream.close();

        //成功完成表创建返回
        result.append(" created successfully\n");
        return result.toString();

    }

    /**
     * 设置需要进行读取和修改的索引文件
     */
    public boolean setIndexFile(String tableName, String attributeName) {

        //如果输入不合法直接抛异常
        if (attributeName == null || tableName == null) {
            throw new NullPointerException();
        }


        //建立一个新实例
        indexFileInfo = new IndexFileInfo();

        //设置表名
        indexFileInfo.indexInfo.tableName = tableName;

        //设置属性名
        indexFileInfo.indexInfo.attributeName = attributeName;

        //对相应索引文件的打开
        file = new File("indexes/" + tableName + "_" + attributeName);

        //如果无法打开文件直接返回
        if (!file.isFile()) {
            System.out.println("The file is invalid.");
            return false;
        }

        //如果可以打开则用RandomAccessFile将其文件头读入
        try {

            //如果之前打开过某个索引文件则关闭当前打开的文件
            if (accessFile != null) {
                accessFile.close();
            }

            //创建只读的随机访问
            accessFile = new RandomAccessFile(file, "rws");
            //用于保存文件头的数据
            byte[] bytes = new byte[IndexFile.FILE_HEADER_SIZE];
            //读取文件头的数据
            accessFile.read(bytes);
            /*//文件头读取结束，关闭文件
            accessFile.close();*/

            //将文件头中的数据读取出来
            ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);

            //将文件头中存储的数据读出
            int keyType = byteBuffer.getInt();
            int keyLength = byteBuffer.getInt();
            int N = byteBuffer.getInt();
            long rootPointer = byteBuffer.getLong();
            long nextPointer = byteBuffer.getLong();


            //放入对象中用于存储索引信息的成员变量
            indexFileInfo.indexInfo.typeAndLength.add(TableFile.TYPE, keyType);
            indexFileInfo.indexInfo.typeAndLength.add(TableFile.LENGTH, keyLength);
            indexFileInfo.N = N;
            indexFileInfo.rootPointer = rootPointer;
            indexFileInfo.nextPointer = nextPointer;

            //设置成功，返回
            return true;


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        //触底失败
        return false;

    }

    /**
     * 得到属性名
     */
    public IndexFileInfo getIndexFileInfo() {
        return this.indexFileInfo;
    }


    /**
     * 这些方法返回指向树节点的指针
     * 我们对树节点的读取都需要通过指针
     */
    @Override
    public Object getNewBPlusTreeNodePointer() {

        //得到下一个可用来存储新生成树节点的指针，并将存储值更新
        long result = this.indexFileInfo.nextPointer++;

        try {

            /*//同时更新索引文件头
            RandomAccessFile accessFile = new RandomAccessFile(file , "rwd");*/

            //转到存储下一个指针的文件头位置
            accessFile.seek(NEXT_POINTER_SAVE_POSITION);

            //将更新值写入
            accessFile.writeLong(this.indexFileInfo.nextPointer);

            /*//关闭文件
            accessFile.close();*/

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    @Override
    public Object getRootPointer() {

        //存放读取出的结果
        long result = NULL_POINTER;

        result = this.indexFileInfo.rootPointer;
/*
        try {

            //打开文件
            RandomAccessFile accessFile = new RandomAccessFile(file , "r");

            //寻找到指定位置
            accessFile.seek(ROOT_POINTER_SAVE_POSITION);

            //读取指向根节点的指针
            result = accessFile.readLong();

            //关闭文件
            accessFile.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        return result;
    }

    /**
     * 判断一个指针是否指向叶节点
     *
     * @param bPlusTreeNodePointer
     */
    @Override
    public boolean isLeaf(Object bPlusTreeNodePointer) {

        return ((long)bPlusTreeNodePointer & LEAF_TEST_BIT) != 0;
    }

    /**
     * 将一个指针设置成指向叶节点的指针
     *
     * @param bPlusTreeNodePointer
     */
    @Override
    public Object setAsPointerToLeaf(Object bPlusTreeNodePointer) {

        return ((long) bPlusTreeNodePointer) | LEAF_TEST_BIT;
    }

    /**
     * 通过给定的树节点指针读取出树节点
     * 再通过树节点类访问和修改其中的内容
     *
     * @param bPlusTreeNodePointer
     */
    @Override
    public Object getNode(Object bPlusTreeNodePointer) {

        //用于存放得到的节点结果
        BPlusTreeNode result = null;

        long position = 0L;

        try {
            //得到树节点在文件中的真实位置
            position = (long) bPlusTreeNodePointer;
        } catch (Exception e) {
            e.printStackTrace();
        }
        position <<= 8;

        //如果为无效指针，报错并返回空指针
        if (position == NULL_POINTER) {
            System.err.println("Invalid pointer!");
        } else {


            try {

                /*//以随机访问方式打开索引文件
                RandomAccessFile accessFile = new RandomAccessFile(file , "r");*/

                //移动读取指针到指定位置
                accessFile.seek(position);

                //将指定大小的节点读出并构造成树节点对象
                byte[] bytes = new byte[FILE_HEADER_SIZE];

                accessFile.read(bytes);

                result = new BPlusTreeNode(bytes);
/*
                accessFile.close();*/

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }



        return result;
    }

    /**
     * 给出一个临时的树节点对象用于方便之后树节点分裂
     */
    @Override
    public Object getTempBPlusTreeNode() {

        //申请一个与树节点一样大的字节数值用于初始化
        byte[] bytes = new byte[FILE_HEADER_SIZE];

        //开始初始化一个树节点头
        long header = 0L;

        //存入N值
        header |= (long) this.indexFileInfo.N;

        header <<= 8;

        //存入键值占用空间
        header |= (long) this.indexFileInfo.indexInfo.typeAndLength.get(TableFile.LENGTH);

        header <<= 2;

        //存入键值类型
        header |= this.indexFileInfo.indexInfo.typeAndLength.get(TableFile.TYPE);

        //由于一开始初始化时树节点中没有任何值所以size不用赋值
        header <<= 5;

        //将初始化好的节点头放入
        ByteBuffer.wrap(bytes).putLong(header);

        //返回构造好的空树节点
        return new BPlusTreeNode(bytes);
    }

    /**
     * 通过比较键值进行目标的搜索
     *
     * @param key
     */
    @Override
    public Object getKeyValue(Object key) {
        return key;
    }

    @Override
    public int compareKeyValue(Object keyValue1, Object keyValue2) {

        //定义一种比较失败的返回值
        final int COMPARE_ERROR = 100;

        //存储比较结果
        int result = COMPARE_ERROR;

        try {
            //根据数据类型进行比较
            switch (this.indexFileInfo.indexInfo.typeAndLength.get(TableFile.TYPE)) {
                case DataTypes.INT :
                    result = ((Long)keyValue1).compareTo((Long) keyValue2);
                    break;
                case DataTypes.FLOAT :
                    result = Double.compare(((Number)keyValue1).doubleValue() , ((Number)keyValue2).doubleValue());
                    break;
                case DataTypes.CHAR :
                    result = (keyValue1.toString()).compareTo(keyValue2.toString());
                    break;
                default :
                    System.err.println("Unrecognized type!");
                    return COMPARE_ERROR;
            }
        } catch (Exception e) {

            e.printStackTrace();
        }

        //返回指定的比较结果
        if (result == 0) {
            return IndexFile.EQUALS;
        } else if (result > 0) {
            return IndexFile.GREATER_THAN;
        } else if (result < 0) {
            return IndexFile.LESSER_THAN;
        }

        return COMPARE_ERROR;
    }

    /**
     * 保存你所改变的树节点
     * 这里需要你要保存的位置和你要保存的内容
     *
     * @param bPlusTreeNodePointer
     * @param bPlusTreeNode
     */
    @Override
    public void saveTreeNodeChanges(Object bPlusTreeNodePointer, Object bPlusTreeNode) {

        //得到在文件中存放的真实位置
        long position = (long) bPlusTreeNodePointer << 8;

        //如果是无效指针，直接返回
        if (position == NULL_POINTER) {
            return;
        } else {

            try {

                /*//打开文件
                RandomAccessFile accessFile = new RandomAccessFile(file , "rwd");*/

                //移动到保存位置
                accessFile.seek(position);

                //保存数据
                accessFile.write(((BPlusTreeNode) bPlusTreeNode).getBytes());

                /*//关闭文件
                accessFile.close();*/

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * 在树结构发生变动后当根节点发生改变时要改变根节点的指针
     *
     * @param bPlusTreeNodePointer
     */
    @Override
    public void setTreeRoot(Object bPlusTreeNodePointer) {

        long newRoot = (long) bPlusTreeNodePointer;

        //改写成员变量
        this.indexFileInfo.rootPointer = newRoot;

        try {

            /*//用随机访问方式打开指定文件
            RandomAccessFile accessFile = new RandomAccessFile(this.file , "rwd");*/

            //移动到存储根节点指针的位置
            accessFile.seek(IndexFile.ROOT_POINTER_SAVE_POSITION);

            //改写指针
            accessFile.writeLong((Long) bPlusTreeNodePointer);

            /*//关闭文件保存修改
            accessFile.close();*/

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 得到该树的索引名
     */
    @Override
    public String getAttributeName() {
        return this.indexFileInfo.indexInfo.attributeName;
    }
}
