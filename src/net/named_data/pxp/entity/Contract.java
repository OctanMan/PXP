package net.named_data.pxp.entity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.der.DerDecodingException;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.security.certificate.IdentityCertificate;
import net.named_data.jndn.security.identity.IdentityManager;
import net.named_data.jndn.util.Blob;

import java.util.Date;

/**

 */
@DatabaseTable(tableName = "contracts")
public final class Contract {

    //Note: Technically a surrogate id and not part of the Contract itself.
    // Value is relative to each Actor's local MetaStorage (i.e. Database)
    @DatabaseField(generatedId = true)
    private int id;

    //Note: For persistent storage, explicitly define index in case additional states are added
    public enum Status { UNSIGNED (0), SIGNED (1), EXPIRED (2), REVOKED (3), TERMINATED (4);
        public int index;
        Status(int i) { this.index = i; } }

    //Note: For persistent storage, explicitly define index in case additional states are added
    public enum Permission { NO (0), YES (1);
        public int index;
        Permission(int i) { this.index = i; } }

    //Note: Status is metadata about the Contract and not part of the Contract itself
    @DatabaseField
    Status status;

    // ############################## CONTRACT ATTRIBUTES AND WORKFLOW ##############################

    //BEGIN CLIENT ----- Encode attributes -----

    @DatabaseField(foreign = true, uniqueCombo = true)
    Policy policy;

    @DatabaseField(foreign = true, uniqueCombo = true)
    Actor client;

    @DatabaseField(foreign = true, uniqueCombo = true)
    Actor contractor;

    @DatabaseField
    Date validFrom;

    @DatabaseField
    Date validTo;

    @DatabaseField
    Permission permissionToSubcontract;

    @DatabaseField
    Name parentContractUri;

    @DatabaseField(dataType = DataType.BYTE_ARRAY)
    byte[] clientCertificate;

    // ----- Sign -----

    @DatabaseField(dataType = DataType.BYTE_ARRAY)
    byte[] clientSignature;

    // ----- Append signature, getJsonEncoding as Data, sign and sent to Contractor ----- END CLIENT

    //BEGIN CONTRACTOR ----- Receive from Client, verify, (if OK) append certificate and getJsonEncoding -----

    @DatabaseField(dataType = DataType.BYTE_ARRAY)
    byte[] contractorCertificate;

    // ----- Sign -----

    @DatabaseField(dataType = DataType.BYTE_ARRAY)
    byte[] contractorSignature;

    // ----- Append signature, getJsonEncoding as Data, sign and sent to Client upon request ----- END CONTRACTOR

    //##############################################################################################

    Contract(){}

    public Contract(Policy policy, Actor contractor){
        this.policy = policy;
        this.contractor = contractor;

        client = policy.owner;
        validFrom = new Date();
        validTo = new Date();
        permissionToSubcontract = Permission.NO;
    }

    public static Contract fromEncoding(Blob encodedContract){

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Policy.class, new PolicyTypeAdapter())
                .registerTypeAdapter(Actor.class, new ActorTypeAdapter())
                .registerTypeAdapter(Contract.class, new ContractTypeAdapter())
                .registerTypeAdapter(byte[].class, new ByteArrayToBase64TypeAdapter())
                .create();

        return gson.fromJson(encodedContract.toString(), Contract.class);
    }

    public String getJsonEncoding(){
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Policy.class, new PolicyTypeAdapter())
                .registerTypeAdapter(Actor.class, new ActorTypeAdapter())
                .registerTypeAdapter(Contract.class, new ContractTypeAdapter())
                .registerTypeAdapter(byte[].class, new ByteArrayToBase64TypeAdapter())
                .setPrettyPrinting().create();

        return gson.toJson(this);
    }

    public void signAsClient(KeyChain keyChain) throws SecurityException, DerDecodingException {

        //Important: We assume the KeyChain has been setup for signing by identity
        IdentityManager idm = keyChain.getIdentityManager();

        IdentityCertificate ownerCertificate = idm.getCertificate(
                idm.getDefaultCertificateNameForIdentity(client.getId()));

        //Note: It seems only byte[] encoding works correctly for a SignedBlob
        clientCertificate = ownerCertificate.wireEncode().getImmutableArray();

        clientSignature = idm.signByCertificate(new Blob(getJsonEncoding()).buf(), ownerCertificate.getName())
                .getSignature().getImmutableArray();
    }

    public void signAsContractor(KeyChain keyChain) throws SecurityException, DerDecodingException {

        //Important: We assume the KeyChain has been setup for signing by identity
        IdentityManager idm = keyChain.getIdentityManager();

        IdentityCertificate ownerCertificate = idm.getCertificate(
                idm.getDefaultCertificateNameForIdentity(contractor.getId()));

        //Note: It seems only byte[] encoding works correctly for a SignedBlob
        contractorCertificate = ownerCertificate.wireEncode().getImmutableArray();

        contractorSignature = idm.signByCertificate(new Blob(getJsonEncoding()).buf(), ownerCertificate.getName())
                .getSignature().getImmutableArray();
    }

    //Getters
    public Status getStatus() {
        return status;
    }

    public Policy getPolicy(){
        return policy;
    }
    public Actor getClient() {
        return client;
    }
    public Actor getContractor() {
        return contractor;
    }

    public Date getValidFrom() {
        return validFrom;
    }
    public Date getValidTo() {
        return validTo;
    }

    public Permission getPermissionToSubcontract() {
        return permissionToSubcontract;
    }

    //TODO - return the IdentityCertificate and Signature NDN classes instead of byte[]
    public byte[] getClientCertificate() {
        return clientCertificate;
    }
    public byte[] getClientSignature() {
        return clientSignature;
    }
    public byte[] getContractorCertificate() {
        return contractorCertificate;
    }
    public byte[] getContractorSignature() {
        return contractorSignature;
    }

    //Setters
    public void setStatus(Status status) {
        this.status = status;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }
    public void setClient(Actor client) {
        this.client = client;
    }
    public void setContractor(Actor contractor) {
        this.contractor = contractor;
    }

    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }
    public void setValidTo(Date validTo) {
        this.validTo = validTo;
    }

    public void setPermissionToSubcontract(Permission permissionToSubcontract) {
        this.permissionToSubcontract = permissionToSubcontract;
    }
}
