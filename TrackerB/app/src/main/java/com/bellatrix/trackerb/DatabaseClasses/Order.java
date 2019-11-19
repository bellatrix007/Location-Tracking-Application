package com.bellatrix.trackerb.DatabaseClasses;

public class Order {

    private String admin, customer, delivery, description, delivered;

    public Order() {

    }

    public Order(String admin, String customer, String delivery, String description) {
        this.admin = admin;
        this.customer = customer;
        this.delivery = delivery;
        this.description = description;
        this.delivered = "0";
    }

    public String getAdmin() {
        return this.admin;
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
