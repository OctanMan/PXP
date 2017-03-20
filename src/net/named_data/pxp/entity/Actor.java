package net.named_data.pxp.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;
import net.named_data.jndn.Name;
import net.named_data.pxp.persistance.NamePersister;

import java.util.Collection;

/**

 */
@DatabaseTable(tableName = "actors")
public final class Actor {

    @DatabaseField(id = true, persisterClass = NamePersister.class)
    Name id;

    @DatabaseField
    String nickName;

    @DatabaseField
    String email;

    @DatabaseField
    String organisation;

    //FYI: This could be a bank account, Bitcoin Address, etc.
    @DatabaseField
    String paymentInfo;

    @ForeignCollectionField
    Collection<Policy> ownedPolicies;

    @ForeignCollectionField
    Collection<Contract> contractedPolicies;

    Actor(){}

    public Actor(Name identityName){
        this.id = identityName;
    }

    public Name getId(){
        return id;
    }
    public String getNickName() {
        return nickName;
    }
    public String getEmail() {
        return email;
    }
    public String getOrganisation() {
        return organisation;
    }
    public String getPaymentInfo() {
        return paymentInfo;
    }

    public Collection<Policy> getOwnedPolicies() {
        return ownedPolicies;
    }
    public Collection<Contract> getContractedPolicies() {
        return contractedPolicies;
    }
}
