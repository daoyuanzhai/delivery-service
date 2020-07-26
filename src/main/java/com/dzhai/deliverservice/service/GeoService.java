package com.dzhai.deliverservice.service;

import com.dzhai.deliverservice.model.Location;
import com.dzhai.deliverservice.util.Util;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.RestTemplate;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class GeoService {
    @Value("${tamu.geocoding.url}")
    private String geocodingUrl;

    @Value("${tamu.geocoding.key}")
    private String geocodingKey;

    private final RestTemplate restTemplate;
    private final Util util;

    @Autowired
    public GeoService(final RestTemplate restTemplate, Util util) {
        this.restTemplate = restTemplate;
        this.util = util;
    }

    public int getLatLng() throws IOException {
        log.info("Reading excel file...");
        val fileInputStream = new FileInputStream(ResourceUtils.getFile("classpath:amazon-orders-latlng-2018.xlsx"));
        val workbook = new XSSFWorkbook(fileInputStream);
        val sheet = workbook.getSheetAt(0);
        log.info("Done reading excel file...");
        fileInputStream.close();

        var count = 0;
//        for (int index = 1; index < sheet.getPhysicalNumberOfRows(); index++) {
        for (int index = 1; index < 324170; index++) {
            val row = sheet.getRow(index);

//            if (row.getCell(8) != null && row.getCell(9) != null) {
            if (row.getCell(6) != null && row.getCell(7) != null
                    && row.getCell(6).getNumericCellValue() != 0 && row.getCell(7).getNumericCellValue() != 0) {
                continue;
            }
            var address = row.getCell(1) == null ? "" : row.getCell(1).getStringCellValue();
            if (address.isEmpty()) {
                continue;
            }
            val poundSignIndex = address.indexOf('#');
            if (poundSignIndex != -1) {
                address = address.substring(0, poundSignIndex);
            }
//            address += row.getCell(2) == null ? "" : " " + row.getCell(2).getStringCellValue();
//            address += row.getCell(3) == null ? "" : " " + row.getCell(3).getStringCellValue();

            val city = row.getCell(2).getStringCellValue();

            val state = row.getCell(5).getStringCellValue();

            String zip;
            try {
                zip = row.getCell(3).getStringCellValue();
            } catch (IllegalStateException ex) {
                log.error(ex.getMessage());
                zip = String.valueOf((int)row.getCell(3).getNumericCellValue());
            }
            zip = zip.length() < 5 ? "0" + zip : zip;

            log.info(address + ", " + city + ", " + state + ", " + zip);

            var urlStringBuilder = new StringBuilder(geocodingUrl);
            urlStringBuilder.append("?apiKey="+geocodingKey);
            urlStringBuilder.append("&version=4.01");
            urlStringBuilder.append("&streetAddress="+address.replace(' ', '+'));
            urlStringBuilder.append("&city="+city.replace(' ', '+'));
            urlStringBuilder.append("&state="+state.replace(' ', '+'));
            urlStringBuilder.append("&zip="+zip);

            try {
                var uri = new URI(urlStringBuilder.toString());
                var response = restTemplate.getForEntity(uri, String.class);
                var responseArray = response.getBody().split(",");
                log.info("(" + responseArray[3] + ", " + responseArray[4] + ")");
//                row.createCell(8).setCellValue(Double.valueOf(responseArray[3]));
//                row.createCell(9).setCellValue(Double.valueOf(responseArray[4]));
                row.createCell(6).setCellValue(Double.valueOf(responseArray[3]));
                row.createCell(7).setCellValue(Double.valueOf(responseArray[4]));
                count++;

                if (count % 100 == 0) {
                    val fileOutputStream = new FileOutputStream("src/main/resources/amazon-orders-latlng-2018.xlsx");
                    workbook.write(fileOutputStream);
                    fileOutputStream.close();
                    log.info("saving to excel: " + count);
                }
            } catch (Exception ex) {
                log.error(ex.getMessage());
            }
        }

        val fileOutputStream = new FileOutputStream("src/main/resources/amazon-orders-latlng-2018.xlsx");
        workbook.write(fileOutputStream);
        fileOutputStream.close();
        workbook.close();
        return count;
    }

    public int getDistances() throws IOException {
        log.info("Reading summary file...");
        val fileInputStream = new FileInputStream(ResourceUtils.getFile("classpath:amazon-data-summary.xlsx"));
        val workbook = new XSSFWorkbook(fileInputStream);
        val citySheet = workbook.getSheetAt(0);
//        val amazon2018Sheet = workbook.getSheetAt(1);
        val amazon2018Sheet = workbook.getSheetAt(2);
        log.info("Done reading excel file...");
        fileInputStream.close();

        for (int cityIndex = 1; cityIndex < 36; cityIndex++) {
            val cityRow = citySheet.getRow(cityIndex);
            val cityName = cityRow.getCell(2).getStringCellValue();
            val cityLat = cityRow.getCell(3).getNumericCellValue();
            val cityLng = cityRow.getCell(4).getNumericCellValue();
            log.info("Processing " + cityName + " (" + cityLat + ", " + cityLng + ")");

            var disMap = new HashMap<String, Double>();
            val locMap = new HashMap<String, Location>();

            for (int orderIndex = 1; orderIndex < amazon2018Sheet.getPhysicalNumberOfRows(); orderIndex++) {
                val orderRow = amazon2018Sheet.getRow(orderIndex);
                val orderId = Integer.toString((int)orderRow.getCell(0).getNumericCellValue());
                val orderLatCell = orderRow.getCell(5);
                val orderLngCell = orderRow.getCell(6);
                if (orderLatCell == null || orderLngCell == null
                        || orderLatCell.getNumericCellValue() == 0 || orderLngCell.getNumericCellValue() == 0) {
                    continue;
                }
                val orderLat = orderLatCell.getNumericCellValue();
                val orderLng = orderLngCell.getNumericCellValue();

                val distance = calculateDistance(cityLat, cityLng, orderLat, orderLng, "M");
                disMap.put(orderId, distance);
                locMap.put(orderId, new Location(orderLat, orderLng));
            }
            disMap = util.sortByValue(disMap);
            val resultWorkbook = new XSSFWorkbook();

            val amazon2018ResultSheet = resultWorkbook.createSheet("2018");

            val resultCityRow = (amazon2018ResultSheet.getRow(0) == null) ?
                    amazon2018ResultSheet.createRow(0) : amazon2018ResultSheet.getRow(0);
//            resultCityRow.createCell(cityIndex * 4 - 1).setCellValue(cityName);
            resultCityRow.createCell(0).setCellValue("Order ID");
            resultCityRow.createCell(1).setCellValue("Distance");
            resultCityRow.createCell(2).setCellValue("Lat");
            resultCityRow.createCell(3).setCellValue("Lng");

            var rowCount = 1;
            for (val entry : disMap.entrySet()) {
                val resultOrderRow = (amazon2018ResultSheet.getRow(rowCount) == null) ?
                        amazon2018ResultSheet.createRow(rowCount) : amazon2018ResultSheet.getRow(rowCount);

                val orderId = entry.getKey();
//                resultOrderRow.createCell(cityIndex * 4 - 4).setCellValue(orderId);
                resultOrderRow.createCell(0).setCellValue(orderId);

                val orderDistance = entry.getValue();
//                resultOrderRow.createCell(cityIndex * 4 - 3).setCellValue(orderDistance);
                resultOrderRow.createCell(1).setCellValue(orderDistance);

                val orderLocation = locMap.get(orderId);
//                resultOrderRow.createCell(cityIndex * 4 - 2).setCellValue(orderLocation.getLatitude());
                resultOrderRow.createCell(2).setCellValue(orderLocation.getLatitude());
//                resultOrderRow.createCell(cityIndex * 4 - 1).setCellValue(orderLocation.getLongitude());
                resultOrderRow.createCell(3).setCellValue(orderLocation.getLongitude());

                rowCount++;
            }
            val fileOutputStream = new FileOutputStream("src/main/resources/" + cityName.replace(" ", "-")+ ".xlsx");
            resultWorkbook.write(fileOutputStream);
            fileOutputStream.close();
            resultWorkbook.close();
        }
        return 0;
    }


    private static double calculateDistance(double lat1, double lng1, double lat2, double lng2, String unit) {
        if ((lat1 == lat2) && (lng1 == lng2)) {
            return 0;
        }
        else {
            double theta = lng1 - lng2;
            double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
            dist = Math.acos(dist);
            dist = Math.toDegrees(dist);
            dist = dist * 60 * 1.1515;
            if (unit.equals("K")) {
                dist = dist * 1.609344;
            } else if (unit.equals("N")) {
                dist = dist * 0.8684;
            }
            return (dist);
        }
    }
}
