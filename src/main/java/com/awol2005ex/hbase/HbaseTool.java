package com.awol2005ex.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.NamespaceDescriptor;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.security.UserGroupInformation;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class HbaseTool {
    private static ExecutorService executor = Executors.newSingleThreadExecutor();

    public HbaseTool() {
/*
        System.getenv().forEach((key, value) -> {
            System.out.println(key + "=" + value);
        });

        System.getProperties().forEach((key, value) -> {
            System.out.println(key + "=" + value);
        });

        String krb5Conf = System.getProperty("java.security.krb5.conf");
        if(krb5Conf!=null){
            File krb5File =new File(krb5Conf);
            if(!krb5File.exists()){
                throw  new RuntimeException("krb5.conf not found");
            }
        }
*/

    }

    public Connection getConnection(Map<String, String> conf) throws Exception {

        Configuration hbaseConf = HBaseConfiguration.create();

        for (String key : conf.keySet()) {
            //System.out.println(key + "=" + conf.get(key));
            hbaseConf.set(key, conf.get(key));
        }

        UserGroupInformation.setConfiguration(hbaseConf);

        if ("kerberos".equals(conf.get("hadoop.security.authentication"))) {
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

    public List<String> getNamespaces(Map<String, String> conf) throws Exception {

        Connection conn = getConnection(conf);
        Admin admin = conn.getAdmin();

        List<String> namespaces = Arrays.stream(admin.listNamespaceDescriptors()).map(NamespaceDescriptor::getName).collect(Collectors.toList());
        admin.close();
        conn.close();

        return namespaces;
    }

    public Future<List<String>> getNamespacesWithFuture(Map<String, String> conf) throws Exception {


        CompletableFuture<List<String>> completableFuture = new CompletableFuture<>();
        executor.submit(() -> {

            Connection conn = getConnection(conf);
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
}
