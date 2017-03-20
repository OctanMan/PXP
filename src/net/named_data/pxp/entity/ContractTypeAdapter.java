package net.named_data.pxp.entity;

import com.google.gson.*;
import net.named_data.jndn.Name;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**

 */
public final class ContractTypeAdapter implements JsonSerializer<Contract>, JsonDeserializer<Contract> {

    public JsonElement serialize(Contract src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject contract = new JsonObject();

        contract.add("policy", context.serialize(src.policy));
        contract.add("client", context.serialize(src.client));
        contract.add("contractor", context.serialize(src.contractor));

        DateFormat df = new SimpleDateFormat("yyyy/MM/dd");
        contract.addProperty("validFrom", df.format(src.validFrom));
        contract.addProperty("validTo", df.format(src.validTo));

        contract.addProperty("permissionToSubcontract", src.permissionToSubcontract.name());
        if(src.parentContractUri != null)
            contract.addProperty("parentContractUri", src.parentContractUri.toUri());

        contract.add("clientCertificate", context.serialize(src.clientCertificate));
        contract.add("clientSignature", context.serialize(src.clientSignature));
        contract.add("contractorCertificate", context.serialize(src.contractorCertificate));
        contract.add("contractorSignature", context.serialize(src.contractorSignature));

        return contract;
    }

    public Contract deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jobj = json.getAsJsonObject();

        Contract contract = new Contract();
        contract.policy = context.deserialize(jobj.get("policy"), Policy.class);
        contract.client = context.deserialize(jobj.get("client"), Actor.class);
        contract.contractor = context.deserialize(jobj.get("contractor"), Actor.class);

        DateFormat df = new SimpleDateFormat("yyyy/MM/dd");
        try {
            contract.validFrom = df.parse(jobj.get("validFrom").getAsString());
            contract.validTo = df.parse(jobj.get("validTo").getAsString());
        } catch (ParseException e) {
            e.printStackTrace();
            throw new JsonParseException("Error parsing valid Date");
        }

        contract.permissionToSubcontract = context.deserialize(
                jobj.get("permissionToSubcontract"), Contract.Permission.class);
        if(jobj.has("parentContractUri"))
            contract.parentContractUri = new Name(jobj.get("parentContractUri").getAsString());

        contract.clientCertificate = context.deserialize(jobj.get("clientCertificate"), byte[].class);
        contract.clientSignature = context.deserialize(jobj.get("clientSignature"), byte[].class);
        contract.contractorCertificate = context.deserialize(jobj.get("contractorCertificate"), byte[].class);
        contract.contractorSignature = context.deserialize(jobj.get("contractorSignature"), byte[].class);

        return contract;
    }
}
