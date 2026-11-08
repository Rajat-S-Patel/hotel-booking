package com.rajat.hotelbooking.payment.mapper;

import com.rajat.hotelbooking.common.mapper.MoneyMapper;
import com.rajat.hotelbooking.payment.dto.RefundResponse;
import com.rajat.hotelbooking.payment.entity.RefundAttempt;
import org.mapstruct.Mapper;

@Mapper(uses = MoneyMapper.class)
public interface PaymentMapper {

    RefundResponse toResponse(RefundAttempt refund);
}
