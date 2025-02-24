package com.awol2005ex.hbase.entity;

import java.io.Serializable;

public class HbaseTableStatus implements Serializable {

    private String name;
    private String namespace;
    private Boolean enabled;
    private long disksize;
    private long memstoresize;

    public long getMemstoresize() {
        return memstoresize;
    }

    public HbaseTableStatus setMemstoresize(long memstoresize) {
        this.memstoresize = memstoresize;
        return this;
    }

    public String getName() {
        return name;
    }

    public HbaseTableStatus setName(String name) {
        this.name = name;
        return this;
    }

    public long getDisksize() {
        return disksize;
    }

    public HbaseTableStatus setDisksize(long disksize) {
        this.disksize = disksize;
        return this;
    }

    public String getNamespace() {
        return namespace;
    }

    public HbaseTableStatus setNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public HbaseTableStatus setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public String toString() {
        return "HbaseTableStatus [name=" + name + ", namespace=" + namespace + ", enabled=" + enabled + ", disksize=" + disksize + ", memstoresize=" + memstoresize + "]";
    }
}
