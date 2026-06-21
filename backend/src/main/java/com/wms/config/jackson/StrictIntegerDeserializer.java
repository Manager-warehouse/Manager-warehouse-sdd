package com.wms.config.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

public class StrictIntegerDeserializer extends JsonDeserializer<Integer> {

    @Override
    public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonToken token = parser.currentToken();

        if (token == JsonToken.VALUE_NUMBER_INT) {
            return parser.getIntValue();
        }

        if (token == JsonToken.VALUE_NUMBER_FLOAT) {
            throw context.weirdNumberException(parser.getDecimalValue(),
                    Integer.class,
                    "Fractional quantities are not allowed");
        }

        if (token == JsonToken.VALUE_STRING) {
            String raw = parser.getText();
            try {
                return Integer.valueOf(raw);
            } catch (NumberFormatException ex) {
                throw context.weirdStringException(raw,
                        Integer.class,
                        "Quantity must be a whole number");
            }
        }

        return (Integer) context.handleUnexpectedToken(Integer.class, parser);
    }
}
