package com.poc.fraud.domain;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

@Node("IPAddress")
public class IpAddress {

    @Id
    @GeneratedValue
    private Long nodeId;

    @Property("addr")
    private String addr;

    public IpAddress() {
    }

    public IpAddress(String addr) {
        this.addr = addr;
    }

    public Long getNodeId() {
        return nodeId;
    }

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }
}
