package net.named_data.pxp.dialogue.collaboration;

import net.named_data.jndn.Face;
import net.named_data.jndn.security.KeyChain;
import net.named_data.pxp.dialogue.Dialogue;
import net.named_data.pxp.entity.Actor;
import net.named_data.pxp.entity.Contract;
import net.named_data.pxp.entity.Policy;
import net.named_data.pxp.persistance.SQLiteMetaStorage;

/**

 */
public abstract class CollaborationDialogue extends Dialogue {

    protected Contract _contract;

    protected CollaborationDialogue(KeyChain keyChain, Face face, SQLiteMetaStorage metaStorage){

        super(keyChain, face, metaStorage);
    }
}
