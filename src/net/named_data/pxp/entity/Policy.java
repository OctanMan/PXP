package net.named_data.pxp.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;
import net.named_data.jndn.Name;
import net.named_data.pxp.persistance.NamePersister;

import java.time.Instant;
import java.util.Collection;

/**

 */
@DatabaseTable(tableName = "policies")
public class Policy {


    @DatabaseField(id = true, uniqueCombo = true)
    Name id;

    @DatabaseField
    String nickName;

    @DatabaseField(persisterClass = NamePersister.class)
    Name dossierUri;

    @DatabaseField(foreign = true, canBeNull = false, uniqueCombo = true)
    Actor owner;

    //This is an encoded version of a Policy with the owner's signature - for proof of ownership
    //@DatabaseField(dataType = DataType.BYTE_ARRAY)
    //private byte[] signedObjectEncoding;

    @ForeignCollectionField
    public Collection<Contract> contracts;

    //For ORM use
    Policy(){}

    /* Use this constructor when creating a Policy from scratch
     */
    public Policy (Actor owner) {
        this.owner = owner;
        String epochMilli = String.valueOf(Instant.now().toEpochMilli());
        this.id = new Name(owner.getId()).append("pid-" + epochMilli);
        this.dossierUri = this.getDefaultDossierUri();
    }

    /* Use this constructor when creating a local copy of a Policy

    public static Policy FromExisting (Actor owner, Name id) {
        Policy p = new Policy();
        p.owner = owner;
        p.id = new Name(id);
        return p;
    }
    */

    public Actor getOwner() { return owner; }

    //One idea could be to share with ChronoSync, although problematic in large networks like the internet
    //See: http://irl.cs.ucla.edu/~zhenkai/papers/chronosync.pdf
    private Name getDefaultDossierUri(){
        return new Name(owner.id).append("PXP").append("dossier")
                .append("POLICY-ID").append(id);
    }

    public Name getId(){
        return id;
    }

    public Name.Component getIdSuffix() {
        return id.get(id.size()-1);
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    /*Used to place a signed JSON representation of a Policy within itself. Only the owner has the key to do this.
    public void generateSignedObjectEncoding(KeyChain keyChain) throws SecurityException, DerDecodingException, EncodingException, DerEncodingException {

        Gson gson = new GsonBuilder().registerTypeAdapter(Policy.class, new PolicyTypeAdapter()).create();
        String policyJSON = gson.toJson(this);
        Data d = new Data();
        d.setContent(new Blob(policyJSON));

        //Important: We assume the KeyChain has been setup for signing by identity
        //keyChain.signByIdentity(d, getOwner().getId());
        IdentityManager idm = keyChain.getIdentityManager();

        IdentityCertificate ownerCertificate = idm.getCertificate(
                idm.getDefaultCertificateNameForIdentity(getOwner().getId()));

        //Note: It seems only byte[] encoding works correctly for a SignedBlob
        byte[] ownerCertDer = ownerCertificate.wireEncode().getImmutableArray();

        IdentityCertificate oc2 = new IdentityCertificate();
        oc2.wireDecode(new Blob(ownerCertDer));

        keyChain.signByIdentity(new Blob(policyJSON).buf(), getOwner().getId());


        Blob signatureBits = d.getSignature().getSignature();
        Signature copySig = new Sha256WithRsaSignature();
        copySig.setSignature(signatureBits);


        Blob encoding = d.wireEncode();
        signedObjectEncoding = encoding.getImmutableArray();

        //DEBUG Test decode
        Blob b2 = new Blob(signedObjectEncoding, true);
        Data d2 = new Data();
        try {
            d2.wireDecode(b2);
        } catch (EncodingException e) {
            e.printStackTrace();
        }

        String policyJSON2 = d2.getContent().toString();
        System.out.println(policyJSON2);
        Policy p2 = gson.fromJson(policyJSON2, Policy.class);
    }*/
}
