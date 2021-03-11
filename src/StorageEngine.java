import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * 进行存取方面的相关处理
 */
public class StorageEngine {

    protected TableFile tableFile;

    protected LinkedHashMap<String, BPlusTree> stringBPlusTreeHashMap;

    protected DataFile dataFile;


    public StorageEngine() {

        this.tableFile = new TableFile();
        this.stringBPlusTreeHashMap = new LinkedHashMap<>();
        this.dataFile = new DataFile();
    }

    /**
     * 设置存储管理现在要进行操作的表
     */
    protected void setTable(String tableName) {

        //检查现在的存在的tableFile是否满足要求
        // ,满足则无需后续操作
        if (tableName.equals(tableFile.getTableName())) {
            return;
        }

        //设置表文件
        tableFile.setTable(tableName);

        //设置数据存储文件
        this.dataFile.setDataFile(tableName);

        //清空前一个表对应的索引树
        stringBPlusTreeHashMap.clear();

        //将新设置的表中的全部属性加入到hash表中

        //得到表中所有的属性名
        String[] attributeNames = tableFile.getAttributeNames();

        //对每个属性名进行相应B+树的初始化
        for (int i = 0; i < attributeNames.length; ++i) {

            //创建相应属性的索引文件操作对象
            IndexFile indexFile = new IndexFile();
            indexFile.setIndexFile(tableName, attributeNames[i]);

            //根据索引文件操作对象创建B+树
            BPlusTree bPlusTree = new BPlusTree();
            bPlusTree.setBPlusTree(indexFile);

            //将该属性对应的B+树加入hash表中
            stringBPlusTreeHashMap.put(attributeNames[i], bPlusTree);
        }
    }

    /**
     * 进行一条数据的插入指令操作
     */
    public String insert(String insertCommond) {


        //创建返回结果
        StringBuffer result = new StringBuffer("");

        //调用翻译器得到插入所需要的信息
        InsertInfo insertInfo = CommandInterpreter.insertInfoInterpret(insertCommond);


        //调整到对应的操作指令需要的表,并将表中所有属性对应的索引树加载
        this.setTable(insertInfo.tableName);

        //将得到的数据根据表信息进行插入前的处理
        Object[] values = insertInfo.attributeValues;
        Collection<ArrayList<Integer>> attributes = this.tableFile.getAttributes().values();
        int i = 0;
        for (Iterator<ArrayList<Integer>> it = attributes.iterator(); it.hasNext(); ++i) {
            switch (it.next().get(TableFile.TYPE)) {
                case DataTypes.INT:
                    values[i] = ((Number) values[i]).longValue();
                    break;
                case DataTypes.FLOAT:
                    values[i] = ((Number) values[i]).doubleValue();
                    break;
                case DataTypes.CHAR:
                    values[i] = values[i].toString();
                    break;
                default:
                    new Exception("Don't have this type.").printStackTrace();
                    break;
            }
        }

        //将数据插入文件并得到其指针,用于接下来的索引文件的更新
        Long dataPointer = (Long) this.doDataInsert(insertInfo);

        //对表中的所有索引文件进行更新

        Collection<BPlusTree> trees = this.stringBPlusTreeHashMap.values();

        i = 0;

        for (Iterator<BPlusTree> it = trees.iterator(); it.hasNext(); ++i) {

            BPlusTree tempTree = it.next();

            tempTree.insert(values[i], dataPointer);
        }

        return result.toString();
    }

    public ShabbyResultSet search(String searchCommond) {

        //存储搜索后得到的结果
        ShabbyResultSet resultSet = null;

        //对命令进行翻译
        SearchInfo searchInfo = CommandInterpreter.searchInfoInterpret(searchCommond);

        //对当前要操作的表进行设置
        this.setTable(searchInfo.tableName);

        //对给定的属性键值进行检索相应类型的检索

        Object[] valuePointers = null;

        switch (searchInfo.whichSearch) {
            case SearchInfo.SHOW_ALL:
                valuePointers = this.stringBPlusTreeHashMap.values().iterator().next().showAll();
                break;
            case SearchInfo.NORMAL_SEARCH:
            case SearchInfo.RANGE_SEARCH:
                valuePointers = this.stringBPlusTreeHashMap.get(searchInfo.attributeName).find(searchInfo.from, searchInfo.to);
                break;

        }


        //将实际的内容从DataFile中读出

        //用于存储一条数据
        byte[][] dataRows = new byte[valuePointers.length][];

        for (int i = 0; i < valuePointers.length; ++i) {

            //从datafile中取出
            dataRows[i] = this.dataFile.getData(valuePointers[i]);
        }

        //将关系表中的偏移量和长度信息按次序取出用于初始化ShabbyResultSet

        //用于存放取出的信息
        int[] offset = new int[this.tableFile.getAttributes().size()];
        int[] length = new int[this.tableFile.getAttributes().size()];

        //每次存放信息的位置
        int count = 0;

        //取出存有信息的映射
        LinkedHashMap<String, ArrayList<Integer>> attributes = this.tableFile.getAttributes();


        //对映射的键值进行遍历取出每个属性的信息数组
        for (String attributeName : attributes.keySet()) {

            //对应属性的信息
            ArrayList<Integer> attribute = attributes.get(attributeName);

            //将偏移量和长度取出
            offset[count] = attribute.get(TableFile.OFFSET);
            length[count] = attribute.get(TableFile.LENGTH);

            //准备放置下一组信息
            ++count;
        }

        resultSet = new ShabbyResultSet(dataRows, offset, length);

        return resultSet;
    }

    public ShabbyResultSet delete(String deleteCommond) {

        //翻译删除信息，现在基本就是一个劣化的搜索
        DeleteInfo deleteInfo = CommandInterpreter.deleteInfoInterpret(deleteCommond);

        //设置为要操作的关系
        this.setTable(deleteInfo.tableName);

        //对要删除的信息进行检索
        Object[] dataPointers = this.stringBPlusTreeHashMap.get(deleteInfo.attributeName).find(deleteInfo.from, deleteInfo.to);

        //如果没有搜索到想要的结果
        if (dataPointers == null || dataPointers.length == 0) {
            return null;
        }

        //存储数据结果
        byte[][] rows = new byte[dataPointers.length][];

        for (int i = 0; i < dataPointers.length; ++i) {
            rows[i] = dataFile.getData(dataPointers[i]);
        }

        //用于存放取出的信息
        int[] offset = new int[this.tableFile.getAttributes().size()];
        int[] length = new int[this.tableFile.getAttributes().size()];

        //每次存放信息的位置
        int count = 0;

        //取出存有信息的映射
        LinkedHashMap<String, ArrayList<Integer>> attributes = this.tableFile.getAttributes();


        //对映射的键值进行遍历取出每个属性的信息数组
        for (String attributeName : attributes.keySet()) {

            //对应属性的信息
            ArrayList<Integer> attribute = attributes.get(attributeName);

            //将偏移量和长度取出
            offset[count] = attribute.get(TableFile.OFFSET);
            length[count] = attribute.get(TableFile.LENGTH);

            //准备放置下一组信息
            ++count;
        }

        ShabbyResultSet resultSet = new ShabbyResultSet(rows, offset, length);

        //第几个值
        count = 1;
        for (String attributeName : this.stringBPlusTreeHashMap.keySet()) {
            //将光标移到第一个结果
            resultSet.first();

            //读出相应的树
            BPlusTree bPlusTree = this.stringBPlusTreeHashMap.get(attributeName);

            //datapointers的光标
            int cursor = 0;

            //循环删除树中信息
            do {

                //装要删除的键
                Object keyToBeDeleted = null;

                //判断是什么类型的属性
                switch (this.tableFile.getAttributes().get(attributeName).get(TableFile.TYPE)) {
                    case TableFile.CHAR:
                        keyToBeDeleted = resultSet.getString(count);
                        break;
                    case TableFile.FLOAT:
                        keyToBeDeleted = resultSet.getDouble(count);
                        break;
                    case TableFile.INT:
                        keyToBeDeleted = resultSet.getLong(count);
                        break;
                    default:
                        new Exception("type not found").printStackTrace();
                        break;
                }


                //删除指定数据
                bPlusTree.delete(keyToBeDeleted, dataPointers[cursor]);

                //光标下移
                ++cursor;

            } while (resultSet.next());

            //下一个值
            ++count;
        }

        //重置光标到最初使
        resultSet.reset();

        return resultSet;
    }

    /**
     * 将数据插入DataFile中，返回其在文件中的指针(Long)
     */
    protected Object doDataInsert(InsertInfo info) {

        //要插入的元组中的所有信息
        Object[] objects = info.attributeValues;

        //如果给出的插入元组信息数与表中的给出的属性数不同
        // 输出一条错误信息并返回null
        if (objects.length != this.tableFile.tableInfo.attributes.size()) {
            System.err.println("Insert data does not match, with table info!");
            return null;
        }

        //将insertinfo中的信息转换成byte数组以便输入到文件中
        ByteBuffer buffer = ByteBuffer.allocate(this.tableFile.tableInfo.dataSize);

        //将表文件中存储的属性信息拿出依次用来进行转换
        Collection<ArrayList<Integer>> values = this.tableFile.tableInfo.attributes.values();

        //对应insertinfo的信息的索引
        int i = 0;

        //遍历所有属性信息进行转换
        for (Iterator<ArrayList<Integer>> it = values.iterator(); it.hasNext() && i < objects.length; ++i) {

            ArrayList<Integer> temp = it.next();

            //根据表文件中所给出的属性类型进行转换
            switch (temp.get(TableFile.TYPE)) {
                case DataTypes.INT:
                    buffer.putLong(temp.get(TableFile.OFFSET), ((Number) objects[i]).longValue());
                    break;
                case DataTypes.FLOAT:
                    buffer.putDouble(temp.get(TableFile.OFFSET), ((Number) objects[i]).doubleValue());
                    break;
                case DataTypes.CHAR:
                    buffer.position(temp.get(TableFile.OFFSET));
                    buffer.put(ByteBuffer.allocate(temp.get(TableFile.LENGTH)).put((objects[i].toString()).getBytes()).array());
                    break;
                default:
                    break;
            }

        }

        //调用datafile中的方法将byte数组输入到文件指定位置并返回其指针
        return dataFile.insertAData(buffer.array());
    }


    public static void main(String[] args) throws IOException {
        ShabbyDB db = new ShabbyDB();
        long timeRecord = System.currentTimeMillis();
        /*db.doFile("test");
        System.out.println("test done time consume " + (System.currentTimeMillis() - timeRecord));
        timeRecord = System.currentTimeMillis();
        db.doFile("insert100");
        System.out.println("insert100 done time consume " + (System.currentTimeMillis() - timeRecord));
        timeRecord = System.currentTimeMillis();
        db.doFile("insert1000");
        System.out.println("insert1000 done time consume " + (System.currentTimeMillis() - timeRecord));
        timeRecord = System.currentTimeMillis();
        db.doFile("insert10000");
        System.out.println("insert10000 done time consume " + (System.currentTimeMillis() - timeRecord));
        timeRecord = System.currentTimeMillis();*/
        ArrayList<ShabbyResultSet> resultSets = db.doFile("querys");
        System.out.println("querys done time consume " + (System.currentTimeMillis() - timeRecord));
        System.out.println("This query has " + resultSets.size() + " results");
        for (ShabbyResultSet resultSet : resultSets) {
            int i = 1;
            /*String temp = null;*/
            System.out.println("\nResult starts\n");
            while (resultSet.next()) {
                /*if (temp != null && temp.compareTo(resultSet.getString(1)) > 0) {
                    System.err.println("Not in order.");
                }
                temp = resultSet.getString(1);*/
                System.out.println(i + "  id: " + resultSet.getString(1) + " 姓名: " + resultSet.getString(2) + " 班级: " + resultSet.getLong(3) + " 性别: " + resultSet.getString(4) + " 成绩: " + resultSet.getDouble(5));

//                System.out.println(i + " name : " + resultSet.getString(1));

                ++i;

            }
            System.out.println("\nResult ends\n");
        }
    }

}
