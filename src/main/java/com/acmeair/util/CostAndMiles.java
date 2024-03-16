package com.acmeair.util;

public class CostAndMiles {

    private Long cost;
    private Long miles;

    public CostAndMiles() {
    }

    public CostAndMiles(Long cost, Long miles) {
        this.setCost(cost);
        this.setMiles(miles);
    }

    public Long getCost() {
        return cost;
    }

    public void setCost(Long cost) {
        this.cost = cost;
    }

    public Long getMiles() {
        return miles;
    }

    public void setMiles(Long miles) {
        this.miles = miles;
    }
}
