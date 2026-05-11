package me.jangsalt.sampling.common.component.mask;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import java.io.IOException;

public class MaskSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private final MaskType maskType;

    public MaskSerializer() {
        this(MaskType.DEFAULT);
    }

    private MaskSerializer(MaskType maskType) {
        this.maskType = maskType;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (!MaskingContext.isMaskingEnabled()) {
            gen.writeString(value);
            return;
        }
        gen.writeString(MaskingUtil.mask(value, maskType));
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
        if (property != null) {
            Mask annotation = property.getAnnotation(Mask.class);
            if (annotation != null) {
                return new MaskSerializer(annotation.value());
            }
        }
        return this;
    }
}
