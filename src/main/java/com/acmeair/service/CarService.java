package com.acmeair.service;

import com.acmeair.web.dto.Car;

import java.io.IOException;

public abstract class CarService {
    protected abstract Car getCar(String id);

    public abstract Car getCarByName(String name);

    protected abstract String insertCar(String name, String baseCost, String loyaltyPoints);

    protected abstract void loadCarDb() throws IOException;

    protected abstract void purgeDb();
}
