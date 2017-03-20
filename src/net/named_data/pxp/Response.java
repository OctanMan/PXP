package net.named_data.pxp;

import net.named_data.jndn.*;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.encoding.WireFormat;
import net.named_data.jndn.security.OnVerifiedInterest;
import net.named_data.jndn.security.OnVerifyInterestFailed;
import net.named_data.jndn.security.certificate.IdentityCertificate;
import net.named_data.pxp.entity.Actor;

/* Handles an incoming Interest
* */
public abstract class Response extends Phrase
        implements OnRegisterFailed, OnInterestCallback, OnVerifiedInterest, OnVerifyInterestFailed {

    protected long _registeredPrefix;
    protected boolean _isValidInterest;

    public void onRegisterFailed(Name prefix) {

    }

    public void onInterest(Name name, Interest interest, Face face, long l, InterestFilter interestFilter) {

    }

    public void onVerifiedInterest(Interest interest) {
        _isValidInterest = true;
    }

    public void onVerifyInterestFailed(Interest interest) {
        _isValidInterest = false;
    }


    protected Actor extractActorFromSignedInterest(Interest interest){

        /* Example Signed Interest:
        ndn/pxp/demo/bob
        /PXP/collaboration/invite
        /%00%00%01V%B2%3DA%26/S%EA%CF%22F%DAmB
        /%16%3F%1B%01%01%1C%3A%078%08%03ndn%08%03pxp%08%04demo%08%05alice%08%03KEY%08%11ksk-1471783265575%08%07ID-CERT
        /%17%FD%01%00m%0C%16%BC%1B%82%CA%0D4%0AI%80%BB%12%9E%89%9B%FEQ%83%28%BD%F6%FB%D5%E5%9D6%94wD%2A2%E9%8B%01%CA%82s%3A%3BviF%E3%19%01t+f_%88%7D%09t%3A%B5%3CA%91%5D%A6%C3%E1%F4%16%09p%81%B8T%89%C6%14%FD%95NE%08%04%23c%97%E5%E6cZ%84X%7C%CA%D5%8DZ%28%CA8%19%3C%B4j%BCmB%12%3AC%C7%10%FA%11%1D%C4%5BEJ%96%E9%22%FF%26%C1%AC%60%F1%AF%1C%8D%F6%01%26%3C%A1%E4%A9%E6%CD%96VSA%EC%FD%01%8FhW%99%B6%E7%DBI%B1%5E2%2A%CF%9A.%A4%B1%D4%0C%0C%B1%DB%A8%40%3Art%DA%9F%B7%98%3C%14Z%F5u%8F%005%8A%0A%13%AE%28%D8%D5J%D2%D3A%08%F8_%AD%14IK%9F%7Ee%5C%0B%E5%E8%88%8AT73%17%1E%0E%8EK%2C%22%7C%D8%AA%16%FE%88-X%A1z%7C%E0%E4k%AA%D4%28%04%AF%01Y%E7F%C5%9A%DE%AF%18%F3%29%CCS%A7D%C6%D0
        */

        Signature signature = extractSignature(interest);
        Name signatureName = KeyLocator.getFromSignature(signature).getKeyName();
        Name keyName = IdentityCertificate.certificateNameToPublicKeyName(signatureName);
        Name senderName = keyName.getSubName(0, keyName.size()-1);
        return  new Actor(senderName);
    }

    /**
     * Note: This method has been taken (almost) directly from ConfigurationPolicyManager
     * - part of the jNDN library. Adapted only to use the default WireFormat
     * Extract the signature information from the interest name.
     * @param interest The interest whose signature is needed.
     * from the interest name.
     * @return A shared_ptr for the Signature object. This is null if can't decode.
     */
    protected Signature extractSignature(Interest interest)
    {
        WireFormat wireFormat = WireFormat.getDefaultWireFormat();
        if (interest.getName().size() < 2)
            return null;

        try {
            return wireFormat.decodeSignatureInfoAndValue
                    (interest.getName().get(-2).getValue().buf(),
                            interest.getName().get(-1).getValue().buf());
        } catch (EncodingException ex) {
            return null;
        }
    }
}
