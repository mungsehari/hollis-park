package com.hollispark.store.service.content;

import com.hollispark.store.dto.content.CarePickDto;
import com.hollispark.store.entity.content.CarePick;
import com.hollispark.store.exception.BadRequestException;
import com.hollispark.store.exception.ResourceNotFoundException;
import com.hollispark.store.repository.content.CarePickRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Care picks (affiliate products) edited in the admin panel. */
@Service
@RequiredArgsConstructor
public class CarePickService {
    private final CarePickRepository picks;

    @Transactional(readOnly = true)
    public List<CarePick> list() {
        return picks.findAllByOrderBySortOrderAscCreatedAtAsc();
    }

    @Transactional
    public CarePick create(CarePickDto in) {
        checkLink(in);
        CarePick p = CarePick.builder().build();
        in.applyTo(p);
        return picks.saveAndFlush(p);
    }

    @Transactional
    public CarePick update(UUID id, CarePickDto in) {
        checkLink(in);
        CarePick p = find(id);
        in.applyTo(p);
        return picks.saveAndFlush(p);
    }

    @Transactional
    public void delete(UUID id) {
        picks.delete(find(id));
    }

    private static void checkLink(CarePickDto in) {
        if (!in.marketplace().accepts(in.url())) {
            throw new BadRequestException("Use an https link on " + in.marketplace().label() + " (for example the product page or your affiliate short link).");
        }
    }

    private CarePick find(UUID id) {
        return picks.findById(id).orElseThrow(() -> new ResourceNotFoundException("Pick not found."));
    }
}
