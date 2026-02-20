package com.carrental.paymentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "bookingservice",
        url = "${booking.service.url}"
)
public interface BookingServiceClient {

    @PostMapping("/api/bookings/{id}/confirm")
    void confirmBooking(
            @PathVariable("id") Long id,
            @RequestParam("paymentTxnId") String paymentTxnId
    );

    @PostMapping("/api/bookings/{id}/confirm/internal")
    void confirmBookingInternal(
            @PathVariable("id") Long id,
            @RequestParam("paymentTxnId") String paymentTxnId,
            @RequestHeader("X-Internal-Call") String header
    );
}
