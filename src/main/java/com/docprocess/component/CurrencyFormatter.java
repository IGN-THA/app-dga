package com.docprocess.component;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Optional;

@Component
public class CurrencyFormatter {
    public String format(BigDecimal value, Locale locale) {
        DecimalFormat formatter = new DecimalFormat();
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(',');
        return Optional.ofNullable(value)
                .filter(it -> it.compareTo(BigDecimal.ZERO) > 0)
                .map(it -> it.setScale(0, RoundingMode.HALF_UP))
                .map(formatter::format)
                .map(it -> {
                    if (locale.getLanguage().equalsIgnoreCase("en")) {
                        return it + " THB";
                    }else if (locale.getLanguage().equalsIgnoreCase("zh")) {
                        return it + " THB";
                    }
                    return it + " บาท";
                })
                .orElse("-");
    }

    public String formatAddon(BigDecimal value, Locale locale) {
        DecimalFormat formatter = new DecimalFormat();
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(',');
        return Optional.ofNullable(value)
                .filter(it -> it.compareTo(BigDecimal.ZERO) > 0)
                .map(it -> it.setScale(0, RoundingMode.HALF_UP))
                .map(formatter::format)
                .map(it -> {
                    if (locale.getLanguage().equalsIgnoreCase("en")) {
                        return it + " THB";
                    } else if (locale.getLanguage().equalsIgnoreCase("zh")) {
                        return it + " THB";
                    }
                    return it + " บาท";
                })
                .orElse("");
    }

    public String formatDecimal(BigDecimal value, Locale locale) {
        DecimalFormat formatter = new DecimalFormat("#,##0.00");
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(',');
        return Optional.ofNullable(value)
                .filter(it -> it.compareTo(BigDecimal.ZERO) > 0)
//                .map(it -> it.setScale(2, RoundingMode.HALF_UP))
                .map(formatter::format)
                .map(it -> {
                    if (locale.getLanguage().equalsIgnoreCase("en")) {
                        return it + " THB";
                    }else if (locale.getLanguage().equalsIgnoreCase("zh")) {
                        return it + " THB";
                    }
                    return it + " บาท";
                })
                .orElse("-");
    }
}
