import java.nio.ByteBuffer;

/**
 * 用于存储在查询中
 * 从DataFile中取出的
 * 未经加工的数据
 * 通过类似ResultSet的方法对
 * 得到的数据取出
 */
public class ShabbyResultSet {
    public ShabbyResultSet(byte[][] rows, int[] offset, int[] length) {

        this.offset = offset;
        this.length = length;
        this.rows = rows;
        this.columnSize = offset.length;
        if (rows != null) {
            this.rowSize = rows.length;
        } else {
            this.rowSize = 0;
        }
        this.cursor = -1;
    }


    /**
     * Moves the cursor forward one row from its current position.
     * A ResultSet cursor is initially positioned before the first row;
     * the first call to the method next makes the first row the current row;
     * the second call makes the second row the current row, and so on.
     */
    public boolean next() {
        if (cursor < rowSize - 1) {
            cursor++;
            return true;
        }
        return false;
    }

    public boolean first() {
        if (rowSize > 0) {
            cursor = 0;
            return true;
        }
        return false;
    }

    private boolean isOutOfRange(int columnIndex) {
        if (columnIndex <= 0 || columnIndex > this.columnSize) {
            new Exception("result set visit out of range").printStackTrace();
            return true;
        }
        return false;
    }

    public long getLong(int columnIndex) {

        if (isOutOfRange(columnIndex)) {
            return 0L;
        }
        return ByteBuffer.wrap(rows[cursor]).getLong(offset[columnIndex - 1]);
    }

    public double getDouble(int columnIndex) {
        if (isOutOfRange(columnIndex)) {
            return 0;
        }
        return ByteBuffer.wrap(rows[cursor]).getDouble(offset[columnIndex - 1]);
    }

    public String getString(int columnIndex) {
        if (isOutOfRange(columnIndex)) {
            return null;
        }
        byte[] bytes = new byte[length[columnIndex - 1]];
        ByteBuffer buffer = null;
        try {
            buffer= ByteBuffer.wrap(rows[cursor], offset[columnIndex - 1], length[columnIndex - 1]);
        } catch (IndexOutOfBoundsException e ) {
            e.printStackTrace();
        }
        buffer.get(bytes);
        String[] res = new String(bytes).split("\0");
        return res[0];
    }

    public void reset() {
        this.cursor = -1;
    }

    //一条数据有多少个属性
    private int columnSize;
    //本结果有多少条数据
    private int rowSize;
    //每个属性在一条数据中的位置以及长度
    private int[] offset;
    private int[] length;
    //各条数据
    private byte[][] rows;
    //当前访问的数据-1代表未开始
    private int cursor;
}
