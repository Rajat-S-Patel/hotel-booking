package com.rajat.hotelbooking.common.mapper;

import com.rajat.hotelbooking.common.dto.MoneyResponse;
import com.rajat.hotelbooking.common.entity.Money;
import org.mapstruct.Mapper;

@Mapper
public interface MoneyMapper {

    default MoneyResponse toResponse(Money money) {
        return MoneyResponse.from(money);
    }
}
