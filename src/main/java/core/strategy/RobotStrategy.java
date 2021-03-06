package core.strategy;

import java.util.ArrayList;
import java.util.List;

import core.parameters.RobotParameters;
import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Order.OrderType;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.Trade;
import eu.verdelhan.ta4j.TradingRecord;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;
import shell.global.GlobalSettings;

public class RobotStrategy {

	public static List<Trade> backtest(RobotParameters parameters) {
		return backtest(GlobalSettings.timeSeries(), parameters);
	}

	public static List<Trade> backtest(TimeSeries series, RobotParameters parameters) {
		ClosePriceIndicator closePrices = new ClosePriceIndicator(series);
		StrategyRules rules = new StrategyRules(closePrices, parameters);

		return run(series, rules, parameters);
	}

	private static List<Trade> run(TimeSeries series, StrategyRules rules, RobotParameters param) {
		List<Trade> trades = new ArrayList<>();

		TradingRecord buyingRecord = new TradingRecord(OrderType.BUY);
		TradingRecord sellingRecord = new TradingRecord(OrderType.SELL);

		boolean bought = false;
		boolean sold = false;

		for (int i = series.getBegin(); i < series.getEnd(); i++) {
			Decimal buyOperate = rules.buyOperate(i, buyingRecord);
			Decimal sellOperate = rules.sellOperate(i, sellingRecord);

			boolean newBuyPosition = bought ^ buyOperate != null;
			boolean newSellPosition = sold ^ sellOperate != null;

			if (newBuyPosition && newSellPosition) {
				buyOperate = sellOperate = null;
			}

			if (buyOperate != null) {
				buyingRecord.operate(i, buyOperate, GlobalSettings.numberOfContracts());
				bought = !bought;

				if (bought) {
					rules.startNewTrade();
				} else {
					trades.add(buyingRecord.getLastTrade());
				}
			}
			if (sellOperate != null) {
				sellingRecord.operate(i, sellOperate, GlobalSettings.numberOfContracts());
				sold = !sold;

				if (sold) {
					rules.startNewTrade();
				} else {
					trades.add(sellingRecord.getLastTrade());
				}
			}
		}

		if (bought) {
			buyingRecord.operate(series.getEnd(), series.getTick(series.getEnd()).getClosePrice(),
					GlobalSettings.numberOfContracts());
			trades.add(buyingRecord.getLastTrade());
		}

		if (sold) {
			sellingRecord.operate(series.getEnd(), series.getTick(series.getEnd()).getClosePrice(),
					GlobalSettings.numberOfContracts());
			trades.add(sellingRecord.getLastTrade());
		}

		return trades;
	}
}
