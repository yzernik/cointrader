package com.cryptocoinpartners.schema;

import org.joda.time.Instant;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import java.math.BigDecimal;


/**
 * @author Tim Olson
 */
@MappedSuperclass
public class Quote extends Pricing {

    public Quote(Side side, MarketListing marketListing, Instant time, String remoteKey,
                 Long priceCount, Long volumeCount) {
        super(time, remoteKey, marketListing, priceCount, volumeCount);
        this.side = side;
    }


    @Enumerated(EnumType.STRING)
    public Side getSide() {
        return side;
    }


    // JPA
    protected Quote() {}
    protected void setSide(Side side) { this.side = side; }


    private Side side;
}
