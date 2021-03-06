package com.cryptocoinpartners.module.xchangedata;

import com.cryptocoinpartners.schema.Market;
import com.cryptocoinpartners.schema.Markets;


/**
 * 
 * @author Philip 
 * this class provides mapping between Exchange and Market
 * 
 */
public class ExchangeMarketMapping {

	private static Market market;

	/**
	 * @param id the exchange ID
	 * @return the Market corresponding to the ID
	 */
	public static Market getMarketByExchangeId(int id) {

		//TODO
		switch (id) {
		case ExchangeNames.Bitfinex:
			market = Markets.BITFINEX;
			break;
		default:
			return null;
		}
		return market;
	}

}
