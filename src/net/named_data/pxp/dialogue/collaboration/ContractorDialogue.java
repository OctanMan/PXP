package net.named_data.pxp.dialogue.collaboration;

import net.named_data.jndn.*;
import net.named_data.jndn.encoding.der.DerDecodingException;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.security.certificate.IdentityCertificate;
import net.named_data.jndn.util.Blob;
import net.named_data.pxp.Demo;
import net.named_data.pxp.Request;
import net.named_data.pxp.Response;
import net.named_data.pxp.Speaker;
import net.named_data.pxp.entity.Actor;
import net.named_data.pxp.entity.Contract;
import net.named_data.pxp.persistance.SQLiteMetaStorage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.Scanner;

/*
 *
 */
public class ContractorDialogue extends CollaborationDialogue {

    private Interest _invite;

    private ContractorDialogue(Actor contractor, Interest invite, KeyChain keyChain, Face face, SQLiteMetaStorage metaStorage)
            throws SecurityException, IOException, InterruptedException {

        super(keyChain, face, metaStorage);
        _us = contractor;
        _invite = invite;
        Speaker.enqueuePhrase(new CollaborationResponse());
    }

    private class CollaborationResponse extends Response {

        public void begin() {

            //Check the interest meets our verification requirements
            try {
                _keyChain.verifyInterest(_invite, this, this);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
            if (!_isValidInterest)
                return;

            _them = extractActorFromSignedInterest(_invite);

            /* Note: The interface/interaction here is simplified for this proof-of-concept.
             * A production version would likely have a GUI with numerous ways to view relevant information.
             */

            //Ask the user for their executive decision on whether to collaborate
            Demo.out(_them.getId() + " has invited you to collaborate on a Policy\n" +
                    "Accept invitation? [Y/n]: ");

            Scanner scanner = new Scanner(System.in);
            int answer = scanner.next().charAt(0);
            if (answer == 'y' || answer == 'Y') {
                try {
                    sendStringResponse(_invite, "Accept");
                } catch (SecurityException | IOException e) {
                    e.printStackTrace();
                }
                //Set the begin time of this next phrase to a few seconds later
                Date when = new Date();
                long nowish = when.getTime();
                when.setTime(nowish + 3000);
                Speaker.enqueuePhrase(new ContractDetailsRequest(when));

            } else {
                try {
                    sendStringResponse(_invite, "Reject");
                } catch (SecurityException | IOException e) {
                    e.printStackTrace();
                }
                _isComplete = true;
            }
        }
    }

    private class ContractDetailsRequest extends Request {

        private ContractDetailsRequest(Date beginTime){
            _beginTime = beginTime;
        }

        public void begin() {

            /* Note: The client may take some time to receive our "Accept" and register
             * the ".../contract-details" prefix.
             * For the POC it is sufficient just to wait a short period before calling begin(), however,
             * a more formalised, fail-safe mechanism for this part of the protocol dialogue should be
             * established, likely using Interest Timeouts and NACK's for deciding if and when to retry.
             */

            Name policyId = extractPolicyIdFromInterest(_invite);
            Interest request = new Interest(new Name(_them.getId())
                    .append("PXP").append("collaboration").append("contract-details")
                    .append("POLICY-ID").append(policyId));

            //FOR-DEMO: Set lifetime to 10 seconds so the NFD doesn't get encumbered
            request.setInterestLifetimeMilliseconds(10000);

            //Sign to prove it's us
            try {
                _keyChain.sign(request, _keyChain.getIdentityManager()
                        .getDefaultCertificateNameForIdentity(_us.getId()));
            } catch (SecurityException e) {
                e.printStackTrace();
            }

            Demo.out("Sending contract details request to " + _them.getId());

            try {
                _pendingInterestId = _face.expressInterest(request, this, this, this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //This should be the encoded contract
        public void onData(Interest interest, Data data) {

            Demo.out("Received a response to a Contract details request");

            //Check the data meets our verification requirements
            try {
                _keyChain.verifyData(data, this, this);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
            if (!_isValidData) {
                Demo.out("Packet signature failed verification");
                return;
            }
            Demo.out("Packet signature verified");


            if(reviewContractDetails(data.getContent())){
                //Enqueue our next response
                Speaker.enqueuePhrase(new ContractCoSignResponse());
            }
            //TODO - handle a negative review outcome

        }

        //Additional check to ensure it came from the client we responded to
        public void onVerified(Data data){

            Name signatureName = KeyLocator.getFromSignature(data.getSignature()).getKeyName();
            Name keyName = IdentityCertificate.certificateNameToPublicKeyName(signatureName);
            Name senderName = keyName.getSubName(0, keyName.size()-1);
            if(senderName.match(_them.getId())) {
                _isValidData = true;
            }
            else {

                /* This shouldn't happen unless our interest was intercepted and a 'fake' response sent back.
                 * It does, however, rely on us getting the real Id certificate for the client
                 * We'd have to send out another interest and hope it will reach the (legitimate) intended target.
                 */
                Demo.out("A response from a different Actor has been detected!: " + senderName + " ");
                _isValidData = false;
            }
        }

        /* Note: This method is extremely limited in the proof-of-concept - we assume the contract is legit as that's
         * the only possible outcome of the current demo, barring an encoding/transmission error.
         * A real implementation would perform verification and do more than just print the JSON!
         */
        private boolean reviewContractDetails(Blob response){

            //TODO - wrap this in try/catch for decoding errors
            _contract = Contract.fromEncoding(response);

            Demo.out("Contract Details:\n" + _contract.getJsonEncoding());

            return true;
        }
    }

    private class ContractCoSignResponse extends Response {

        public void begin() {

            Name responsePrefix = new Name(_us.getId())
                    .append("PXP").append("collaboration").append("co-signed-contract")
                    .append("POLICY-ID").append(_contract.getPolicy().getId());
            try {
                _registeredPrefix = _face.registerPrefix(responsePrefix, this, this);
            } catch (IOException | SecurityException e) {
                e.printStackTrace();
            }
        }

        public void onInterest(Name name, Interest interest, Face face, long l, InterestFilter interestFilter) {

            Data responseData = new Data(interest.getName());
            Demo.out("Received a request for a co-signed Contract");

            //Check the interest meets our verification requirements
            try {
                _keyChain.verifyInterest(interest, this, this);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
            if(!_isValidInterest){
                Demo.out("Packet signature failed verification");
                responseData.setContent(new Blob("Bad Request: Packet signature failed verification"));
                try {
                    //Sign to prove the responseData came from us
                    _keyChain.sign(responseData, _keyChain.getIdentityManager()
                            .getDefaultCertificateNameForIdentity(_us.getId()));
                    _face.putData(responseData);
                } catch (IOException | SecurityException e) {
                    e.printStackTrace();
                }
                return;
            }
            Demo.out("Packet signature verified");

            /* Note: The interface/interaction here is simplified for this proof-of-concept.
             * A production version would likely have a GUI with numerous ways to view relevant information.
             * The act of co-signing could also be done at any time after receiving the contract details and
             * prior to receiving the "co-signed-contract" request, it's just here in the proof-of-concept
             * for the sake of simplicity and readability
             */

            //Ask the user for their executive decision on whether to sign the contract
            Demo.out("Co-Sign Contract? [Y/n]: ");

            Scanner scanner = new Scanner(System.in);
            int answer = scanner.next().charAt(0);
            if (answer == 'y' || answer == 'Y') {
                try {
                    _contract.signAsContractor(_keyChain);
                } catch (SecurityException | DerDecodingException e) {
                    e.printStackTrace();
                }
                responseData.setContent(new Blob(_contract.getJsonEncoding()));
                Demo.out("Sending signed Contract");
                try {
                    //Sign to prove the responseData came from us
                    _keyChain.sign(responseData, _keyChain.getIdentityManager()
                            .getDefaultCertificateNameForIdentity(_us.getId()));
                    _face.putData(responseData);
                } catch (IOException | SecurityException e) {
                    e.printStackTrace();
                }
                try {
                    //Add the relevant entities to the metaStorage (if they're not there already)
                    _metaStorage.persistActor(_contract.getClient());
                    _metaStorage.persistPolicy(_contract.getPolicy());
                    _metaStorage.persistContract(_contract);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            else {
                try {
                    sendStringResponse(interest, "Reject");
                } catch (SecurityException | IOException e) {
                    e.printStackTrace();
                }
            }

            _face.removeRegisteredPrefix(_registeredPrefix);
            _isComplete = true;
        }

        //An additional check to make sure the interest came from the contractor we invited in this dialogue
        public void onVerifiedInterest(Interest interest) {

            Actor sender = extractActorFromSignedInterest(interest);
            if(sender.getId().match(_them.getId())) {
                _isValidInterest = true;
            }
            else {
                //This is a verified actor but not one we invited to this policy!
                Demo.out("Interest from a verified Actor but not the one invited to collaborate on this Policy!");
                //Note: You would likely log this as either an error or a sneaky actor trying to subvert the protocol

                _isValidInterest = false;
            }
        }
    }

    private void sendStringResponse(Interest request, String response) throws SecurityException, IOException {

        // Make and sign the response Data
        //TODO - Encrypt the response for secrecy purposes (optional?)
        Data responseData = new Data(request.getName());
        responseData.setContent(new Blob(response));

        //FOR-DEMO: Set freshness to 10 seconds so the NFD doesn't get encumbered
        responseData.getMetaInfo().setFreshnessPeriod(10000);

        //Sign to prove the responseData came from us
        _keyChain.sign(responseData, _keyChain.getIdentityManager()
                .getDefaultCertificateNameForIdentity(_us.getId()));

        //Send
        System.out.println("Sending response to " + _them.getId());
        _face.putData(responseData);
    }

    /* Note: This method is a bit of a hack, but again, suitable for the proof-of-concept demo.
     * Why? Because string-level parsing of the interest name introduces plenty of opportunity for errors.
     * E.g. an Identity Name may feature a Component like "rapid-response-team" which would be picked up in the
     * search for the string "pid-" in the code below.
     * Rather than forcefully avoiding such Identity Names, the solution is to define a TLV wire encoding for
     * PXP Name Components like this one, although this requires some standardisation and the necessary wire
     * encoders/decoders to be created.
     * See SignatureInfo & SignatureValue as an example: http://named-data.net/doc/ndn-tlv/signature.html
     */
    private Name extractPolicyIdFromInterest(Interest interest) {
        /* Example Signed Interest Name:
        "ndn/pxp/demo/bob
        /PXP/collaboration/invite
        /POLICY-ID/ndn/pxp/demo/alice/pid-1471869173758
        /%00%00%01V%B2%3DA%26/S%EA%CF%22F%DAmB
        /%16%3F%1B%01%01%1C%3A%078%08%03ndn%08%03pxp%08%04demo%08%05alice%08%03KEY%08%11ksk-1471783265575%08%07ID-CERT
        /%17%FD%01%00m%0C%16%BC%1B%82%CA%0D4%0AI%80%BB%12%9E%89%9B%FEQ%83%28%BD%F6%FB%D5%E5%9D6%94wD%2A2%E9%8B%01%CA%82s
        %3A%3BviF%E3%19%01t+f_%88%7D%09t%3A%B5%3CA%91%5D%A6%C3%E1%F4%16%09p%81%B8T%89%C6%14%FD%95NE%08%04%23c%97%E5%E6cZ
        %84X%7C%CA%D5%8DZ%28%CA8%19%3C%B4j%BCmB%12%3AC%C7%10%FA%11%1D%C4%5BEJ%96%E9%22%FF%26%C1%AC%60%F1%AF%1C%8D%F6%01
        %26%3C%A1%E4%A9%E6%CD%96VSA%EC%FD%01%8FhW%99%B6%E7%DBI%B1%5E2%2A%CF%9A.%A4%B1%D4%0C%0C%B1%DB%A8%40%3Art%DA%9F%B7
        %98%3C%14Z%F5u%8F%005%8A%0A%13%AE%28%D8%D5J%D2%D3A%08%F8_%AD%14IK%9F%7Ee%5C%0B%E5%E8%88%8AT73%17%1E%0E%8EK%2C%22
        %7C%D8%AA%16%FE%88-X%A1z%7C%E0%E4k%AA%D4%28%04%AF%01Y%E7F%C5%9A%DE%AF%18%F3%29%CCS%A7D%C6%D0"
        */

        Name name = new Name(interest.getName());
        int policyStartComponent, policyEndComponent, i = 0;
        Name.Component c = name.get(i);
        while(!(c.toEscapedString().matches("POLICY-ID"))) {
            i++;
            c = name.get(i);
        }
        policyStartComponent = i;
        i++;
        while(!(c.toEscapedString().startsWith("pid-"))){
            i++;
            c = name.get(i);
        }
        policyEndComponent = i;

        Name senderId = name.getSubName(policyStartComponent+1,
                (policyEndComponent - policyStartComponent)-1);

        //Actor sender = new Actor(senderId);
        //return Policy.AsContractor(sender, c);
        return senderId.append(c);
    }

    public static class Listener implements OnInterestCallback, OnRegisterFailed, OnRegisterSuccess {

        private static Listener _instance;
        private static Actor _client;
        private static KeyChain _keyChain;
        private static Face _face;
        private static SQLiteMetaStorage _metaStorage;
        private static long _registeredPrefix;
        private static boolean _registerSuccessful;

        private static Listener getInstance(){
            if(_instance != null)
                return _instance;
            else {
                _instance = new Listener();
                return _instance;
            }
        }

        private Listener(){}

        public static boolean startListening(Actor client, KeyChain keyChain, Face face, SQLiteMetaStorage metaStorage)
                throws IOException, SecurityException {
            _client = client;
            _keyChain = keyChain;
            _face  = face;
            _metaStorage = metaStorage;

            //Start listening for collaboration invites
            Name prefix = new Name(_client.getId() + "/PXP/collaboration/invite");
            _registeredPrefix = _face.registerPrefix(prefix,
                    getInstance(), getInstance(), (OnRegisterSuccess) getInstance());
            return _registerSuccessful;
        }

        public static void stopListening(){
            _face.removeRegisteredPrefix(_registeredPrefix);
        }

        /* This onInterest will be called after we have started listening for collaborators who have
         * initiated a dialogue with us via an interest <client-name>/PXP/collaboration/invite/<policy-id>
         */
        public void onInterest(Name name, Interest interest, Face face, long l, InterestFilter interestFilter) {

            try {
                new ContractorDialogue(_client, interest, _keyChain, _face, _metaStorage);
            } catch (SecurityException | IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void onRegisterFailed(Name name) {
            _registerSuccessful = false;
        }

        public void onRegisterSuccess(Name prefix, long registeredPrefixId) {
            Demo.out("Started listening for collaboration invites");
            _registerSuccessful = true;
        }
    }
}