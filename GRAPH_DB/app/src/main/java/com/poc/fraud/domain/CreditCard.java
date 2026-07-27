package com.poc.fraud.domain;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

@Node("CreditCard")
public class CreditCard {

    @Id
    @GeneratedValue
    private Long nodeId;

    @Property("number")
    private String number;

    public CreditCard() {
    }

    public CreditCard(String number) {
        this.number = number;
    }

    public Long getNodeId() {
        return nodeId;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }
}
