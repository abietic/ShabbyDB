import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

public class DataFile implements DataTypes {


    /**
     * 文件头长度
     */
    private static final int FILE_HEADER_SIZE = 256;
    /**
     * 更新nextPointer时的文件偏移量
     */
    private static final int NEXT_POINTER_SAVE_POSITION = 4;
    /**
     * 成员变量
     */
    private DataFileInfo dataFileInfo;
    private File file;
    private RandomAccessFile accessFile;
    /**
     * 该文件的单条数据长度
     */
    private int dataSize;

    /**
     * 构造函数
     */
    public DataFile() {
        this.dataFileInfo = new DataFileInfo();
        this.dataSize = 0;
        this.accessFile = null;
    }

    /**
     * 创建存放数据的文件
     */
    public static void createDataFile(DataFileInfo dataFileInfo) {

        //如果传入的参数为空抛出异常
        if (dataFileInfo == null) {
            throw new NullPointerException();
        }

        //试图创建DataFile
        File file = new File("datas/" + dataFileInfo.tableName);


        try {

            /*//如果已经存在则直接返回
            if (!file.createNewFile()) {
                return;
            }*/

            file.createNewFile();

            //进行文件的随机读取
            RandomAccessFile dataFileOutput = new RandomAccessFile(file, "rwd");

            ByteBuffer header = ByteBuffer.allocate(FILE_HEADER_SIZE);


            //写入单条数据大小信息
            header.putInt(dataFileInfo.dataSize);

            //初始化虚拟指针
            header.putLong(dataFileInfo.nextPointer);


            //写入文件
            dataFileOutput.write(header.array());

            //完成写入关闭文件
            dataFileOutput.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 读取相应文件中的数据
     */
    public void setDataFile(String tableName) {

        //如果是同一文件则无需操作直接返回
        if (tableName.equals(dataFileInfo.tableName)) {
            return;
        }

        //如果不是开始试图读取文件
        this.file = new File("datas/" + tableName);

        //如果不是可执行文件直接返回
        if (!file.isFile()) {
            return;
        }

        //读取文件
        try {

            if (accessFile != null) {
                accessFile.close();
            }

            accessFile = new RandomAccessFile(file, "rws");

            int dataSize = accessFile.readInt();

            long nextPointer = accessFile.readLong();

            DataFileInfo info = new DataFileInfo(tableName, dataSize);

            info.nextPointer = nextPointer;

            this.dataFileInfo = info;


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 得到对应的一条数据
     */
    public byte[] getData(Object dataPointer) {

        //如果未设置文件抛出异常
        if (this.dataFileInfo == null) {
            throw new NullPointerException();
        }

        //得到虚拟指针
        long vPointer = (Long) dataPointer;

        //计算出真实位置
        long tPointer = (vPointer - 1) * this.dataFileInfo.dataSize + FILE_HEADER_SIZE;

        try {

            /*//创建文件读取
            RandomAccessFile accessFile = new RandomAccessFile(this.file, "r");*/

            //寻得相应位置
            accessFile.seek(tPointer);

            //将这条数据读出
            byte[] bytes = new byte[this.dataFileInfo.dataSize];

            accessFile.read(bytes);

            /*accessFile.close();*/

            //返回读取的数据
            return bytes;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 插入一条数据到DataFile中
     */
    public Long insertAData(byte[] dataValue) {

        if (this.dataFileInfo == null) {
            throw new NullPointerException();
        }

        //输入内容与需求信息不匹配时放弃操作返回无效指针
        if (dataValue.length != this.dataFileInfo.dataSize) {
            return 0L;
        }

        //保存下一个可用位置
        long position = this.dataFileInfo.nextPointer;

        //计算出真实的可插入位置
        long truePosition = (position - 1) * this.dataFileInfo.dataSize + FILE_HEADER_SIZE;


        try {
            /*//完成插入
            RandomAccessFile accessFile = new RandomAccessFile(file, "rwd");*/

            //移动到插入位置
            accessFile.seek(truePosition);

            //执行插入
            accessFile.write(dataValue);

            //更新成员变量中的信息值
            this.dataFileInfo.nextPointer = position + 1;

            //移动到文件头修改信息
            accessFile.seek(NEXT_POINTER_SAVE_POSITION);

            accessFile.writeLong(this.dataFileInfo.nextPointer);

            /*accessFile.close();*/

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        return position;
    }
}
