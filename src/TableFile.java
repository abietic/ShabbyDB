import java.io.*;
import java.util.*;


public class TableFile implements DataTypes {

    /**
     * 表值中各个index对应的信息种类常量
     */
    public static int TYPE = 0;
    public static int LENGTH = 1;
    public static int OFFSET = 2;
    protected TableInfo tableInfo;



    /**
     * 通过公共静态方法创建表文件
     */
    public static String creatTable(String tableInfo) throws IOException {

        //通过CommandIntepreter的静态方法得到相应数据
        //将处理好的信息传入创建用的方法进行创建并返回创建结果

        return doCreateTable(CommandInterpreter.tableInfoInterpret(tableInfo));
    }

    /**
     * 进行进一步的信息处理与操作
     */
    protected static String doCreateTable(TableInfo tableInfo) throws IOException {

        //存放返回结果
        StringBuffer result = new StringBuffer();
        result.append("Table file " + tableInfo.tableName);


        //创建用于存储表信息的文件，由于可能会产生异常，所以本方法也加入了异常抛出
        File tableFile = new File("tables/" + tableInfo.tableName);

        tableFile.createNewFile();

        ObjectOutputStream tableOutput = new ObjectOutputStream(new FileOutputStream(tableFile));


        //写入tableInfo
        writeTable(tableOutput, tableInfo);

        //关闭文件
        tableOutput.close();

        //完成了表信息文件的创建
        result.append(" created successfully.\n");


        //建立相应的数据文件
        DataFile.createDataFile(new DataFileInfo(tableInfo.tableName, tableInfo.dataSize));


        //顺带完成相应索引文件的初始化
        //对map进行迭代遍历
        Set<Map.Entry<String, ArrayList<Integer>>> set = tableInfo.attributes.entrySet();

        for (Iterator<Map.Entry<String, ArrayList<Integer>>> it = set.iterator();
             it.hasNext();
                ) {

            Map.Entry<String, ArrayList<Integer>> entry = it.next();

            IndexInfo indexInfo = new IndexInfo(tableInfo.tableName, entry.getKey(), entry.getValue());
            result.append(IndexFile.createIndexFile(indexInfo));
        }

        return result.toString();
    }

    /**
     * 将表信息写入
     */
    protected static void writeTable(ObjectOutputStream outputStream, TableInfo tableInfo) throws IOException {

        //写入tableName
        outputStream.writeObject(tableInfo.tableName);

        //写入dataSize
        outputStream.writeObject(tableInfo.dataSize);

        //写入attributes

        outputStream.writeObject(tableInfo.attributes.size());

        Set<Map.Entry<String, ArrayList<Integer>>> entrySet = tableInfo.attributes.entrySet();


        for (Iterator<Map.Entry<String, ArrayList<Integer>>> it = entrySet.iterator(); it.hasNext(); ) {
            Map.Entry<String, ArrayList<Integer>> entry = it.next();
            outputStream.writeObject(entry.getKey());
            outputStream.writeObject(entry.getValue());
        }
    }

    /**
     * 与写入表的相同顺序读入文件中的表信息
     */
    protected TableInfo readTable(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {

        //读入表的名字
        String tableName = (String) inputStream.readObject();

        //读入单条数据大小
        int dataSize = (Integer) inputStream.readObject();

        //读入接下来要读入的键值对个数
        int attributeSize = (Integer) inputStream.readObject();

        //创建表的属性信息表
        LinkedHashMap<String, ArrayList<Integer>> attributes = new LinkedHashMap<>();

        //读入所有键值对
        for (int i = 0; i < attributeSize; ++i) {
            String attributeName = (String) inputStream.readObject();
            ArrayList<Integer> typeAndLength = (ArrayList<Integer>) inputStream.readObject();
            attributes.put(attributeName, typeAndLength);
        }

        //返回读取完整的TableInfo
        return new TableInfo(tableName, dataSize, attributes);
    }

    /**
     * 通过该函数对对象所指向的表进行修改
     */
    public void setTable(String fileName) {

        //如果传入参数为空抛出异常
        if (fileName == null) {
            throw new NullPointerException();
        }

        //开始试图对相应表信息文件的读取
        File file = new File("tables/" + fileName);

        //如果该不是一个有效的文件则直接返回
        if (!file.isFile()) {
            System.out.println("The file " + fileName + " is invalid.");
            tableInfo = null;
            return;
        }

        //如果是有效文件则开始建立输入流将信息读入。
        ObjectInputStream objectInputStream = null;
        try {
            objectInputStream = new ObjectInputStream(new FileInputStream(file));
            //读入存储在文件中的信息
            tableInfo = readTable(objectInputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 得到表的全部属性
     * 用于得到data之后内容的
     * 全部输出
     * 平时用取单个信息的方法就行了
     */
    public LinkedHashMap<String, ArrayList<Integer>> getAttributes() {
        return this.tableInfo.attributes;
    }

    /**
     * 得到表的全部信息，用于读取索引文件
     */
    public TableInfo getTableInfo() {
        return this.tableInfo;
    }

    /**
     * 得到一条data的长度
     */
    public int getDataSize() {
        return this.tableInfo.dataSize;
    }

    /**
     * 得到表的名字
     * 用于判断是否
     * 需要替换当前
     * 表
     */
    public String getTableName() {
        if (this.tableInfo == null) {
            return null;
        }
        return this.tableInfo.tableName;
    }

    /**
     * 获得表中所有属性的名字
     */
    public String[] getAttributeNames() {

        Set<String> keySet = this.tableInfo.attributes.keySet();

        String[] attributeNames = new String[keySet.size()];

        Iterator<String> it = keySet.iterator();

        for (int i = 0; i < keySet.size() && it.hasNext(); ++i) {
            attributeNames[i] = it.next();
        }

        return attributeNames;
    }


    /**测试表的创建与读取*/
    /*public static void main(String[] args) {
        TableFile tableFile = new TableFile();
        tableFile.setTable("student_info");
        System.out.println(tableFile.getTableName());
        System.out.println(tableFile.getDataSize());
        TableInfo tableInfo = tableFile.getTableInfo();
        Set<Map.Entry<String, ArrayList<Integer>>> entrySet = tableInfo.attributes.entrySet();


        for (Iterator<Map.Entry<String, ArrayList<Integer>>> it = entrySet.iterator() ; it.hasNext() ; ) {
            Map.Entry<String, ArrayList<Integer>> entry = it.next();
            System.out.println(entry.getKey());
        }
    }*/
}
