package com.example.ocean.service;

import com.example.ocean.domain.Place;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
class PlaceServiceTest {


    @Autowired
    PlaceService  service;

    @Test

    public  void test(){
        List<Place> p = service.findByWorkspaceCd("c668eccd-f338-4867-941b-a43bf9893c88");
        System.out.println(p);
    }
}