package com.cryptocoinpartners.schema;


import org.joda.time.Instant;

import javax.annotation.Nullable;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import java.math.BigDecimal;


/**
 * @author Tim Olson
 */
@MappedSuperclass
public abstract class Pricing extends MarketData {

    /**
     * @param time when the pricing event originally occured
     * @param remoteKey the exchange's unique ID for the pricing event (to prevent duplicates)
     * @param marketListing which MarketListing this pricing is for
     * @param priceCount relative to the MarketListing's quoteBasis
     * @param volumeCount relative to the MarketListing's volumeBasis
     */
    public Pricing(Instant time, @Nullable String remoteKey, MarketListing marketListing,
                   @Nullable Long priceCount, @Nullable Long volumeCount) {
        super(time, remoteKey, marketListing);
        this.priceCount = priceCount;
        this.volumeCount = volumeCount;
    }


    public Pricing(Instant time, @Nullable String remoteKey, MarketListing marketListing,
                   @Nullable BigDecimal price, @Nullable BigDecimal volume) {
        super(time, remoteKey, marketListing);
        this.priceCount = DiscreteAmount.countForValueRounded(price,marketListing.getPriceBasis());
    }


    public @Nullable Long getPriceCount() { return priceCount; }


    public @Nullable Long getVolumeCount() { return volumeCount; }


    @Transient @Nullable
    public DiscreteAmount getPrice() {
        if( priceCount == null )
            return null;
        if( price == null )
            price = new DiscreteAmount(priceCount,getMarketListing().getPriceBasis());
        return price;
    }


    @Transient @Nullable
    public Double getPriceAsDouble() {
        DiscreteAmount price = getPrice();
        return price == null ? null : price.asDouble();
    }


    @Transient @Nullable
    public DiscreteAmount getVolume() {
        if( volumeCount == null )
            return null;
        if( volume == null )
            volume = new DiscreteAmount(volumeCount,getMarketListing().getVolumeBasis());
        return price;
    }


    @Transient @Nullable
    public Double getVolumeAsDouble() {
        DiscreteAmount volume = getVolume();
        return volume == null ? null : volume.asDouble();
    }


    // JPA
    protected Pricing() { super(); }
    protected void setPriceCount(Long priceCount) { this.priceCount = priceCount; }
    protected void setVolumeCount(Long volumeCount) { this.volumeCount = volumeCount; }


    private DiscreteAmount price;
    private DiscreteAmount volume;
    private Long priceCount;
    private Long volumeCount;
}
