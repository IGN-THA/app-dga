//package com.docprocess.constant;
//
//import com.fasterxml.jackson.annotation.JsonCreator;
//import com.fasterxml.jackson.annotation.JsonValue;
//import lombok.AllArgsConstructor;
//import lombok.Getter;
//
//import java.text.MessageFormat;
//
//@Getter
//@AllArgsConstructor
//public enum TemplateType {
//
//    QUOTE_SLIP("quote-slip");
//
//    private final String value;
//
//    @JsonCreator
//    public static TemplateType fromValue(String value) {
//        for (TemplateType it : values()) {
//            if (it.getValue().equalsIgnoreCase(value.trim())) {
//                return it;
//            }
//        }
//        throw new UnsupportedOperationException(MessageFormat.format("Cannot parse string {0} to enum {1}", value, TemplateType.class));
//    }
//
//    @JsonValue
//    public String toValue() {
//        return value;
//    }
//}
