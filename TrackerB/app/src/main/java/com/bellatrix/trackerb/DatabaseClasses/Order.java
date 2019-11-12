package com.bellatrix.trackerb.DatabaseClasses;

public class Order {

    private String customer, delivery, description, delivered;

    public Order() {

    }

    public Order(String customer, String delivery, String description) {
        this.customer = customer;
        this.delivery = delivery;
        this.description = description;
        this.delivered = "0";
    }

    public String getCustomer() {
        return this.customer;
    }

    public String getDelivery() {
        return this.delivery;
    }

    public String getDescription() {
        return this.description;
    }

    public String getDelivered() {
        return this.delivered;
    }
}
