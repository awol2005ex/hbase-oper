# HBase 管理工具库 (用于rust j4rs调用)

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

## 🚀 核心功能
- **命名空间管理**：创建/删除命名空间、获取命名空间列表
- **表操作**：动态建表（支持JSON配置）、启用/禁用表、删除表
- **数据查询**：分页扫描表数据（集成PageFilter）、行数统计
- **元数据管理**：获取表列族信息、表状态检测
- **异步支持**：命名空间列表的异步查询
- **安全认证**：集成Kerberos认证支持

## 📦 技术栈
- **核心框架**: HBase Client 2.x
- **JSON处理**: Gson
- **构建工具**: Gradle
- **并发模型**: Java ExecutorService
- **单元测试**: JUnit 5

## ⚙️ 快速开始
### 环境要求
- Java 8+
- HBase 2.x 集群
- Gradle 6.8+

### 安装

```shell
git clone https://github.com/awol2005ex/hbase-oper.git

cd hbase-oper

./gradlew shadowJar
```

### 基础用法

```java
public static Map<String, String> getConf(){
        Map<String, String> conf = new HashMap<>();
        conf.put("hbase.rootdir", "hdfs://nameservice1/hbase");
        conf.put("hbase.zookeeper.quorum", "nn3,nn2,nn1");
        conf.put("hbase.zookeeper.property.clientPort", "2181");
        conf.put("hbase.master.kerberos.principal","hbase/_HOST@XXX.COM");
        // 必需
        conf.put("hadoop.security.authentication", "kerberos");
        conf.put("hadoop.security.authorization", "true");
        // 必需
        conf.put("hbase.security.authentication", "kerberos");
        conf.put("hbase.defaults.for.version.skip","true");

        conf.put("hbase.regionserver.kerberos.principal", "hbase/_HOST@XXX.COM");
        conf.put("hbase.thrift.kerberos.principal", "hbase/_HOST@XXX.COM");
        conf.put("hbase.thrift.ssl.enabled", "false");
        conf.put("hbase.rpc.protection","authentication");
        conf.put("hadoop.security.kerberos.keytab","/tmp/hive@XXX.COM.keytab");
        conf.put("hadoop.security.kerberos.principal","hive@XXX.COM");

        return conf;
    }

    public static Map<String, String> getEnv(){
        Map<String, String> env = new HashMap<>();
        env.put("java.security.krb5.conf", "D:/MIT/krb5.ini");
        env.put("javax.security.auth.useSubjectCredsOnly", "false");
        return env;
    }
    @Test
    public void testNamespaces() throws Exception {

        // krb5.conf必需
        System.setProperty("java.security.krb5.conf", "D:/MIT/krb5.ini");


        System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");



        HbaseTool hbaseTool = new HbaseTool();

        Map<String, String> conf = getConf();
        Map<String, String> env = getEnv();
        List<String> namespaces = hbaseTool.getNamespaces(conf,env);
        System.out.println(namespaces);
    }
```
## 🤝 贡献指南
1. Fork项目仓库
2. 创建特性分支（`git checkout -b feature/xxx`）
3. 提交修改（`git commit -am 'Add some feature'`）
4. 推送分支（`git push origin feature/xxx`）
5. 新建Pull Request

## 📄 许可证
本项目采用 [Apache License 2.0](LICENSE)
