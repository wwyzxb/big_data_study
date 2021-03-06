package org.vincent.study.bigdata.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.*;

/**
 * @Author: wuxiaobing
 * @Date 2018/1/31
 **/
public class HBaseUtils {
    /**
     * 使用静态内部类实现的单例模式
     */
    private HBaseUtils() {
    }

    private static class Holder {
        private Holder() {
        }

        private static final HBaseUtils instance = new HBaseUtils();
    }

    public static synchronized HBaseUtils getInstance() {
        return Holder.instance;
    }

    private Connection connection = null;

    /**
     * 连接HBase
     *
     * @return
     */
    private Connection getConnection() {
        try {
            if (connection == null) {
                //默认会到classpath下加载hbase-default.xml和hbase-site.xml文件
                Configuration conf = HBaseConfiguration.create();
                //设置ZooKeeper集群信息
                conf.set("hbase.zookeeper.quorum", "emr-header-1,emr-header-2,emr-header-3");
                conf.set("hbase.zookeeper.property.clientPort", "2181");
                connection = ConnectionFactory.createConnection(conf);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return connection;
    }

    public void closeConnection() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 根据表名来获得表
     *
     * @param tableName
     * @return
     */
    public Table getTable(String tableName) {
        Table table = null;
        try {
            table = getConnection().getTable(TableName.valueOf(tableName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return table;
    }

    /**
     * 向表中添加数据
     *
     * @param tableName
     * @param rowKey
     * @param family
     * @param data
     */
    public void add(String tableName, String rowKey, String family, Map<String, String> data) {
        try {
            Table table = getTable(tableName);
            Put put = new Put(Bytes.toBytes(rowKey));
            for (Map.Entry<String, String> entry : data.entrySet()) {
                put.addColumn(Bytes.toBytes(family), Bytes.toBytes(entry.getKey()), Bytes.toBytes(entry.getValue()));
            }
            table.put(put);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据表名和rowKey遍历指定表的指定行
     *
     * @param tableName
     * @param rowKey
     * @return
     */
    public List<Map<String, String>> getResultByTableAndRowKey(String tableName, String rowKey) {
        Table table = getTable(tableName);
        return getResultFromTableByRowKey(table, rowKey);
    }

    /**
     * 遍历指定表的指定行
     *
     * @param table
     * @param rowKey
     * @return
     */
    public List<Map<String, String>> getResultFromTableByRowKey(Table table, String rowKey) {
        List<Map<String, String>> resultList = new ArrayList<>();
        try {
            Get get = new Get(Bytes.toBytes(rowKey));
            Result result = table.get(get);
            parseResult(result, resultList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resultList;
    }

    /**
     * 解析HBase返回的result
     *
     * @param result
     * @param resultList
     */
    public void parseResult(Result result, List<Map<String, String>> resultList) {
        NavigableMap<byte[], NavigableMap<byte[], NavigableMap<Long, byte[]>>> resultMap = result.getMap();
        for (Map.Entry<byte[], NavigableMap<byte[], NavigableMap<Long, byte[]>>> famlEntry : resultMap.entrySet()) {
            for (Map.Entry<byte[], NavigableMap<Long, byte[]>> qualEntry : famlEntry.getValue().entrySet()) {
                for (Map.Entry<Long, byte[]> valueEntry : qualEntry.getValue().entrySet()) {
                    Map<String, String> map = new HashMap<>();
                    map.put(Bytes.toString(result.getRow()) + ":<" + Bytes.toString(famlEntry.getKey()) + ":" + Bytes.toString(qualEntry.getKey()) + ">:" + valueEntry.getKey(), Bytes.toString(valueEntry.getValue()));
                    resultList.add(map);
                }
            }
        }
    }

    /**
     * 解析HBase返回的result
     *
     * @param result
     * @return
     */
    public List<Map<String, String>> parseResult(Result result) {
        List<Map<String, String>> resultList = new ArrayList<>();
        parseResult(result, resultList);
        return resultList;
    }

    /**
     * 删除指定表指定行的数据
     *
     * @param tableName
     * @param rowKey
     */
    public void delete(String tableName, String rowKey) {
        try {
            Table table = getTable(tableName);
            Delete delete = new Delete(Bytes.toBytes(rowKey));
            table.delete(delete);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 遍历resultScanner并输出结果
     *
     * @param resultScanner
     */
    public void printTheResultofResultScanner(ResultScanner resultScanner) {
        List<Map<String, String>> resultList = new ArrayList<>();
        for (Result result : resultScanner) {
            parseResult(result, resultList);
        }
        System.out.println(resultList);
    }
}
