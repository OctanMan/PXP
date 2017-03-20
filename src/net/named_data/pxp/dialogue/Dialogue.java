package net.named_data.pxp.dialogue;

import net.named_data.jndn.*;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.encoding.WireFormat;
import net.named_data.jndn.security.*;
import net.named_data.jndn.security.certificate.IdentityCertificate;
import net.named_data.pxp.Phrase;
import net.named_data.pxp.entity.Actor;
import net.named_data.pxp.persistance.SQLiteMetaStorage;

import java.io.IOException;

/**

 */
public abstract class Dialogue {

    protected Phrase _currentPhrase;
    protected boolean _isComplete;
    protected String _errorMessage;

    protected KeyChain _keyChain;
    protected Face _face;
    protected SQLiteMetaStorage _metaStorage;

    protected Actor _us, _them;

    protected Dialogue(KeyChain keyChain, Face face, SQLiteMetaStorage metaStorage){

        _keyChain = keyChain;
        _face = face;
        _metaStorage = metaStorage;
    }

    public void processEvents(){
        try {
            _face.processEvents();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (EncodingException e) {
            e.printStackTrace();
        }
    }

    public Phrase getCurrentPhrase() {
        return _currentPhrase;
    }

    public boolean getCompletionStatus(){
        return _isComplete;
    }

}
