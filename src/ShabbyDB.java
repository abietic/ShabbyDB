import java.io.IOException;
import java.util.ArrayList;


public class ShabbyDB {


    //用于多分支选择的常量
    protected static final String createTable = "cr";
    protected static final String insert = "in";
    protected static final String search = "se";
    protected static final String delete = "de";
    protected static final String comment = "##";
    //读取命令文件的类
    protected ReadExecuteFile readExecuteFile;
    //数据库管理对象
    protected StorageEngine storageEngine;

    //构造方法
    public ShabbyDB() {
        this.readExecuteFile = new ReadExecuteFile();
        this.storageEngine = new StorageEngine();
    }

    /**
     * 执行命令文件的方法
     * 参数 要执行的文件名
     * 返回 字符串
     */
    public ArrayList<ShabbyResultSet> doFile(String fileName) throws IOException {


        //设置要执行的文件
        readExecuteFile.setExecuteFile(fileName);

        //临时存放文件中的一条指令
        String command;

        //存放一条指令的返回结果
        ShabbyResultSet result;

        //存放整个之星文件的所有返回结果
        ArrayList<ShabbyResultSet> results = new ArrayList<>();

        //读取文件中的每一条指令并执行返回结果
        while (readExecuteFile.hasNext()) {

            command = readExecuteFile.nextCommand();

            result = execute(command);

            if (result != null) {
                results.add(result);
            }

        }

        readExecuteFile.closeFile();
        return results;
    }

    /**
     * 执行命令的方法
     * 参数 命令字符串
     * 返回 答复字符串
     */
    protected ShabbyResultSet execute(String command) throws IOException {


        ShabbyResultSet resultSet = null;

        StringBuffer result = new StringBuffer();

        String order = command.substring(0, 2);

        switch (order) {


            case createTable:
                String tableInfo = command.substring("createtable ".length());
                result.append(TableFile.creatTable(tableInfo));
                break;

            case insert:
                String insertCommond = command.substring("insert into ".length());
                result.append(storageEngine.insert(insertCommond));
                break;

            case search:
                String searchCommond = command.substring("select from ".length());
                resultSet = storageEngine.search(searchCommond);
                break;

            case delete:
                String deleteCommond = command.substring("delete from ".length());
                resultSet = storageEngine.delete(deleteCommond);
                break;

            case comment:
                break;

            default:
                break;

        }


        return resultSet;
    }

    /*public static void main(String[] args) throws IOException {
        ShabbyDB db = new ShabbyDB();
        db.doFile("test");
    }*/
}
