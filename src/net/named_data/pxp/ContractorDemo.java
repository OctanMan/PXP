package net.named_data.pxp;

import net.named_data.jndn.Face;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.security.identity.BasicIdentityStorage;
import net.named_data.jndn.security.identity.IdentityManager;
import net.named_data.jndn.security.policy.PolicyManager;
import net.named_data.jndn.security.policy.SelfVerifyPolicyManager;
import net.named_data.pxp.dialogue.collaboration.ContractorDialogue;
import net.named_data.pxp.entity.Actor;
import net.named_data.pxp.persistance.SQLiteMetaStorage;

import java.io.IOException;
import java.sql.SQLException;

/*
* In this demo we meet Bob, an Autonomous System admin looking to implement
* inter-domain Policy Contracts on his network
*/
public class ContractorDemo {

    public static void main(String[] args) {

        //Create the persistent pxp metadata storage
        SQLiteMetaStorage metaStore = new SQLiteMetaStorage("bob");

        //Create an actor named Bob
        Actor bob = new Actor(new Name("/ndn/pxp/demo/bob"));

        //Persist Bob to the storage
        try {
            metaStore.persistActor(bob);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        /* Bob decides to make his Endorsements and Certifications public to make it
         * easy for clients to find out about the services he can provide.
         * TODO - Create resource listeners e.g for prefix .../PXP/resources/certifications
         */

        /* Bob wants to listen for collaboration invites.
         * This requires setting up a few requisite components...
         */

        //BasicIdentityStorage reads and writes to ~/home/.ndn/ndnsec-public-info.db (setup by NFD)
        BasicIdentityStorage identityStorage;
        try {
            identityStorage = new BasicIdentityStorage();
        } catch (SecurityException e) {
            e.printStackTrace();
            return;
        }

        /* Note: PolicyManager is an abstract class in the jNDN lib relating to security
         * and has nothing to do with PXP Policy (so apologies for reusing the term).
         * Its purpose is to verify signed packets by some predefined 'trust threshold'
         * as configured by individual actors based on their personal trust models.
         * TODO - Create a PolicyManager that is able to use PXP's Endorsements and Certificates
         * As these entities are part of the protocol design it would be good to use them in the
         * properly integrate them into the verification process, but alas, proof-of-concept limitations.
         * In the meantime, SelfVerifyPolicyManager will check the local identityStorage for
         * self-signed ID-CERTs. These contain the public keys required for signature verification.
         */
        PolicyManager trustChecker = new SelfVerifyPolicyManager(identityStorage);

        //A KeyChain provides a convenient object to sign and verify packets
        KeyChain keyChain;
        try {
            keyChain = new KeyChain(new IdentityManager(identityStorage), trustChecker);
        } catch (SecurityException e) {
            e.printStackTrace();
            return;
        }

        //A Face is a communication conduit between this process and a NDN Forwarding Daemon
        Face face = new Face();
        keyChain.setFace(face);
        try {
            face.setCommandSigningInfo(keyChain,
                    keyChain.getIdentityManager().getDefaultCertificateNameForIdentity(bob.getId()));
        } catch (SecurityException e) {
            e.printStackTrace();
            return;
        }

        //Bob now has the requisite components to listen for collaboration invites as a contractor
        try {
            ContractorDialogue.Listener.startListening(bob, keyChain, face, metaStore);
        } catch (IOException | SecurityException e) {
            e.printStackTrace();
        }

        /* In a full implementation, listening would be handled by more than one thread, however,
         * for this proof-of-concept demo, we shall run it from here
         */
        while (true) {
            try {
                Speaker.speak();
                face.processEvents();
                Thread.sleep(100);
            } catch (IOException | EncodingException | InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
    }
}
