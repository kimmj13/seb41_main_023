package com.newyear.mainproject.place.service;

import com.newyear.mainproject.exception.BusinessLogicException;
import com.newyear.mainproject.exception.ExceptionCode;
import com.newyear.mainproject.place.entity.Place;
import com.newyear.mainproject.place.repository.PlaceRepository;
import com.newyear.mainproject.plan.service.PlanService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class PlaceService {

    private final PlaceRepository placeRepository;

    public PlaceService(PlaceRepository placeRepository, PlanService planService) {
        this.placeRepository = placeRepository;
    }

    /**
     * 해당 일정에 대한 장소 정보 등록
     */
    public Place createPlace(Place place) {
        return placeRepository.save(place);
    }

    /**
     * 해당 일정에 대한 장소 정보 수정
     */
    public Place updatePlace(Place place) {
        Place findPlace = findVerifiedPlace(place.getPlaceId());

        Optional.ofNullable(place.getPlaceName())
                .ifPresent(placeName -> findPlace.setPlaceName(placeName));
        //장소에서 예산 등록했을 때 처리

        Optional.ofNullable(place.getExpense())
                .ifPresent(expense -> findPlace.setExpense(expense));


        Optional.ofNullable(place.getStartTime())
                .ifPresent(startTime -> findPlace.setStartTime(startTime));
        Optional.ofNullable(place.getEndTime())
                .ifPresent(endTime -> findPlace.setEndTime(endTime));

        return placeRepository.save(findPlace);
    }

    /**
     * 해당 일정에 대한 장소 삭제
     */
    public void deletePlace(Long placeId) {
        Place findPlace = findVerifiedPlace(placeId);
        placeRepository.delete(findPlace);
    }

    /**
     * 장소 정보 존재 여부 확인
     */
    private Place findVerifiedPlace(Long placeId) {
        Optional<Place> optionalPlace = placeRepository.findById(placeId);
        return optionalPlace.orElseThrow(() -> new BusinessLogicException(ExceptionCode.PLACE_NOT_FOUND));
    }
}