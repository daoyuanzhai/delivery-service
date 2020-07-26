package com.dzhai.deliverservice.controller;

import com.dzhai.deliverservice.service.GeoService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ResourceUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@RestController
@RequestMapping(path = "/geocoding")
@Slf4j
@Validated
public class GeoServiceController {
    private final GeoService geoService;

    @Autowired
    public GeoServiceController(final GeoService geoService) {
        this.geoService = geoService;
    }

    @GetMapping("")
    public @ResponseBody
    ResponseEntity<Integer> getLatLng() throws IOException {
        val count = geoService.getLatLng();
        return new ResponseEntity<>(count, HttpStatus.OK);
    }

    @GetMapping("/distance-matrix")
    public @ResponseBody
    ResponseEntity<Integer> getDistanceMatrix() throws IOException {
        val count = geoService.getDistances();
        return new ResponseEntity<>(count, HttpStatus.OK);
    }
}
