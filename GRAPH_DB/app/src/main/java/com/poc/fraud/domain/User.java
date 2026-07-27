package com.poc.fraud.domain;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashSet;
import java.util.Set;

@Node("User")
public class User {

    @Id
    @GeneratedValue
    private Long nodeId;

    @Property("id")
    private String id;

    private String name;

    /** active | banned */
    private String status;

    private boolean riskFlag;

    @Relationship(type = "OWNS", direction = Relationship.Direction.OUTGOING)
    private Set<CreditCard> cards = new HashSet<>();

    @Relationship(type = "USED_FROM", direction = Relationship.Direction.OUTGOING)
    private Set<IpAddress> ipAddresses = new HashSet<>();

    @Relationship(type = "USED_DEVICE", direction = Relationship.Direction.OUTGOING)
    private Set<Device> devices = new HashSet<>();

    @Relationship(type = "HAS_PHONE", direction = Relationship.Direction.OUTGOING)
    private Set<PhoneNumber> phoneNumbers = new HashSet<>();

    public User() {
    }

    public User(String id, String name, String status, boolean riskFlag) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.riskFlag = riskFlag;
    }

    public Long getNodeId() {
        return nodeId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isRiskFlag() {
        return riskFlag;
    }

    public void setRiskFlag(boolean riskFlag) {
        this.riskFlag = riskFlag;
    }

    public Set<CreditCard> getCards() {
        return cards;
    }

    public Set<IpAddress> getIpAddresses() {
        return ipAddresses;
    }

    public Set<Device> getDevices() {
        return devices;
    }

    public Set<PhoneNumber> getPhoneNumbers() {
        return phoneNumbers;
    }
}
