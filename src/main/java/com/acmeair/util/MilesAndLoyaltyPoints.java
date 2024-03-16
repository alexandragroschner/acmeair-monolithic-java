package com.acmeair.util;

public class MilesAndLoyaltyPoints {

    private Long miles;
    private Long loyaltyPoints;

    public MilesAndLoyaltyPoints() {
    }

    public MilesAndLoyaltyPoints(Long miles, Long loyaltyPoints) {
        this.setMiles(miles);
        this.setLoyaltyPoints(loyaltyPoints);
    }

    public Long getMiles() {
        return miles;
    }

    public void setMiles(Long miles) {
        this.miles = miles;
    }

    public Long getLoyaltyPoints() {
        return loyaltyPoints;
    }

    public void setLoyaltyPoints(Long loyaltyPoints) {
        this.loyaltyPoints = loyaltyPoints;

    }
}
