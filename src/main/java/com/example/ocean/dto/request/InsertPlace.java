package com.example.ocean.dto.request;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@NoArgsConstructor
@AllArgsConstructor
@Data
public class InsertPlace {
    private String          eventCd;
    private String          placeNm;
    private String          placeId;
    private String          address;
    private double          lat;
    private double          lng;
}
