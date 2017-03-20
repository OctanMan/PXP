package net.named_data.pxp.entity;

import com.google.gson.*;


import java.lang.reflect.Type;
import java.util.Base64;

/**
 * Thanks go to https://gist.github.com/orip/3635246 for the example of using Base64 with Gson
 */
public class ByteArrayToBase64TypeAdapter implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {
    public byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return Base64.getDecoder().decode(json.getAsString());
    }

    public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(Base64.getEncoder().encodeToString(src));
    }
}
