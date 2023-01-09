package com.newyear.mainproject.place.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.newyear.mainproject.plan.dto.PlanDto;
import lombok.*;

public class PlaceDto {

    @Getter
    @Setter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Post {
        private Long placeDateId;
        private String placeName;
    }

    @Getter
    @Setter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Patch {
        private Long placeId;
        private String placeName;
        private Integer expense;
        private String startTime;
        private String endTime;
    }

    @Builder
    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL) // response null 필드 제외
    public static class Response {
        private PlanDto.PlanDatesResponse planDates;
        private Long placeId;
        private String placeName;
        private Integer expense;
        private String startTime;
        private String endTime;
    }
}
