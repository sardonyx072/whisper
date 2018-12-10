package com.mjjhoffmann.whisper;

import java.text.NumberFormat;

import javax.swing.text.NumberFormatter;

public class PortNumberFormatter extends NumberFormatter {
	public PortNumberFormatter(NumberFormat fmt) {
		super(fmt);
		this.setValueClass(Integer.class);
		this.setMinimum(0);
		this.setMaximum(Integer.MAX_VALUE);
		this.setAllowsInvalid(false);
		//this.setCommitsOnValidEdit(true);
	}
	@Override
	public Integer stringToValue(String text) {
		if (text.isEmpty()) return null;
		else return Integer.parseInt(text);
	}
}
