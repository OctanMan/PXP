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
import net.named_data.pxp.dialogue.collaboration.ClientDialogue;
import net.named_data.pxp.entity.Actor;
import net.named_data.pxp.entity.Contract;
import net.named_data.pxp.entity.Policy;
import net.named_data.pxp.persistance.SQLiteMetaStorage;

import java.io.IOException;
import java.sql.SQLException;

/*
* In this demo we meet Alice, a client looking to implement her Policy.
*/
public class OwnerDemo {

    public static void main(String[] args) {

        //Create the persistent pxp metadata store
        SQLiteMetaStorage metaStore = new SQLiteMetaStorage("alice");

        //Create an actor named Alice
        Actor alice = new Actor(new Name("/ndn/pxp/demo/alice"));

        //Persist Alice to the store
        try {
            metaStore.persistActor(alice);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        Demo.out("Acting as Alice");

        //Create a demo Policy with Alice as its owner
        Policy alicesPolicy = new Policy(alice);
        alicesPolicy.setNickName("Alice's Demo Policy");

        //And commit the policy to the store
        try {
            metaStore.persistPolicy(alicesPolicy);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        Demo.out("Using new Policy, " + alicesPolicy.getNickName());

        /* Alice identifies Bob as a possible candidate for implementing her Policy.
         * Before making contact, however, she wants to ascertain how trusted he is.
         * She decides to define a local representation of Bob's identity for recording
         * the the data of this research.
         */

        //Create an actor named Bob
        Actor bob = new Actor(new Name("/ndn/pxp/demo/bob"));

        //Persist Bob to the store
        try {
            metaStore.persistActor(bob);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        //TODO 1 - Create the PXP resource-gathering mechanism to get Bob's Endorsements and Certificates ( + TODO 2)

        /* Alice receives the information she needs to trust Bob.
         * This is because Bob believes transparency is key to his business strategy and has chosen
         * to disclose the services he offers without stringently verifying Alice.
         * She decides to send him a collaboration invite to implement her policy as a contractor,
         * drawing up a Contract as a requisite, amongst other components.
         */
        Demo.out("Identifying Bob ... trust threshold reached");

        Contract contractForBob = new Contract(alicesPolicy, bob);

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
         * TODO 2 - Create a PolicyManager that is able to use PXP's Endorsements and Certificates
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
                    keyChain.getIdentityManager().getDefaultCertificateNameForIdentity(alice.getId()));
        } catch (SecurityException e) {
            e.printStackTrace();
            return;
        }

        //Alice now has the requisite components to send a collaboration invite to Bob (herself as a Policy owner/client)
        ClientDialogue dialogue;
        try {
            dialogue = new ClientDialogue(contractForBob, keyChain, face, metaStore);
        } catch (IOException | SecurityException e) {
            e.printStackTrace();
            return;
        }

        /* In a full implementation, communication would be handled by more than one thread, however,
         * for this proof-of-concept demo, we shall run it from here
         */
        while (!(dialogue.getCompletionStatus())) {
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
