package com.awol2005ex.hbase.entity;

import java.io.Serializable;

public class HbaseTableStatus implements Serializable {

    private String name;
    private String namespace;
    private Boolean enabled;

    public String getName() {
        return name;
    }

    public HbaseTableStatus setName(String name) {
        this.name = name;
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
}
