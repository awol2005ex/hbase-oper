package com.awol2005ex.hbase.entity;

import java.io.Serializable;

public class NamespaceStatus implements Serializable {
    private String name;
    private long disksize;
    private long memstoresize;

    public String getName() {
        return name;
    }

    public NamespaceStatus setName(String name) {
        this.name = name;
        return this;
    }

    public long getDisksize() {
        return disksize;
    }

    public NamespaceStatus setDisksize(long disksize) {
        this.disksize = disksize;
        return this;
    }

    public long getMemstoresize() {
        return memstoresize;
    }

    public NamespaceStatus setMemstoresize(long memstoresize) {
        this.memstoresize = memstoresize;
        return this;
    }
}
