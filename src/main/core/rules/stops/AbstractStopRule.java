package main.core.rules.stops;

import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Order;
import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.Trade;
import eu.verdelhan.ta4j.TradingRecord;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;
import eu.verdelhan.ta4j.trading.rules.AbstractRule;
import main.core.enums.StopType;

public abstract class AbstractStopRule extends AbstractRule {

	protected final Decimal resultLimit;
	protected final StopType stopType;

	private ClosePriceIndicator closePrice;

	public AbstractStopRule(ClosePriceIndicator closePrice, Decimal resultLimit, StopType stopType) {
		this.closePrice = closePrice;
		this.resultLimit = resultLimit;
		this.stopType = stopType;
	}

	protected abstract Decimal getResult(Order entry, Tick tick);

	protected abstract Decimal getExitPrice(Order entry, Decimal result);

	@Override
	public boolean isSatisfied(int index, TradingRecord tradingRecord) {
		boolean satisfied = false;

		Order entry = getEntryOrder(tradingRecord);
		if (entry != null) {
			Tick tick = closePrice.getTimeSeries().getTick(index);
			Decimal result = this.getResult(entry, tick);

			if (this.stopType == StopType.PERCENTAGE) {
				result = result.multipliedBy(Decimal.HUNDRED).dividedBy(entry.getPrice());
			}

			satisfied = result.isGreaterThanOrEqual(this.resultLimit);
		}

		return satisfied;
	}

	public Decimal getExitPrice(TradingRecord tradingRecord) {
		Order entry = this.getEntryOrder(tradingRecord);
		if (entry == null) {
			throw new IllegalArgumentException("Trading record must hold a opened trade");
		}

		Decimal result = resultLimit;
		if (this.stopType == StopType.PERCENTAGE) {
			result = result.multipliedBy(entry.getPrice()).dividedBy(Decimal.HUNDRED);
		}
		
		return this.getExitPrice(entry, result);
	}

	private Order getEntryOrder(TradingRecord tradingRecord) {
		if (tradingRecord != null) {
			Trade currentTrade = tradingRecord.getCurrentTrade();

			if (currentTrade.isOpened()) {
				return currentTrade.getEntry();
			}
		}

		return null;
	}
}