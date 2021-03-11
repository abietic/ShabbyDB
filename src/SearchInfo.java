public class SearchInfo {

    //在哪个关系中寻找
    public String tableName;

    //通过哪个索引寻找
    public String attributeName;

    //指定的索引值
    public Object from , to;

    //指定是哪种检索
    public int whichSearch;

    //检索类型
    public static final int NORMAL_SEARCH = 1;

    public static final int RANGE_SEARCH = 2;

    public static final int SHOW_ALL = 3;


    public SearchInfo(String tableName, String attributeName, Object from, Object to, int whichSearch) {
        this.tableName = tableName;
        this.attributeName = attributeName;
        this.from = from;
        this.to = to;
        this.whichSearch = whichSearch;
    }
}
