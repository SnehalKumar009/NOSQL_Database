package com.poc.fraud.domain;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

@Node("Device")
public class Device {

    @Id
    @GeneratedValue
    private Long nodeId;

    @Property("id")
    private String deviceId;

    public Device() {
    }

    public Device(String deviceId) {
        this.deviceId = deviceId;
    }

    public Long getNodeId() {
        return nodeId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}
