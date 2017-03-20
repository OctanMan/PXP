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
import java.util.Date;

/**

 */
public class ClientDialogue extends CollaborationDialogue {

    public ClientDialogue(Contract contract, KeyChain keyChain, Face face, SQLiteMetaStorage metaStorage)
            throws IOException, SecurityException {

        super(keyChain, face, metaStorage);
        _contract = contract;
        _us = _contract.getClient();
        _them = _contract.getContractor();
        _currentPhrase = new CollaborationRequest();
        Speaker.enqueuePhrase(_currentPhrase);
    }

    //Send an invitation to collaborate on a Policy
    private class CollaborationRequest extends Request {

        public void begin() {

            Interest invitation = new Interest(new Name(_them.getId())
                    .append("PXP").append("collaboration").append("invite")
                    .append("POLICY-ID").append(_contract.getPolicy().getId()));

            //FOR-DEMO: Set lifetime to 60 seconds & fresh to avoid getting cached responses from previous demo runs
            invitation.setInterestLifetimeMilliseconds(60000);
            invitation.setMustBeFresh(true);

            /* Sign the invitation with the our certificate
             * Important: We assume the our signing key is already in the Identity Storage and set to default for our Id
             * Important: As per the newer SignedInterest specification;
             * https://redmine.named-data.net/projects/ndn-cxx/wiki/SignedInterest
             * this interest may be susceptible to a replay attack. Please keep in mind that this is POC code.
             */
            try {
                _keyChain.sign(invitation,
                        _keyChain.getIdentityManager()
                                .getDefaultCertificateNameForIdentity(_us.getId()));
            } catch (SecurityException e) {
                e.printStackTrace();
            }

            Demo.out("Sending invitation to "+_them.getId());
            try {
                _pendingInterestId =_face.expressInterest(invitation,this,this,this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //This should be the response to our invitation
        public void onData(Interest interest, Data data) {
            //Tip: If we keep getting cached responses then clear the local NFD cache with terminal commands: "nfd-stop", "nfd-start"

            Demo.out("Received a response to a collaboration invitation");

            //Check the data meets our verification requirements
            try {
                _keyChain.verifyData(data, this, this);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
            if(!_isValidData) {
                Demo.out("Packet signature failed verification");
                return;
            }
            Demo.out("- Packet signature verified");

            String response = data.getContent().toString();

            if(response.matches("Accept")) {
                Demo.out("Collaboration invitation accepted by " + _them.getId());

                Speaker.enqueuePhrase(new ContractDetailsResponse());
            }
            else if (response.matches("Reject")) {
                Demo.out("Collaboration invitation rejected by " + _them.getId() + " :(");
                //TODO - Handle rejection with dignity
            }
            else {
                Demo.out("Could not interpret the invitation response: " + response);
            }
        }

        //Additional check to ensure the data came from the contractor we invited
        public void onVerified(Data data){

            Name signatureName = KeyLocator.getFromSignature(data.getSignature()).getKeyName();
            Name keyName = IdentityCertificate.certificateNameToPublicKeyName(signatureName);
            Name senderName = keyName.getSubName(0, keyName.size()-1);
            if(senderName.match(_them.getId())){
                _isValidData = true;
            }
            else {

                /* This shouldn't happen unless our interest was intercepted and a 'fake' response sent back from a trusted actor.
                 * It does, however, rely on us getting the real Id certificate for the contractor
                 * We'd have to send out another interest and hope it will reach the (legitimate) intended target.
                 */
                Demo.out("A response from a different Actor has been detected!: " + senderName);
                _isValidData = false;
            }
        }
    }

    //If the Contractor has accepted our invite, we need to supply them with contract details
    private class ContractDetailsResponse extends Response {

        public void begin() {

            Name responsePrefix = new Name(_contract.getClient().getId())
                    .append("PXP").append("collaboration").append("contract-details")
                    .append("POLICY-ID").append(_contract.getPolicy().getId());
            try {
                _registeredPrefix = _face.registerPrefix(responsePrefix, this, this);
            } catch (IOException | SecurityException e) {
                e.printStackTrace();
            }
        }

        //This should be our target requesting policy details
        public void onInterest(Name name, Interest interest, Face face, long l, InterestFilter interestFilter) {

            /* Note: The contractor may send their request before we get their "Accept" and register
             * the ".../contract-details" prefix.
             * For the POC it is sufficient just to wait a short period before they call begin(), however,
             * a more formalised, fail-safe mechanism for this part of the protocol dialogue should be
             * established, likely using Timeouts and NACK's for deciding if and when to retry.
             */

            Demo.out("Received a Contract details request");

            //Check the interest meets our verification requirements
            try {
                _keyChain.verifyInterest(interest, this, this);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
            if(!_isValidInterest){
                Demo.out("Signature failed verification");
                Data response = new Data(interest.getName());
                response.setContent(new Blob("Bad Request: Signature failed verification"));
                try {
                    _face.putData(response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
            Demo.out("Signature verified");

            //Sign, encode and send the contract
            try {
                _contract.signAsClient(_keyChain);
            } catch (SecurityException | DerDecodingException e) {
                e.printStackTrace();
                Demo.out("Error signing Contract as client");
            }

            Data responseData = new Data(interest.getName());
            responseData.setContent(new Blob(_contract.getJsonEncoding()));
            try {
                _keyChain.sign(responseData, _keyChain.getIdentityManager()
                        .getDefaultCertificateNameForIdentity(_us.getId()));
                _face.putData(responseData);
            } catch (SecurityException | IOException e) {
                e.printStackTrace();
            }

            _face.removeRegisteredPrefix(_registeredPrefix);

            //Set the begin time of this next phrase to a few seconds later
            Date when = new Date();
            long nowish = when.getTime();
            when.setTime(nowish + 3000);
            Speaker.enqueuePhrase(new CoSignedContractRequest(when));
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

    private class CoSignedContractRequest extends Request {

        private CoSignedContractRequest(Date beginTime){
            _beginTime = beginTime;
        }

        public void begin() {

            /* Note: The contractor may take some time to receive our contract details and register
             * the ".../co-signed-contract" prefix.
             * For the POC it is sufficient just to wait a short period before calling begin(), however,
             * a more formalised, fail-safe mechanism for this part of the protocol dialogue should be
             * established, likely using Interest Timeouts and NACK's for deciding if and when to retry.
             */

            Interest invitation = new Interest(new Name(_them.getId())
                    .append("PXP").append("collaboration").append("co-signed-contract")
                    .append("POLICY-ID").append(_contract.getPolicy().getId()));

            //FOR-DEMO: Set lifetime to 60 seconds & fresh to avoid getting cached responses from previous demo runs
            invitation.setInterestLifetimeMilliseconds(60000);
            invitation.setMustBeFresh(true);

            try {
                _keyChain.sign(invitation,
                        _keyChain.getIdentityManager()
                                .getDefaultCertificateNameForIdentity(_us.getId()));
            } catch (SecurityException e) {
                e.printStackTrace();
            }

            Demo.out("Sending co-signed contract request to " + _them.getId());

            try {
                _pendingInterestId = _face.expressInterest(invitation, this, this, this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //This should be the response to our request for
        public void onData(Interest interest, Data data) {
            //Tip: If we keep getting cached responses then clear the local NFD cache with terminal commands: "nfd-stop", "nfd-start"

            Demo.out("Received a response to our request for a co-signed Contract");

            //Check the data meets our verification requirements
            try {
                _keyChain.verifyData(data, this, this);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
            if(!_isValidData) {
                Demo.out("Packet signature failed verification");
                return;
            }
            Demo.out("Packet signature verified");

            Blob response = data.getContent();

            if(response.toString().matches("Reject")) {
                Demo.out("Contract rejected by " + _them.getId() + " :(");
                //TODO - Handle rejection with more dignity
            }
            else {
                _isComplete = reviewCoSignedContract(response);
            }

        }

        //Additional check to ensure the data came from the contractor we invited
        public void onVerified(Data data){

            Name signatureName = KeyLocator.getFromSignature(data.getSignature()).getKeyName();
            Name keyName = IdentityCertificate.certificateNameToPublicKeyName(signatureName);
            Name senderName = keyName.getSubName(0, keyName.size()-1);
            if(senderName.match(_them.getId())){
                _isValidData = true;
            }
            else {

                /* This shouldn't happen unless our interest was intercepted and a 'fake' response sent back from a trusted actor.
                 * It does, however, rely on us getting the real Id certificate for the contractor
                 * We'd have to send out another interest and hope it will reach the (legitimate) intended target.
                 */
                Demo.out("A response from a different Actor has been detected!: " + senderName);
                _isValidData = false;
            }
        }

        private boolean reviewCoSignedContract(Blob response){

            //TODO - check for decoding errors
            Contract rContract = Contract.fromEncoding(response);

            //TODO - review, validate and persist the co-signed contract

            //TODO - figure out what to do in the event that we receive a garbled mess
            //System.out.println("Could not interpret the co-signed Contract response. Possible network error");

            Demo.out("Co-signed Contract received. Awesome!");

            return true;
        }
    }
}
