package net.named_data.pxp.persistance;

import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.types.StringType;
import com.j256.ormlite.field.SqlType;
import net.named_data.jndn.Name;

/**
 * This class allows ORMLite to persist an NDN Name type in the database as a String/VARCHAR
 */
public class NamePersister extends StringType{

    private static final NamePersister instance = new NamePersister();
    @SuppressWarnings("deprecation")

    private NamePersister() {
        super(SqlType.STRING, new Class<?>[] { Name.class });
    }

    public static NamePersister getSingleton() {
        return instance;
    }

    @Override
    public Object javaToSqlArg(FieldType fieldType, Object javaObject) {
        Name name = (Name) javaObject;
        if (name == null) {
            return null;
        } else {
            return name.toUri();
        }
    }

    @Override
    public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) {
       return new Name((String) sqlArg);
    }
}
