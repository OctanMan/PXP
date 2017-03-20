package net.named_data.pxp.entity;

import com.google.gson.*;
import net.named_data.jndn.Name;

import java.lang.reflect.Type;

/**

 */
public class ActorTypeAdapter implements JsonSerializer<Actor>, JsonDeserializer<Actor> {

    public JsonElement serialize(Actor src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject actor = new JsonObject();

        actor.addProperty("id", src.id.toUri());
        actor.addProperty("nickName", src.nickName);
        actor.addProperty("email", src.email);
        actor.addProperty("organisation", src.organisation);
        actor.addProperty("paymentInfo", src.paymentInfo);

        return actor;
    }

    public Actor deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

        JsonObject jobj = json.getAsJsonObject();

        Actor actor = new Actor();
        actor.id = new Name(jobj.get("id").getAsString());

        if(jobj.has("nickName"))
            actor.nickName = jobj.get("nickName").getAsString();
        if(jobj.has("email"))
            actor.email = jobj.get("email").getAsString();
        if(jobj.has("organisation"))
            actor.organisation = jobj.get("organisation").getAsString();
        if(jobj.has("paymentInfo"))
        actor.paymentInfo = jobj.get("paymentInfo").getAsString();

        return actor;
    }
}
