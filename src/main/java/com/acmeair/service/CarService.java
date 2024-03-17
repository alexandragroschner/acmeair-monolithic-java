package com.acmeair.service;

import com.acmeair.web.dto.Car;

import java.io.IOException;

public interface CarService {
    public Car getCar(String id);

    public Car getCarByName(String name);

    public String insertCar(String name, String baseCost, String loyaltyPoints);

    public void loadCarDb() throws IOException;

    public void purgeDb();
}
