package com.awol2005ex.hbase;

import com.awol2005ex.hbase.entity.HbaseTableStatus;
import com.awol2005ex.hbase.entity.NamespaceStatus;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.FirstKeyOnlyFilter;
import org.apache.hadoop.hbase.filter.PageFilter;
import org.apache.hadoop.hbase.regionserver.BloomType;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.security.UserGroupInformation;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class HbaseTool {
    private final static ExecutorService executor = Executors.newSingleThreadExecutor();

    public HbaseTool() {
/*
        System.getenv().forEach((key, value) -> {
            System.out.println(key + "=" + value);
        });

        System.getProperties().forEach((key, value) -> {
            System.out.println(key + "=" + value);
        });


*/

    }

    public Connection getConnection(Map<String, String> conf, Map<String, String> env) throws Exception {

        Configuration hbaseConf = HBaseConfiguration.create();
        hbaseConf.set("hbase.rpc.timeout", "1800000");
        hbaseConf.set("hbase.client.scanner.timeout.period", "1800000");
        for (String key : conf.keySet()) {
            //System.out.println(key + "=" + conf.get(key));
            hbaseConf.set(key, conf.get(key));
        }
        for (String key : env.keySet()) {
            //System.out.println(key + "=" + env.get(key));
            System.setProperty(key, env.get(key));
        }

        UserGroupInformation.setConfiguration(hbaseConf);

        if ("kerberos".equals(conf.get("hadoop.security.authentication"))) {
            String krb5Conf = System.getProperty("java.security.krb5.conf");
            if (krb5Conf != null) {
                File krb5File = new File(krb5Conf);
                if (!krb5File.exists()) {
                    throw new RuntimeException("krb5.conf not found . Please set jvm property java.security.krb5.conf ");
                }
            } else {
                throw new RuntimeException(" jvm property java.security.krb5.conf is not set ");

            }
            if (!conf.containsKey("hadoop.security.kerberos.principal")) {
                throw new Exception("hadoop.security.kerberos.principal is not set");
            }
            if (!conf.containsKey("hadoop.security.kerberos.keytab")) {
                throw new Exception("hadoop.security.kerberos.keytab is not set");
            }
            UserGroupInformation.loginUserFromKeytab(conf.get("hadoop.security.kerberos.principal"), conf.get("hadoop.security.kerberos.keytab"));
        }


        return ConnectionFactory.createConnection(hbaseConf);
    }

    public List<NamespaceStatus> getNamespaces(Map<String, String> conf, Map<String, String> env) throws Exception {

        Connection conn = getConnection(conf, env);
        Admin admin = conn.getAdmin();

        List<NamespaceStatus> namespaces = Arrays.stream(admin.listNamespaceDescriptors()).map(n-> new NamespaceStatus().setName(n.getName())).collect(Collectors.toList());
        admin.close();
        conn.close();

        return namespaces;
    }

    public List<NamespaceStatus> getNamespaceMetricsList(Map<String, String> conf, Map<String, String> env) throws Exception {

        Connection conn = getConnection(conf, env);
        Admin admin = conn.getAdmin();
        Collection<ServerName> serviceNames = admin.getRegionServers();
        List<NamespaceStatus> namespaces = Arrays.stream(admin.listNamespaceDescriptors()).map(n->
        {
            NamespaceStatus na=new NamespaceStatus().setName(n.getName());
            long disksize =0L;
            long memstoresize=0L;
            try {
                for (TableDescriptor tableName : admin.listTableDescriptorsByNamespace(n.getName().getBytes())) {
                    for (ServerName serviceName : serviceNames) {
                        List<RegionMetrics> m = admin.getRegionMetrics(serviceName, tableName.getTableName());
                        for (RegionMetrics r : m) {
                            disksize += r.getStoreFileSize().getLongValue();
                            memstoresize += r.getMemStoreSize().getLongValue();
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            na.setMemstoresize(memstoresize);
            na.setDisksize(disksize);
            return na;
        }

        ).collect(Collectors.toList());
        admin.close();
        conn.close();

        return namespaces;
    }

    public Future<List<String>> getNamespacesWithFuture(Map<String, String> conf, Map<String, String> env) throws Exception {


        CompletableFuture<List<String>> completableFuture = new CompletableFuture<>();
        executor.submit(() -> {

            Connection conn = getConnection(conf, env);
            Admin admin = conn.getAdmin();

            List<String> namespaces = Arrays.stream(admin.listNamespaceDescriptors()).map(NamespaceDescriptor::getName).collect(Collectors.toList());
            admin.close();
            conn.close();
            /*
            for(String namespace : namespaces){
                System.out.println(namespace);
            }*/
            completableFuture.complete(namespaces);
            return null;
        });
        return completableFuture;
    }


    public List<HbaseTableStatus> getTables(Map<String, String> conf, Map<String, String> env, String namespace) throws Exception {

        Connection conn = getConnection(conf, env);
        Admin admin = conn.getAdmin();

        List<HbaseTableStatus> tables = new ArrayList<>();
        for (TableDescriptor tableName : admin.listTableDescriptorsByNamespace(namespace.getBytes())) {
            HbaseTableStatus hbaseTableStatus = new HbaseTableStatus().setName(tableName.getTableName().getNameAsString()).setNamespace(namespace).setEnabled(admin.isTableEnabled(tableName.getTableName()));
            tables.add(hbaseTableStatus);
        }
        admin.close();
        conn.close();

        return tables;
    }

    public List<HbaseTableStatus> getTableMetricsList(Map<String, String> conf, Map<String, String> env, String namespace) throws Exception {

        Connection conn = getConnection(conf, env);
        Admin admin = conn.getAdmin();
        Collection<ServerName> serviceNames = admin.getRegionServers();

        List<HbaseTableStatus> tables = new ArrayList<>();
        for (TableDescriptor tableName : admin.listTableDescriptorsByNamespace(namespace.getBytes())) {
            HbaseTableStatus hbaseTableStatus = new HbaseTableStatus().setName(tableName.getTableName().getNameAsString()).setNamespace(namespace).setEnabled(admin.isTableEnabled(tableName.getTableName()));
            long disksize =0L;
            long memstoresize=0L;
            for (ServerName serviceName : serviceNames) {
                List<RegionMetrics> m = admin.getRegionMetrics(serviceName, tableName.getTableName());
                for (RegionMetrics r : m) {
                    disksize += r.getStoreFileSize().getLongValue();
                    memstoresize += r.getMemStoreSize().getLongValue();
                }
            }
            hbaseTableStatus.setDisksize(disksize).setMemstoresize(memstoresize);

            tables.add(hbaseTableStatus);
        }
        admin.close();
        conn.close();

        return tables;
    }


    public List<String> getTableColumnFamilies(Map<String, String> conf, Map<String, String> env, String tablename) throws Exception {

        Connection conn = getConnection(conf, env);

        Table table = conn.getTable(TableName.valueOf(tablename));
        List<String> fields = Arrays.stream(table.getDescriptor().getColumnFamilies()).map(ColumnFamilyDescriptor::getNameAsString).toList();
        table.close();
        conn.close();

        return fields;
    }

    public List<Map<String, String>> getTableDataList(Map<String, String> conf, Map<String, String> env, String tablename, int pageNum, int pageSize) throws Exception {
        List<Map<String, String>> list = new ArrayList<>();
        Connection conn = getConnection(conf, env);

        Table table = conn.getTable(TableName.valueOf(tablename));
        Scan scan = new Scan();

        // 组装 Filter 列表
        FilterList filterList = new FilterList();
        // PageFilter
        PageFilter pageFilter = new PageFilter(pageSize);
        //注意：如果用到了多个 filter，其中包含 pagefilter，那么 pagefilter 需要放在 fiterlist 的最后一个
        filterList.addFilter(pageFilter);
        scan.setFilter(filterList);
        //记录上一次返回的分页数据中的最大的 Rowkey，最开始为 null
        byte[] lastRowKey = null;

        for (int currentPage = 1; currentPage <= pageNum; currentPage++) {
            if (lastRowKey != null) {
                // 注意：在这里需要在 lastRowkey 后面补 0，否则会把当前这条数据也返回过来，这样就重复了，
                // 补 0 之后可以保证返回的都是新数据，避免重复数据
                scan.withStartRow(Bytes.add(lastRowKey, new byte[]{0}));
            }

            //获取结果
            ResultScanner resultScanner = table.getScanner(scan);
            //记录每次迭代的数据条数
            int rowCount = 0;

            for (Result result : resultScanner) {
                if (currentPage == pageNum) {
                    HashMap<String, String> map = new HashMap<>();
                    String row = Bytes.toString(result.getRow());
                    map.put("row", row);
                    for (Cell cell : result.listCells()) {
                        String family = Bytes.toString(cell.getFamilyArray(),
                                cell.getFamilyOffset(), cell.getFamilyLength());
                        String qualifier = Bytes.toString(cell.getQualifierArray(),
                                cell.getQualifierOffset(), cell.getQualifierLength());
                        String data = Bytes.toString(cell.getValueArray(),
                                cell.getValueOffset(), cell.getValueLength());
                        map.put(family + ":" + qualifier, data);
                    }
                    list.add(map);
                }

                lastRowKey = result.getRow();
                rowCount++;

                //scan 返回的数据是基于 rowkey 有序的，直接判断数据条数即可。
                //当前页面数据获取完毕，退出循环
                if (rowCount >= pageSize) {
                    break;
                }
            }

            resultScanner.close();
        }

        table.close();

        conn.close();

        return list;
    }

    public Integer getTableDataCount(Map<String, String> conf, Map<String, String> env, String tablename) throws Exception {
        Connection conn = getConnection(conf, env);
        Table table = conn.getTable(TableName.valueOf(tablename));
        Scan scan = new Scan();
        scan.setFilter(new FirstKeyOnlyFilter());
        ResultScanner resultScanner = table.getScanner(scan);
        int rowCount = 0;
        for (Result result : resultScanner) rowCount++;
        table.close();
        conn.close();
        return rowCount;
    }

    public void createTable(Map<String, String> conf, Map<String, String> env, String settings) throws Exception {
        Connection conn = getConnection(conf, env);

        Admin admin = conn.getAdmin();


        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        Map<String, Object> settinsMap = gson.fromJson(settings, type);
        TableName tableName = TableName.valueOf((String) settinsMap.get("tableName"));

        if (admin.tableExists(tableName)) {
            throw new RuntimeException("Table already exists");
        }


        TableDescriptorBuilder tableDescBuilder = TableDescriptorBuilder.newBuilder(tableName);
        List<Map<String, Object>> columnFamilies = (List<Map<String, Object>>) settinsMap.get("columnFamilies");
        if (columnFamilies != null && !columnFamilies.isEmpty()) {

            for (Map<String, Object> columnFamily : columnFamilies) {
                ColumnFamilyDescriptorBuilder familyBuilder = ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes((String) columnFamily.get("name")));

                for (Map.Entry<String, Object> entry : columnFamily.entrySet()) {
                    if (entry.getKey().equals("name")) {
                        continue;
                    }
                    switch (entry.getKey()) {
                        case "maxVersions":
                            familyBuilder.setMaxVersions(Double.valueOf(entry.getValue().toString()).intValue());
                            break;
                        case "timeToLive":
                            familyBuilder.setTimeToLive(Double.valueOf(entry.getValue().toString()).intValue());
                            break;
                        case "blockSize":
                            familyBuilder.setBlocksize(Double.valueOf(entry.getValue().toString()).intValue());
                            break;
                        case "bloomFilterType":
                            familyBuilder.setBloomFilterType(BloomType.valueOf((String) entry.getValue()));
                            break;

                    }
                }
                tableDescBuilder.setColumnFamily(familyBuilder.build());
            }
        }
        TableDescriptor tableDesc = tableDescBuilder.build();
        admin.createTable(tableDesc);


        admin.close();
        conn.close();
    }


    //创建表空间
    public void createNamespace(Map<String, String> conf, Map<String, String> env, String namespace) throws Exception {
        Connection conn = getConnection(conf, env);
        Admin admin = conn.getAdmin();
        long c = Arrays.stream(admin.listNamespaceDescriptors()).filter(ns -> ns.getName().equals(namespace)).count();
        if (c > 0) {
            throw new RuntimeException("Namespace already exists");
        }
        NamespaceDescriptor descriptor = NamespaceDescriptor.create(namespace).build();
        admin.createNamespace(descriptor);
        admin.close();
        conn.close();
    }

    //删除表空间
    public void deleteNamespace(Map<String, String> conf, Map<String, String> env, String namespace) throws Exception {
        Connection conn = getConnection(conf, env);
        Admin admin = conn.getAdmin();
        admin.deleteNamespace(namespace);
        admin.close();
        conn.close();
    }

    //删除表
    public void deleteTable(Map<String, String> conf, Map<String, String> env, String tablename) throws Exception {
        Connection conn = getConnection(conf, env);
        Admin admin = conn.getAdmin();
        admin.deleteTable(TableName.valueOf(tablename));
        admin.close();
        conn.close();
    }

    //启用表
    public void enableTable(Map<String, String> conf, Map<String, String> env, String tablename) throws Exception {
        Connection conn = getConnection(conf, env);
        Admin admin = conn.getAdmin();
        if (!admin.isTableEnabled(TableName.valueOf(tablename))) {
            admin.enableTable(TableName.valueOf(tablename));
        }
        admin.close();
        conn.close();
    }

    //禁用表
    public void disableTable(Map<String, String> conf, Map<String, String> env, String tablename) throws Exception {
        Connection conn = getConnection(conf, env);
        Admin admin = conn.getAdmin();
        if (admin.isTableEnabled(TableName.valueOf(tablename))) {
            admin.disableTable(TableName.valueOf(tablename));
        }
        admin.close();
        conn.close();
    }

    public HbaseTableStatus getTableMetrics(Map<String, String> conf, Map<String, String> env, HbaseTableStatus tablestatus) throws Exception {
        Connection conn = getConnection(conf, env);
        Admin admin = conn.getAdmin();
        TableName name = TableName.valueOf(tablestatus.getName());

        // 获取表的所有 region
        Collection<ServerName> serviceNames = admin.getRegionServers();
        long disksize = 0L;
        long memstoresize = 0L;

        for (ServerName serviceName : serviceNames) {
            List<RegionMetrics> m = admin.getRegionMetrics(serviceName, name);
            for (RegionMetrics r : m) {
                disksize += r.getStoreFileSize().getLongValue();
                memstoresize += r.getMemStoreSize().getLongValue();
            }
        }
        tablestatus.setDisksize(disksize).setMemstoresize(memstoresize);

        admin.close();
        conn.close();
        return tablestatus;
    }


}
