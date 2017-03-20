package net.named_data.pxp.persistance;

import java.sql.*;
import java.util.List;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.field.DataPersisterManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import net.named_data.jndn.Name;
import net.named_data.pxp.entity.Actor;
import net.named_data.pxp.entity.Contract;
import net.named_data.pxp.entity.Policy;

/**

 */
public class SQLiteMetaStorage {

    private ConnectionSource _connectionSource;
    Dao<Actor, Name> _actorDao;
    Dao<Policy, Integer> _policyDao;
    Dao<Contract, Integer> _contractDao;

    //TODO - Create an Interface for MetaStorage to enable other implementations
    public SQLiteMetaStorage(String databaseFileName){

        String databaseUrl = "jdbc:sqlite:" + databaseFileName + ".db";
        // create a connection source to our database
        try {
            _connectionSource = new JdbcConnectionSource(databaseUrl);

            //Attempt to set up the database
            DataPersisterManager.registerDataPersisters(NamePersister.getSingleton());

            TableUtils.createTableIfNotExists(_connectionSource, Actor.class);
            TableUtils.createTableIfNotExists(_connectionSource, Policy.class);
            TableUtils.createTableIfNotExists(_connectionSource, Contract.class);

            _actorDao = DaoManager.createDao(_connectionSource, Actor.class);
            _policyDao = DaoManager.createDao(_connectionSource, Policy.class);
            _contractDao = DaoManager.createDao(_connectionSource, Contract.class);

        } catch (SQLException e) {
            System.out.println("Initialisation error for database " + databaseUrl + ":\n" + e.getMessage());
        }
    }

    /*
    * ---------- Persist methods ----------
    */

    public void persistActor(Actor actor) throws SQLException {
        _actorDao.createOrUpdate(actor);
    }

    public void persistPolicy(Policy policy) throws SQLException {
        _policyDao.createOrUpdate(policy);
    }

    public void persistContract(Contract contract) throws SQLException {
        _contractDao.createOrUpdate(contract);
    }

    /*
    * ---------- Retrieve methods ----------
    */

    public Actor retrieveActor(Name identityName) throws SQLException {
        return _actorDao.queryForId(identityName);
    }

    public List<Actor> retrieveActors(Name identityNamePrefix) throws SQLException {
        //Performs a SQL LIKE query to find all Actors whose identity name matches the prefix
        QueryBuilder<Actor, Name> qb = _actorDao.queryBuilder();
        return qb.where().like("identityName", identityNamePrefix + "%").query();
    }

    public Policy retrievePolicy(int policyId) throws SQLException {
        return _policyDao.queryForId(policyId);
    }

    public List<Policy> retrievePolicies(String policyNickNamePattern) throws SQLException {
        //Performs a SQL LIKE query to find all Policies with a nickname matching the pattern
        QueryBuilder<Policy, Integer> qb = _policyDao.queryBuilder();
        return qb.where().like("nickName", "%"+policyNickNamePattern+"%").query();
    }

    public Contract retrieveContract(Policy policy, Actor contractor) throws SQLException {
        QueryBuilder<Contract, Integer> qb = _contractDao.queryBuilder();
        return qb.where()
                .eq("policy_id", policy.getId()).and()
                .eq("contractor_id", contractor.getId())
                .queryForFirst();
    }

}
