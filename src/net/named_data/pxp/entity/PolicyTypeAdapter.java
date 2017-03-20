package net.named_data.pxp.entity;

import com.google.gson.*;
import net.named_data.jndn.Name;

import java.lang.reflect.Type;

/**

 */
public final class PolicyTypeAdapter implements JsonSerializer<Policy>, JsonDeserializer<Policy> {

    public JsonElement serialize(Policy src, Type typeOfSrc, JsonSerializationContext context) {

        JsonObject policy = new JsonObject();
        policy.addProperty("id", src.id.toUri());
        policy.add("owner", context.serialize(src.owner));
        policy.addProperty("nickName", src.nickName);
        policy.addProperty("dossierUri", src.dossierUri.toUri());

        //JsonObject owner = new JsonObject();
        //owner.addProperty("id", src.getOwner().getId().toUri());
        //owner.addProperty("nickName", src.getOwner().nickName);
        //policy.add("owner", owner);

        return policy;
    }

    public Policy deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

        JsonObject jobj = json.getAsJsonObject();

        Policy policy = new Policy();
        policy.id = new Name(jobj.get("id").getAsString());
        policy.owner = context.deserialize(jobj.get("owner"), Actor.class);
        if(jobj.has("nickName"))
            policy.nickName = jobj.get("nickName").getAsString();
        if(jobj.has("dossierUri")) //<-- It really SHOULD have this but it can be set to null
            policy.dossierUri = new Name(jobj.get("dossierUri").getAsString());

        return policy;
    }
}
