import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class ReadExecuteFile {

    protected String executeFile;

    protected BufferedReader fileReader;

    protected String lineBuffer;

    /**
     * 设置要执行的文件
     */
    public void setExecuteFile(String source) {

        //如果得到的参数文件名是空的，则产生异常
        if (source == null) {
            throw new NullPointerException();
        }

        //将要读取的文件名保存在类的成员变量内
        this.executeFile = source;


        //打开指定文件
        try {
            fileReader = new BufferedReader(new FileReader(executeFile));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     * 本方法返回正在读取的文件是否还有一行可读
     */
    public boolean hasNext() throws IOException {


        //如果还未指定相应文件则返回假值
        if (fileReader == null) {
            return false;
        }

        //如果被缓存的一行还未被读出则直接返回真
        if (lineBuffer != null) {
            return true;
        }


        //读取新的一行
        lineBuffer = fileReader.readLine();


        //如果没有下一行则返回假值
        return lineBuffer != null;
    }

    /**
     * 在调用了hasNext方法并返回为真的情况下，调用本函数
     * ，返回文件的下一行，并将行缓冲清空
     */
    public String nextCommand() {

        String result = lineBuffer;


        //当读取了文件的下一行时要记得将缓冲变量清空
        lineBuffer = null;


        return result;
    }


    /**
     * 关闭文件
     */
    public void closeFile() throws IOException {
        if (this.fileReader != null) {
            this.fileReader.close();
        }
    }
}
