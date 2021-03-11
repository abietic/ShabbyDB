import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

public class CommandInterpreter implements DataTypes {

    /**
     * 本方法是对字符串型的信息处理成table的数据
     */
    public static TableInfo tableInfoInterpret(String tableInfo) {

        //将信息通过空格分割
        String[] infos = tableInfo.split(" ");

        //第一个信息为表名
        String tableName = infos[0];

        //不符合书写规范的直接放弃
        if (!infos[1].equals("(")) {
            return null;
        }


        //用于计算dataSize
        int size = 0;

        //记录偏移量
        int offset = 0;


        //以属性名为键值，第一个值为类型，第二个值为偏移量，第三个值为数据长度
        LinkedHashMap<String, ArrayList<Integer>> attributes = new LinkedHashMap<>();


        //读取出命令中的属性信息
        for (int i = 2; i < infos.length && !infos[i].equals(")"); i++) {

            //存储属性名
            String attributeName = infos[i];


            //对该属性法的信息进行存储
            ArrayList<Integer> typeAndLength = new ArrayList<>(4);


            //读取下一个Token
            ++i;


            //该Token为属性的数据类型
            int type = DataTypes.judgeType(infos[i]);


            //类型长度的初始值为8字节
            int length = 8;


            //但如果数据类型为字符串，则需再读取下一个长度数值
            if (type == CHAR) {
                ++i;
                length = Integer.parseInt(infos[i]) * 2;
            }


            //将得到的数值存入数组
            /*System.out.println(typeAndLength.size());*/
            typeAndLength.add(type);
            typeAndLength.add(length);
            typeAndLength.add(offset);


            //将得到的数值存入属性表中
            attributes.put(attributeName, typeAndLength);


            //更新偏移量
            offset += length;

        }

        //计算一条data的大小 , 4字节对齐
        if (offset % 4 == 0) {
            size = offset;
        } else {
            size = (offset / 4) + 4;
        }

        return new TableInfo(tableName, size, attributes);
    }


    /**
     * 对插入指令的翻译得到要插入的数据的值
     */
    public static InsertInfo insertInfoInterpret(String insertCommand) {

        if(insertCommand==null)//如果指令为空，返回searchinfo为空
        {
            return null;
        }
        //	insertCommand = "insert into <tableName> ( 23 2.56 d 5.8 )";//测试代码
        String tablename = null;
        ArrayList attributeValues=new ArrayList();// 用来接收处理完的各属性信息

        /* 对字符串的处理 */
        String[] infos = insertCommand.split(" ");// 用空格分割信息




        tablename = infos[0];//




        /* 获取属性内容并简单判错 */
        for (int i = 2; i < infos.length - 1; i++)
        {
            attributeValues.add(judgeType(infos[i]));//生成对应的类型类

        }



        InsertInfo container = new InsertInfo(tablename, attributeValues.toArray());// 用来接收插入命令的信息的容器
        return container;
    }


    /**
     * 对搜索指令的翻译
     */
    public static SearchInfo searchInfoInterpret(String searchCommand) {
        if(searchCommand==null)//如果指令为空，返回searchinfo为空
        {
            return null;
        }
        //	searchCommand = "select from <tableName> <attributeName> = <searchKey>";
        Object from = null , to =null;
        String tablename, attributeName = null;
        int whichSearch = SearchInfo.NORMAL_SEARCH;
        String[] infos = searchCommand.split(" ");

        //读取要搜索的数据所在的关系
        tablename = infos[0];

        //如果只提供了表名那么是显示全部数据
        if (infos.length == 1) {
            return new SearchInfo(tablename , attributeName , from , to , SearchInfo.SHOW_ALL);
        }

        //取要搜索的数据的根据属性
        attributeName = infos[1];

        //如果是等号代表是一个搜索
        if ("=".equals(infos[2])) {
            from = to = judgeType(infos[3]);
            return new SearchInfo(tablename , attributeName , from , to , SearchInfo.NORMAL_SEARCH);
        }

        //如果是between代表是一个范围搜索
        if ("between".equals(infos[2])) {
            from = judgeType(infos[4]);
            to = judgeType(infos[5]);
            return new SearchInfo(tablename , attributeName , from , to , SearchInfo.RANGE_SEARCH);
        }

        new Exception("An invalid search command").printStackTrace();
        return null;
    }

    /**
     * 对删除指令进行翻译
     * */

    public static DeleteInfo deleteInfoInterpret(String deleteCommand) {
        SearchInfo searchInfo = searchInfoInterpret(deleteCommand);
        return new DeleteInfo(searchInfo.tableName , searchInfo.attributeName , searchInfo.from,searchInfo.to,searchInfo.whichSearch);
    }


    /**
     * 此方法判断str是否为整型,还是字符型，还是字符串型，返回Object类型
     * */
    private static Object judgeType(String str) {

        if (null == str || "".equals(str)) {
            return null;
        }

        Object attributeValues = null;
        Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
        Pattern pattern1 = Pattern.compile("^[-\\+]?[.\\d]*$");


        // 判断是否为整数
        if (pattern.matcher(str).matches())
        {
            attributeValues = new Long(str);//封装整数类
            //		System.out.println("整数");     //用于测试语句，已测试
        }
        //判断为浮点型
        else if(pattern1.matcher(str).matches())
        {
            attributeValues = new Double(str);
            //	System.out.println("浮点型");
        }
        //判断为String类型
        else
        {
            attributeValues = new String(str);//封装字符串类
            //			System.out.println("字符串");
        }
        return attributeValues;//返回Object对象
    }
}
